package com.tuankiet.myapplication.ui.preview;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.tabs.TabLayout;
import com.tuankiet.myapplication.R;
import com.tuankiet.myapplication.agent.AIAgentOrchestrator;
import com.tuankiet.myapplication.agent.PostReviewPolicy;
import com.tuankiet.myapplication.databinding.DialogPreviewBinding;
import com.tuankiet.myapplication.model.Post;
import com.tuankiet.myapplication.model.SocialPlatform;
import com.tuankiet.myapplication.util.PostIllustrationGenerator;

public class PreviewDialogFragment extends DialogFragment {

    private static final String ARG_CONTENT = "ARG_CONTENT";
    private static final String ARG_POST = "ARG_POST";
    private static final String ARG_INITIAL_PLATFORM = "ARG_INITIAL_PLATFORM";

    public interface OnPreviewActionListener {
        default void onPostUpdated(Post updatedPost) {}
        default void onApprovedForPublish(Post approvedPost, SocialPlatform activePlatform) {}
    }

    private DialogPreviewBinding binding;
    private AIAgentOrchestrator orchestrator;
    private OnPreviewActionListener actionListener;

    private Post workingPost;
    private SocialPlatform activePlatform = SocialPlatform.FACEBOOK;
    private boolean isSyncingText = false;
    private boolean isAiRewriteBusy = false;
    private boolean isImageBusy = false;
    private String previousTextBeforeRewrite = null;

    public static PreviewDialogFragment newInstance(String content) {
        return newInstance(content, SocialPlatform.THREADS);
    }

    public static PreviewDialogFragment newInstance(String content, SocialPlatform platform) {
        PreviewDialogFragment fragment = new PreviewDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CONTENT, content != null ? content : "");
        args.putString(ARG_INITIAL_PLATFORM, (platform != null ? platform : SocialPlatform.THREADS).name());
        fragment.setArguments(args);
        return fragment;
    }

    public static PreviewDialogFragment newInstance(Post post, SocialPlatform initialPlatform) {
        PreviewDialogFragment fragment = new PreviewDialogFragment();
        Bundle args = new Bundle();
        if (post != null) {
            args.putSerializable(ARG_POST, post);
        }
        SocialPlatform chosen = initialPlatform != null ? initialPlatform : SocialPlatform.FACEBOOK;
        args.putString(ARG_INITIAL_PLATFORM, chosen.name());
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnPreviewActionListener(OnPreviewActionListener listener) {
        this.actionListener = listener;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogPreviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        orchestrator = new AIAgentOrchestrator(requireContext());

        initWorkingState();
        setupPlatformTabs();
        setupLiveCharCounterAndSafety();
        setupAIRewriteButtons();
        setupImageRegeneration();
        setupActionButtons();

        // Sync UI immediately so counter, safety check, and image reflect initial data right away
        int initialTabIdx = (activePlatform == SocialPlatform.THREADS) ? 1 : 0;
        TabLayout.Tab initialTab = binding.tabPreviewPlatform.getTabAt(initialTabIdx);
        if (initialTab != null && !initialTab.isSelected()) {
            initialTab.select();
        }
        bindActivePlatformUi();
        bindImagePreview();
    }

    private void initWorkingState() {
        Bundle args = getArguments();
        String platformName = args != null ? args.getString(ARG_INITIAL_PLATFORM, SocialPlatform.FACEBOOK.name()) : SocialPlatform.FACEBOOK.name();
        try {
            activePlatform = SocialPlatform.valueOf(platformName);
        } catch (Exception ignored) {
            activePlatform = SocialPlatform.FACEBOOK;
        }
        if (activePlatform == SocialPlatform.LINKEDIN) {
            activePlatform = SocialPlatform.THREADS;
        }

        Post argPost = args != null ? (Post) args.getSerializable(ARG_POST) : null;
        if (argPost != null) {
            workingPost = clonePost(argPost);
        } else {
            workingPost = new Post();
            String initialContent = args != null ? args.getString(ARG_CONTENT, "") : "";
            workingPost.setTopic("Xem trước bài viết");
            workingPost.setTone("Chuyên gia");
            workingPost.setLanguage("Tiếng Việt");
            workingPost.setStatus(Post.STATUS_DRAFT);
            workingPost.setUserApproved(false);
            if (activePlatform == SocialPlatform.THREADS) {
                workingPost.setTargetThreads(true);
                workingPost.setContentThreads(initialContent);
            } else {
                workingPost.setTargetFacebook(true);
                workingPost.setContentFacebook(initialContent);
            }
        }
    }

    private Post clonePost(Post src) {
        Post copy = new Post();
        copy.setId(src.getId());
        copy.setTopic(src.getTopic());
        copy.setTone(src.getTone());
        copy.setLanguage(src.getLanguage());
        copy.setContentFacebook(src.getContentFacebook());
        copy.setContentThreads(src.getContentThreads());
        copy.setContentLinkedIn(src.getContentLinkedIn());
        copy.setStatus(src.getStatus());
        copy.setTargetFacebook(src.isTargetFacebook());
        copy.setTargetThreads(src.isTargetThreads());
        copy.setTargetLinkedIn(src.isTargetLinkedIn());
        copy.setCreatedAt(src.getCreatedAt());
        copy.setImageUrl(src.getImageUrl());
        copy.setImagePrompt(src.getImagePrompt());
        copy.setImageVariantIndex(src.getImageVariantIndex());
        copy.setReachCount(src.getReachCount());
        copy.setLikeCount(src.getLikeCount());
        copy.setCommentCount(src.getCommentCount());
        copy.setShareCount(src.getShareCount());
        copy.setPublishedFacebook(src.isPublishedFacebook());
        copy.setPublishedThreads(src.isPublishedThreads());
        copy.setPublishedLinkedIn(src.isPublishedLinkedIn());
        copy.setUserApproved(false); // Always require fresh explicit human approval in preview
        return copy;
    }

    private void setupPlatformTabs() {
        binding.tabPreviewPlatform.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                saveActiveEditorTextToPost();
                activePlatform = (tab.getPosition() == 1) ? SocialPlatform.THREADS : SocialPlatform.FACEBOOK;
                previousTextBeforeRewrite = null;
                binding.btnUndoRewrite.setVisibility(View.GONE);
                bindActivePlatformUi();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void bindActivePlatformUi() {
        if (binding == null || workingPost == null) return;

        String content;
        if (activePlatform == SocialPlatform.THREADS) {
            binding.ivPreviewPlatformIcon.setImageResource(R.drawable.ic_threads);
            binding.tvPreviewAuthorName.setText("@aitech_vietnam (Threads)");
            binding.tvPreviewSubInfo.setText("Vừa xong • 🧵 Cuộc trò chuyện mở (Tối đa 500 ký tự)");
            content = workingPost.getContentThreads();
            if (content.isEmpty() && !workingPost.getContentFacebook().isEmpty() && !workingPost.isTargetThreads()) {
                content = workingPost.getContentFacebook();
            }
        } else {
            binding.ivPreviewPlatformIcon.setImageResource(R.drawable.ic_facebook);
            binding.tvPreviewAuthorName.setText("AI Tech Vietnam (Facebook Page)");
            binding.tvPreviewSubInfo.setText("Vừa xong • 🌐 Công khai (Tối đa 63,206 ký tự)");
            content = workingPost.getContentFacebook();
        }

        isSyncingText = true;
        binding.etPostContent.setText(content);
        if (binding.etPostContent.getText() != null) {
            binding.etPostContent.setSelection(binding.etPostContent.getText().length());
        }
        isSyncingText = false;

        updateCharCounterAndSafety(content);
    }

    private void bindImagePreview() {
        if (binding == null || workingPost == null) return;
        String prompt = workingPost.getImagePrompt();
        binding.tvPreviewImagePrompt.setText(
                prompt != null && !prompt.trim().isEmpty()
                        ? "Prompt ảnh: " + prompt.trim()
                        : "Chưa có prompt ảnh — bấm 'Sinh lại ảnh' để Visual Agent tạo ảnh mới."
        );

        String imageUrl = workingPost.getImageUrl();
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            binding.tvPreviewImageSource.setText("Visual Agent • Concept #" + (workingPost.getImageVariantIndex() + 1));
            PostIllustrationGenerator.loadIntoImageView(requireContext(), imageUrl, binding.ivPreviewImage, null);
        } else {
            binding.tvPreviewImageSource.setText("Chưa có ảnh minh họa • Bấm 'Sinh lại ảnh' để tạo");
            binding.ivPreviewImage.setImageResource(R.drawable.bg_image_placeholder);
        }
    }

    // Bộ đếm ký tự chuẩn xác theo giới hạn Threads (500) & Facebook (63,206) + Cảnh báo an toàn
    private void setupLiveCharCounterAndSafety() {
        binding.etPostContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isSyncingText || binding == null) return;
                String currentText = s != null ? s.toString() : "";
                saveTextToPlatform(currentText);
                workingPost.setUserApproved(false);
                updateCharCounterAndSafety(currentText);
            }
        });
    }

    private void updateCharCounterAndSafety(String content) {
        if (binding == null || !isAdded()) return;
        PostReviewPolicy.CharCountResult countResult = PostReviewPolicy.evaluateCharCount(content, activePlatform);
        binding.tvCharCounter.setText(countResult.formattedCounter);

        if (countResult.isOverLimit) {
            binding.tvCharCounter.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_red));
        } else if (countResult.isNearLimit) {
            binding.tvCharCounter.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_amber));
        } else {
            binding.tvCharCounter.setTextColor(Color.parseColor("#64748B"));
        }

        PostReviewPolicy.SafetyCheckResult safety = PostReviewPolicy.checkPostSafety(content);
        if (safety.hasWarning && safety.warningMessage != null) {
            binding.cardPreviewSafetyWarning.setVisibility(View.VISIBLE);
            binding.tvPreviewSafetyWarning.setText(safety.warningMessage);
        } else {
            binding.cardPreviewSafetyWarning.setVisibility(View.GONE);
        }
    }

    // Kết nối API LLM thật cho 3 nút viết lại: Ngắn hơn, Trang trọng hơn, Đổi Hook mở đầu
    private void setupAIRewriteButtons() {
        binding.btnShorten.setOnClickListener(v -> triggerRealAiRewrite(PostReviewPolicy.REWRITE_SHORTER));
        binding.btnFormal.setOnClickListener(v -> triggerRealAiRewrite(PostReviewPolicy.REWRITE_FORMAL));
        binding.btnChangeHook.setOnClickListener(v -> triggerRealAiRewrite(PostReviewPolicy.REWRITE_HOOK));

        binding.btnUndoRewrite.setOnClickListener(v -> {
            if (previousTextBeforeRewrite != null && binding != null) {
                binding.etPostContent.setText(previousTextBeforeRewrite);
                if (binding.etPostContent.getText() != null) {
                    binding.etPostContent.setSelection(binding.etPostContent.getText().length());
                }
                previousTextBeforeRewrite = null;
                binding.btnUndoRewrite.setVisibility(View.GONE);
                Toast.makeText(getContext(), "↩ Đã khôi phục nội dung trước khi AI viết lại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void triggerRealAiRewrite(String instructionType) {
        if (binding == null || isAiRewriteBusy) return;

        String fullText = binding.etPostContent.getText() != null
                ? binding.etPostContent.getText().toString()
                : "";
        if (fullText.trim().isEmpty()) {
            Toast.makeText(getContext(), "Nội dung bài viết đang trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        int rawStart = binding.etPostContent.getSelectionStart();
        int rawEnd = binding.etPostContent.getSelectionEnd();
        int selStart = Math.max(0, Math.min(rawStart, rawEnd));
        int selEnd = Math.max(0, Math.max(rawStart, rawEnd));
        boolean hasPartialSelection = (selEnd > selStart)
                && (selEnd <= fullText.length())
                && !PostReviewPolicy.REWRITE_HOOK.equalsIgnoreCase(instructionType)
                && !fullText.substring(selStart, selEnd).trim().isEmpty()
                && !fullText.substring(selStart, selEnd).trim().equals(fullText.trim());

        String selectedSection = hasPartialSelection ? fullText.substring(selStart, selEnd) : null;
        String actionName = PostReviewPolicy.getInstructionDisplayName(instructionType);
        String scopeLabel = hasPartialSelection ? "đoạn đã chọn" : "bài viết";

        setRewriteLoadingState(true, "AI đang xử lý \"" + actionName + "\" cho " + scopeLabel + "...");

        orchestrator.rewriteSection(
                fullText,
                selectedSection,
                instructionType,
                activePlatform,
                workingPost != null ? workingPost.getLanguage() : "Tiếng Việt",
                new AIAgentOrchestrator.RewriteCallback() {
                    @Override
                    public void onSuccess(String rewrittenText) {
                        onSuccessWithMeta(rewrittenText, true, "AI");
                    }

                    @Override
                    public void onSuccessWithMeta(String rewrittenText, boolean usedRealLlm, String providerName) {
                        if (!isAdded() || binding == null) return;
                        setRewriteLoadingState(false, null);

                        previousTextBeforeRewrite = fullText;
                        binding.btnUndoRewrite.setVisibility(View.VISIBLE);

                        String finalUpdated = hasPartialSelection
                                ? PostReviewPolicy.applySectionReplacement(fullText, selStart, selEnd, rewrittenText)
                                : rewrittenText;

                        binding.etPostContent.setText(finalUpdated);
                        if (binding.etPostContent.getText() != null) {
                            binding.etPostContent.setSelection(binding.etPostContent.getText().length());
                        }
                        saveTextToPlatform(finalUpdated);
                        updateCharCounterAndSafety(finalUpdated);

                        String sourceNote = usedRealLlm
                                ? "✨ " + providerName + " đã áp dụng \"" + actionName + "\"!"
                                : "✨ Đã áp dụng \"" + actionName + "\" (Chế độ Demo)";
                        Toast.makeText(getContext(), sourceNote, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded() || binding == null) return;
                        setRewriteLoadingState(false, null);
                        Toast.makeText(getContext(), "⚠️ " + error, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void setRewriteLoadingState(boolean loading, String statusMessage) {
        isAiRewriteBusy = loading;
        if (binding == null) return;
        binding.layoutPreviewAiLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (statusMessage != null) {
            binding.tvPreviewAiStatus.setText(statusMessage);
        }
        binding.btnShorten.setEnabled(!loading);
        binding.btnFormal.setEnabled(!loading);
        binding.btnChangeHook.setEnabled(!loading);
        binding.btnSave.setEnabled(!loading && !isImageBusy);
    }

    // Luồng nút "Sinh lại ảnh" trong màn hình Preview
    private void setupImageRegeneration() {
        binding.btnRegenerateImage.setOnClickListener(v -> {
            if (binding == null || isImageBusy || workingPost == null) return;

            saveActiveEditorTextToPost();
            String currentContent = binding.etPostContent.getText() != null
                    ? binding.etPostContent.getText().toString()
                    : "";
            int nextVariant = workingPost.getImageVariantIndex() + 1;

            setImageLoadingState(true);
            orchestrator.regeneratePostImage(
                    workingPost.getTopic(),
                    currentContent,
                    activePlatform,
                    workingPost.getImagePrompt(),
                    nextVariant,
                    new AIAgentOrchestrator.ImageGenerationCallback() {
                        @Override
                        public void onSuccess(PostIllustrationGenerator.GeneratedIllustration illustration) {
                            if (!isAdded() || binding == null) return;
                            workingPost.setImageUrl(illustration.imageUri);
                            workingPost.setImagePrompt(illustration.imagePrompt);
                            workingPost.setImageVariantIndex(illustration.variantIndex);

                            binding.tvPreviewImagePrompt.setText("Prompt ảnh: " + illustration.imagePrompt);
                            binding.tvPreviewImageSource.setText(illustration.sourceLabel);

                            PostIllustrationGenerator.loadIntoImageView(
                                    requireContext(),
                                    illustration.imageUri,
                                    binding.ivPreviewImage,
                                    () -> {
                                        if (isAdded() && binding != null) {
                                            setImageLoadingState(false);
                                        }
                                    }
                            );

                            String msg = illustration.generatedByRealImageApi
                                    ? "🎨 Đã sinh ảnh minh họa AI mới thành công!"
                                    : "🎨 Đã tạo mẫu ảnh minh họa mới (" + illustration.sourceLabel + ")!";
                            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            if (!isAdded() || binding == null) return;
                            setImageLoadingState(false);
                            Toast.makeText(getContext(), "⚠️ " + error, Toast.LENGTH_LONG).show();
                        }
                    }
            );
        });
    }

    private void setImageLoadingState(boolean loading) {
        isImageBusy = loading;
        if (binding == null) return;
        binding.overlayPreviewImageLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnRegenerateImage.setEnabled(!loading);
        binding.btnRegenerateImage.setText(loading ? "Đang sinh..." : "🔄 Sinh lại ảnh");
        binding.btnSave.setEnabled(!loading && !isAiRewriteBusy);
    }

    // Chốt chặn an toàn Human-in-the-loop: chỉ duyệt & bàn giao đăng khi người dùng bấm xác nhận
    private void setupActionButtons() {
        binding.btnDismiss.setOnClickListener(v -> {
            saveActiveEditorTextToPost();
            workingPost.setUserApproved(false);
            if (actionListener != null) {
                actionListener.onPostUpdated(workingPost);
            }
            dismiss();
        });

        binding.btnSave.setOnClickListener(v -> {
            saveActiveEditorTextToPost();
            String activeText = binding.etPostContent.getText() != null
                    ? binding.etPostContent.getText().toString()
                    : "";

            // Kiểm tra điều kiện hợp lệ theo giới hạn kênh (Threads <= 500, Facebook <= 63,206)
            boolean isMultiChannel = workingPost.isTargetFacebook() && workingPost.isTargetThreads();
            PostReviewPolicy.PublishGateResult gate;
            if (isMultiChannel) {
                workingPost.setUserApproved(true);
                gate = PostReviewPolicy.evaluatePublishGate(workingPost, true);
                workingPost.setUserApproved(false);
            } else {
                gate = PostReviewPolicy.evaluateSingleChannelPublishGate(activeText, activePlatform, true);
            }

            if (!gate.allowed) {
                AlertDialog.Builder errDialog = new AlertDialog.Builder(requireContext())
                        .setTitle("⛔ Chưa đủ điều kiện duyệt đăng")
                        .setMessage(gate.blockReason)
                        .setPositiveButton("Quay lại chỉnh sửa", null);
                if (PostReviewPolicy.CODE_THREADS_LIMIT_EXCEEDED.equals(gate.code)) {
                    errDialog.setNeutralButton("✂️ Dùng AI rút gọn ngay", (d, w) -> {
                        TabLayout.Tab threadsTab = binding != null ? binding.tabPreviewPlatform.getTabAt(1) : null;
                        if (threadsTab != null && !threadsTab.isSelected()) {
                            threadsTab.select();
                        }
                        triggerRealAiRewrite(PostReviewPolicy.REWRITE_SHORTER);
                    });
                }
                errDialog.show();
                return;
            }

            StringBuilder confirmMsg = new StringBuilder();
            confirmMsg.append("Bạn xác nhận đã rà soát nội dung & ảnh minh họa và cho phép xuất bản?\n\n");
            if (isMultiChannel) {
                PostReviewPolicy.CharCountResult fbCount = PostReviewPolicy.evaluateCharCount(
                        workingPost.getContentFacebook(), SocialPlatform.FACEBOOK);
                PostReviewPolicy.CharCountResult thCount = PostReviewPolicy.evaluateCharCount(
                        workingPost.getContentThreads(), SocialPlatform.THREADS);
                confirmMsg.append("• Facebook Page: ").append(fbCount.formattedCounter).append(" ✅\n");
                confirmMsg.append("• Threads: ").append(thCount.formattedCounter).append(" ✅\n");
            } else {
                PostReviewPolicy.CharCountResult countInfo = PostReviewPolicy.evaluateCharCount(activeText, activePlatform);
                confirmMsg.append("• Kênh duyệt: ").append(activePlatform.getDisplayName()).append("\n");
                confirmMsg.append("• Độ dài: ").append(countInfo.formattedCounter).append(" ✅\n");
            }
            confirmMsg.append("• Ảnh minh họa: ")
                    .append(workingPost.getImageUrl() != null && !workingPost.getImageUrl().isEmpty()
                            ? "Đã sẵn sàng (Concept #" + (workingPost.getImageVariantIndex() + 1) + ")"
                            : "Chưa đính kèm ảnh")
                    .append("\n");

            if (gate.hasSafetyWarning && gate.safetyWarningMessage != null) {
                confirmMsg.append("\n⚠️ ").append(gate.safetyWarningMessage);
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("🛡️ Xác nhận duyệt bài (Human-in-the-loop)")
                    .setMessage(confirmMsg.toString())
                    .setNegativeButton("Xem lại", null)
                    .setPositiveButton("✅ Xác nhận duyệt", (d, w) -> {
                        workingPost.setUserApproved(true);
                        if (actionListener != null) {
                            actionListener.onPostUpdated(workingPost);
                            actionListener.onApprovedForPublish(workingPost, activePlatform);
                        } else {
                            Toast.makeText(getContext(), "✅ Đã duyệt & lưu nội dung bài viết!", Toast.LENGTH_SHORT).show();
                        }
                        dismiss();
                    })
                    .show();
        });
    }

    private void saveActiveEditorTextToPost() {
        if (binding == null || workingPost == null) return;
        String currentText = binding.etPostContent.getText() != null
                ? binding.etPostContent.getText().toString()
                : "";
        saveTextToPlatform(currentText);
    }

    private void saveTextToPlatform(String text) {
        if (workingPost == null) return;
        if (activePlatform == SocialPlatform.THREADS) {
            workingPost.setContentThreads(text);
        } else {
            workingPost.setContentFacebook(text);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
