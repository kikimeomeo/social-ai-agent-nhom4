package com.tuankiet.myapplication.ui.create;

import android.content.Intent;
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
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;
import com.tuankiet.myapplication.MainActivity;
import com.tuankiet.myapplication.R;
import com.tuankiet.myapplication.agent.AIAgentOrchestrator;
import com.tuankiet.myapplication.agent.PostReviewPolicy;
import com.tuankiet.myapplication.databinding.FragmentCreatePostBinding;
import com.tuankiet.myapplication.model.Post;
import com.tuankiet.myapplication.model.SocialAccount;
import com.tuankiet.myapplication.model.SocialPlatform;
import com.tuankiet.myapplication.repository.MockDataRepository;
import com.tuankiet.myapplication.ui.preview.PreviewDialogFragment;
import com.tuankiet.myapplication.util.PostIllustrationGenerator;

public class CreatePostFragment extends Fragment {

    private FragmentCreatePostBinding binding;
    private AIAgentOrchestrator orchestrator;
    private Post currentPost;
    private int currentReviewPlatform = 0; // 0: Facebook, 1: Threads, 2: LinkedIn
    private boolean isViewingFacebookTab = true;
    private boolean isSyncingReviewEditor = false;
    private boolean isRewriteBusy = false;
    private boolean isImageRegenBusy = false;
    private String previousReviewTextBeforeRewrite = null;

    private String streamFbContent = "";
    private String streamThreadsContent = "";
    private int currentImageStyleIndex = 0;

    private static final int FB_CHAR_LIMIT = PostReviewPolicy.FACEBOOK_CHAR_LIMIT;
    private static final int THREADS_CHAR_LIMIT = PostReviewPolicy.THREADS_CHAR_LIMIT;
    private static final int LI_CHAR_LIMIT = PostReviewPolicy.LINKEDIN_CHAR_LIMIT;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCreatePostBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        orchestrator = new AIAgentOrchestrator(requireContext());

        setupStep1();
        setupStep2();
        setupStep3();

        if (binding.btnOpenAgentLogs != null) {
            binding.btnOpenAgentLogs.setOnClickListener(v ->
                    com.tuankiet.myapplication.ui.logs.AgentLogsBottomSheet.show(requireContext(), getLayoutInflater()));
        }

        binding.tvStepLabel1.setOnClickListener(v -> goToStep(1));
        binding.tvStepLabel3.setOnClickListener(v -> {
            if (currentPost != null) {
                bindReviewData(currentPost);
                goToStep(3);
            }
        });

        goToStep(1);
    }

    private void goToStep(int step) {
        if (binding == null) return;

        // Container visibility
        binding.containerStep1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        binding.containerStep2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        binding.containerStep3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);

        // Stepper Visuals (Segmented Pill Tabs matching design reference)
        int activeColor = getResources().getColor(R.color.primary);
        int textInactive = getResources().getColor(R.color.text_muted);

        if (binding.tvStepCounter != null) {
            binding.tvStepCounter.setText("Bước " + step + "/3");
        }
        applyPillTab(binding.tvStepLabel1, step == 1, activeColor, textInactive);
        applyPillTab(binding.tvStepLabel2, step == 2, activeColor, textInactive);
        applyPillTab(binding.tvStepLabel3, step == 3, activeColor, textInactive);
    }

    private void applyPillTab(android.widget.TextView tab, boolean active, int activeColor, int textInactive) {
        if (active) {
            tab.setBackgroundResource(R.drawable.bg_soft_pill);
            tab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(activeColor));
            tab.setTextColor(android.graphics.Color.WHITE);
        } else {
            tab.setBackground(null);
            tab.setBackgroundTintList(null);
            tab.setTextColor(textInactive);
        }
    }

    // --- STEP 1: NHẬP LIỆU ---
    private void setupStep1() {
        binding.chipSuggest1.setOnClickListener(v -> {
            binding.etTopic.setText("5 Xu hướng Trí tuệ Nhân tạo định hình 2026");
            if (binding.tilTopic != null) binding.tilTopic.setError(null);
        });
        binding.chipSuggest2.setOnClickListener(v -> {
            binding.etTopic.setText("Tại sao On-Device AI là tương lai của Android Developer?");
            if (binding.tilTopic != null) binding.tilTopic.setError(null);
        });
        binding.chipSuggest3.setOnClickListener(v -> {
            binding.etTopic.setText("Ứng dụng kiến trúc Multi-Agent trong tự động hóa Social Media");
            if (binding.tilTopic != null) binding.tilTopic.setError(null);
        });

        // Clear error as user types
        binding.etTopic.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (binding.tilTopic != null && binding.tilTopic.getError() != null) {
                    binding.tilTopic.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnStartGenerate.setOnClickListener(v -> {
            String topic = binding.etTopic.getText() != null ? binding.etTopic.getText().toString().trim() : "";
            if (topic.isEmpty()) {
                if (binding.tilTopic != null) {
                    binding.tilTopic.setError("Vui lòng nhập chủ đề bài viết!");
                }
                binding.etTopic.requestFocus();
                Toast.makeText(getContext(), "Vui lòng nhập chủ đề bài viết!", Toast.LENGTH_SHORT).show();
                return;
            } else {
                if (binding.tilTopic != null) binding.tilTopic.setError(null);
            }

            boolean targetFb = binding.cbFacebook.isChecked();
            boolean targetThreads = binding.cbThreads.isChecked();
            boolean targetLi = binding.cbLinkedIn.isChecked();
            if (!targetFb && !targetThreads && !targetLi) {
                Toast.makeText(getContext(), "⚠️ Vui lòng chọn ít nhất 1 kênh đăng!", Toast.LENGTH_SHORT).show();
                return;
            }

            String tone = "Chuyên gia";
            if (binding.chipToneHumorous.isChecked()) {
                tone = "Hài hước";
            } else if (binding.chipToneFriendly.isChecked()) {
                tone = "Thân thiện";
            }

            String language = (binding.chipLangEn != null && binding.chipLangEn.isChecked()) ? "English" : "Tiếng Việt";

            startPipeline(topic, tone, language, targetFb, targetThreads, targetLi);
        });
    }

    // --- STEP 2: AI PIPELINE & STREAMING ---
    private void setupStep2() {
        binding.btnCancelPipeline.setOnClickListener(v -> {
            orchestrator.cancel();
            Toast.makeText(getContext(), "Đã hủy sinh bài viết", Toast.LENGTH_SHORT).show();
            goToStep(1);
        });
    }

    private void renderStreamBuffer() {
        if (binding == null) return;
        StringBuilder sb = new StringBuilder();
        if (!streamFbContent.isEmpty()) {
            sb.append("🟢 [Facebook Sub-agent - Đang tạo nội dung]\n\n")
              .append(streamFbContent)
              .append("\n\n");
        }
        if (!streamThreadsContent.isEmpty()) {
            if (!streamFbContent.isEmpty()) {
                sb.append("────────────────────────────────────────\n\n");
            }
            sb.append("🔵 [Threads Sub-agent - Tối ưu < 500 ký tự]\n\n")
              .append(streamThreadsContent)
              .append("\n");
        }
        binding.tvLiveStreamOutput.setText(sb.toString());
        binding.scrollStream.post(() -> {
            if (binding != null) binding.scrollStream.fullScroll(View.FOCUS_DOWN);
        });
    }

    private void startPipeline(String topic, String tone, String language, boolean targetFb, boolean targetThreads, boolean targetLi) {
        goToStep(2);
        streamFbContent = "";
        streamThreadsContent = "";
        binding.pbPipeline.setProgress(16);
        binding.tvLiveStreamOutput.setText("Bắt đầu khởi tạo AI Multi-Agent Pipeline...\nNgôn ngữ: " + language + "\n");

        orchestrator.generatePost(topic, tone, language, targetFb, targetThreads, targetLi, new AIAgentOrchestrator.AgentCallback() {
            @Override
            public void onStepChanged(int stepIndex, String stepTitle, String agentName) {
                onStepChanged(stepIndex, 6, stepTitle, agentName);
            }

            @Override
            public void onStepChanged(int stepIndex, int totalSteps, String stepTitle, String agentName) {
                if (binding == null) return;
                binding.tvCurrentAgent.setText(agentName);
                if (binding.tvStepCounterBadge != null) {
                    binding.tvStepCounterBadge.setText("Bước " + stepIndex + "/" + totalSteps);
                }
                binding.tvPipelineStatus.setText(stepTitle);
                int percent = (stepIndex * 100) / totalSteps;
                binding.pbPipeline.setProgress(percent);
            }

            @Override
            public void onStreamFacebookChunk(String fullCurrentText) {
                if (binding == null) return;
                streamFbContent = fullCurrentText;
                renderStreamBuffer();
            }

            @Override
            public void onStreamThreadsChunk(String fullCurrentText) {
                if (binding == null) return;
                streamThreadsContent = fullCurrentText;
                renderStreamBuffer();
            }

            @Override
            public void onStreamLinkedInChunk(String fullCurrentText) {
                if (binding == null) return;
                binding.tvLiveStreamOutput.setText("[LinkedIn Agent Output]:\n\n" + fullCurrentText);
                binding.scrollStream.fullScroll(View.FOCUS_DOWN);
            }

            @Override
            public void onCompleted(Post post) {
                if (binding == null) return;
                currentPost = post;
                bindReviewData(post);
                goToStep(3);
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                goToStep(1);
            }
        });
    }

    // --- STEP 3: DUYỆT & ĐĂNG BÀI ---
    private void setupStep3() {
        binding.tabPlatformReview.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentReviewPlatform = tab.getPosition();
                isViewingFacebookTab = (currentReviewPlatform == 0);
                previousReviewTextBeforeRewrite = null;
                binding.btnUndoRewrite.setVisibility(View.GONE);
                updateReviewTabContent();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        binding.etReviewContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isSyncingReviewEditor || binding == null) return;
                String currentText = s != null ? s.toString() : "";
                updateCharCounterAndSafety(currentText);

                // Keep edited text in currentPost & require fresh explicit approval after edits
                if (currentPost != null) {
                    currentPost.setUserApproved(false);
                    if (currentReviewPlatform == 0) {
                        currentPost.setContentFacebook(currentText);
                    } else if (currentReviewPlatform == 1) {
                        currentPost.setContentThreads(currentText);
                    } else {
                        currentPost.setContentLinkedIn(currentText);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Quick AI Rewrite Assistant Listeners (Connected to REAL LLM API)
        binding.btnRewriteShorter.setOnClickListener(v -> performRealAiRewrite(PostReviewPolicy.REWRITE_SHORTER));
        binding.btnRewriteFormal.setOnClickListener(v -> performRealAiRewrite(PostReviewPolicy.REWRITE_FORMAL));
        binding.btnRewriteHook.setOnClickListener(v -> performRealAiRewrite(PostReviewPolicy.REWRITE_HOOK));

        binding.btnUndoRewrite.setOnClickListener(v -> {
            if (previousReviewTextBeforeRewrite != null && binding != null) {
                binding.etReviewContent.setText(previousReviewTextBeforeRewrite);
                binding.etReviewContent.setSelection(binding.etReviewContent.getText().length());
                previousReviewTextBeforeRewrite = null;
                binding.btnUndoRewrite.setVisibility(View.GONE);
                Toast.makeText(getContext(), "↩ Đã khôi phục phiên bản trước khi viết lại", Toast.LENGTH_SHORT).show();
            }
        });

        // Visual Agent: Sinh lại ảnh minh họa mới ngay tại màn hình Review
        binding.btnRegenerateImage.setOnClickListener(v -> triggerRegenerateImage());

        // Mở màn hình Preview đầy đủ (PreviewDialogFragment) và đồng bộ ngược lại Step 3
        binding.btnOpenPreviewDialog.setOnClickListener(v -> {
            if (currentPost == null) return;
            PreviewDialogFragment dialog = PreviewDialogFragment.newInstance(currentPost, getActiveReviewPlatform());
            dialog.setOnPreviewActionListener(new PreviewDialogFragment.OnPreviewActionListener() {
                @Override
                public void onPostUpdated(Post updatedPost) {
                    if (!isAdded() || binding == null || updatedPost == null) return;
                    currentPost.setContentFacebook(updatedPost.getContentFacebook());
                    currentPost.setContentThreads(updatedPost.getContentThreads());
                    currentPost.setImageUrl(updatedPost.getImageUrl());
                    currentPost.setImagePrompt(updatedPost.getImagePrompt());
                    currentPost.setImageVariantIndex(updatedPost.getImageVariantIndex());
                    currentPost.setUserApproved(updatedPost.isUserApproved());
                    updateReviewTabContent();
                    bindReviewImage(currentPost, null);
                }

                @Override
                public void onApprovedForPublish(Post approvedPost, SocialPlatform activePlatform) {
                    if (!isAdded() || binding == null || approvedPost == null) return;
                    currentPost.setContentFacebook(approvedPost.getContentFacebook());
                    currentPost.setContentThreads(approvedPost.getContentThreads());
                    currentPost.setImageUrl(approvedPost.getImageUrl());
                    currentPost.setImagePrompt(approvedPost.getImagePrompt());
                    currentPost.setImageVariantIndex(approvedPost.getImageVariantIndex());
                    currentPost.setUserApproved(true);
                    executeApprovedDemoOrOfflinePublish();
                }
            });
            dialog.show(getParentFragmentManager(), "PreviewDialogFragment");
        });

        binding.btnRetryCurrentChannel.setOnClickListener(v -> {
            if (currentPost == null) return;
            Toast.makeText(getContext(), "Đang yêu cầu AI viết lại kênh này...", Toast.LENGTH_SHORT).show();
            goToStep(2);
            streamFbContent = "";
            streamThreadsContent = "";
            orchestrator.retryChannel(currentPost, currentReviewPlatform, new AIAgentOrchestrator.AgentCallback() {
                @Override
                public void onStepChanged(int stepIndex, String stepTitle, String agentName) {
                    onStepChanged(stepIndex, 6, stepTitle, agentName);
                }

                @Override
                public void onStepChanged(int stepIndex, int totalSteps, String stepTitle, String agentName) {
                    if (binding == null) return;
                    binding.tvCurrentAgent.setText(agentName);
                    if (binding.tvStepCounterBadge != null) {
                        binding.tvStepCounterBadge.setText("Bước " + stepIndex + "/" + totalSteps);
                    }
                    binding.tvPipelineStatus.setText(stepTitle);
                    int percent = (stepIndex * 100) / totalSteps;
                    binding.pbPipeline.setProgress(percent);
                }

                @Override
                public void onStreamFacebookChunk(String fullCurrentText) {
                    if (binding == null) return;
                    streamFbContent = fullCurrentText;
                    renderStreamBuffer();
                }

                @Override
                public void onStreamThreadsChunk(String fullCurrentText) {
                    if (binding == null) return;
                    streamThreadsContent = fullCurrentText;
                    renderStreamBuffer();
                }

                @Override
                public void onStreamLinkedInChunk(String fullCurrentText) {
                    if (binding == null) return;
                    binding.tvLiveStreamOutput.setText("[LinkedIn Retry Output]:\n\n" + fullCurrentText);
                }

                @Override
                public void onCompleted(Post post) {
                    if (binding == null) return;
                    currentPost = post;
                    bindReviewData(post);
                    goToStep(3);
                }

                @Override
                public void onError(String message) {
                    if (binding == null) return;
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    goToStep(3);
                }
            });
        });

        binding.btnRemoveImage.setOnClickListener(v -> {
            if (currentPost == null) return;
            currentPost.setImageUrl("");
            currentPost.setUserApproved(false);
            bindReviewImage(currentPost, null);
            Toast.makeText(getContext(), "Đã gỡ ảnh đính kèm", Toast.LENGTH_SHORT).show();
        });

        binding.btnRestoreImage.setOnClickListener(v -> triggerRegenerateImage());

        // ĐĂNG THẬT LÊN CÁC APP TRÊN MÁY (BottomSheet chuyên nghiệp, tự tách nội dung đúng kênh)
        binding.btnShareToRealApp.setOnClickListener(v -> {
            if (currentPost == null) return;
            String currentTabContent = binding.etReviewContent.getText().toString();
            String fbContent = (currentPost.getContentFacebook() != null && !currentPost.getContentFacebook().trim().isEmpty())
                    ? currentPost.getContentFacebook() : currentTabContent;
            String threadsContent = (currentPost.getContentThreads() != null && !currentPost.getContentThreads().trim().isEmpty())
                    ? currentPost.getContentThreads() : currentTabContent;

            if (fbContent.trim().isEmpty() && threadsContent.trim().isEmpty()) {
                Toast.makeText(getContext(), "Nội dung bài viết đang trống!", Toast.LENGTH_SHORT).show();
                return;
            }

            com.google.android.material.bottomsheet.BottomSheetDialog sheetDialog =
                    new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
            com.tuankiet.myapplication.databinding.DialogPublishOptionsBinding sheetBinding =
                    com.tuankiet.myapplication.databinding.DialogPublishOptionsBinding.inflate(getLayoutInflater());
            sheetDialog.setContentView(sheetBinding.getRoot());

            boolean showThreads = currentPost.isTargetThreads() || !threadsContent.trim().isEmpty();
            boolean showFb = currentPost.isTargetFacebook() || !fbContent.trim().isEmpty();

            sheetBinding.cardOptThreadsApi.setVisibility(showThreads ? View.VISIBLE : View.GONE);
            sheetBinding.cardOptOpenThreads.setVisibility(showThreads ? View.VISIBLE : View.GONE);
            sheetBinding.cardOptFacebookApi.setVisibility(showFb ? View.VISIBLE : View.GONE);
            sheetBinding.cardOptOpenFacebook.setVisibility(showFb ? View.VISIBLE : View.GONE);

            sheetBinding.cardOptThreadsApi.setOnClickListener(opt -> {
                sheetDialog.dismiss();
                PostReviewPolicy.PublishGateResult gate = PostReviewPolicy.evaluateSingleChannelPublishGate(
                        threadsContent, SocialPlatform.THREADS, true
                );
                if (!gate.allowed) {
                    showPublishBlockedDialog(gate);
                    return;
                }
                openThreadsPublishDialog(threadsContent);
            });

            sheetBinding.cardOptFacebookApi.setOnClickListener(opt -> {
                sheetDialog.dismiss();
                PostReviewPolicy.PublishGateResult gate = PostReviewPolicy.evaluateSingleChannelPublishGate(
                        fbContent, SocialPlatform.FACEBOOK, true
                );
                if (!gate.allowed) {
                    showPublishBlockedDialog(gate);
                    return;
                }
                openMetaGraphPublishDialog(fbContent);
            });

            sheetBinding.cardOptOpenThreads.setOnClickListener(opt -> {
                sheetDialog.dismiss();
                com.tuankiet.myapplication.util.RealSocialPublisher.shareToThreads(requireActivity(), threadsContent);
            });

            sheetBinding.cardOptOpenFacebook.setOnClickListener(opt -> {
                sheetDialog.dismiss();
                com.tuankiet.myapplication.util.RealSocialPublisher.shareToRealApp(
                        requireActivity(), SocialPlatform.FACEBOOK, fbContent);
            });

            sheetBinding.btnOptShareSystem.setOnClickListener(opt -> {
                sheetDialog.dismiss();
                Intent shareIntent = new Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, currentTabContent);
                startActivity(Intent.createChooser(shareIntent, "Đăng bài SocialAI lên:"));
            });

            sheetDialog.show();
        });

        // ĐĂNG TỰ ĐỘNG QUA META GRAPH API (Nút cấu hình nhanh)
        binding.btnPublishMetaApi.setOnClickListener(v -> {
            if (currentPost == null) return;
            String content = binding.etReviewContent.getText().toString();
            PostReviewPolicy.PublishGateResult gate = PostReviewPolicy.evaluateSingleChannelPublishGate(
                    content, SocialPlatform.FACEBOOK, true
            );
            if (!gate.allowed) {
                showPublishBlockedDialog(gate);
                return;
            }
            openMetaGraphPublishDialog(content);
        });

        // Save Draft (Giữ nháp offline — KHÔNG đăng bài)
        binding.btnSaveDraft.setOnClickListener(v -> {
            if (currentPost != null) {
                currentPost.setStatus(Post.STATUS_DRAFT);
                currentPost.setUserApproved(false);
                MockDataRepository.getInstance(requireContext()).updatePost(currentPost);
                Toast.makeText(getContext(), "Đã lưu bản nháp offline thành công!", Toast.LENGTH_LONG).show();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToTab(R.id.nav_home);
                }
            }
        });

        // Publish Now (CHỐT CHẶN AN TOÀN HUMAN-IN-THE-LOOP: bắt buộc xác nhận duyệt trước khi đăng)
        binding.btnPublishNow.setOnClickListener(v -> {
            if (currentPost == null) return;
            requestHumanApprovalBeforePublish();
        });
    }

    private SocialPlatform getActiveReviewPlatform() {
        if (currentReviewPlatform == 1) return SocialPlatform.THREADS;
        if (currentReviewPlatform == 2) return SocialPlatform.LINKEDIN;
        return SocialPlatform.FACEBOOK;
    }

    private void updateCharCounterAndSafety(String content) {
        if (binding == null || !isAdded()) return;
        SocialPlatform platform = getActiveReviewPlatform();
        PostReviewPolicy.CharCountResult countResult = PostReviewPolicy.evaluateCharCount(content, platform);
        binding.tvCharCounter.setText(countResult.formattedCounter);

        if (countResult.isOverLimit) {
            binding.tvCharCounter.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_red));
        } else if (countResult.isNearLimit) {
            binding.tvCharCounter.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_amber));
        } else {
            binding.tvCharCounter.setTextColor(Color.parseColor("#64748B"));
        }

        AIAgentOrchestrator.SafetyCheckResult safety = orchestrator.checkContentSafety(content);
        if (safety.hasWarning && safety.warningMessage != null) {
            binding.cardContentWarning.setVisibility(View.VISIBLE);
            binding.tvContentWarningText.setText(safety.warningMessage);
        } else {
            binding.cardContentWarning.setVisibility(View.GONE);
        }
    }

    private void performRealAiRewrite(String instructionType) {
        if (binding == null || isRewriteBusy) return;

        String fullText = binding.etReviewContent.getText() != null
                ? binding.etReviewContent.getText().toString()
                : "";
        if (fullText.trim().isEmpty()) {
            Toast.makeText(getContext(), "Nội dung bài viết đang trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        int rawStart = binding.etReviewContent.getSelectionStart();
        int rawEnd = binding.etReviewContent.getSelectionEnd();
        int selStart = Math.max(0, Math.min(rawStart, rawEnd));
        int selEnd = Math.max(0, Math.max(rawStart, rawEnd));
        boolean hasPartialSelection = (selEnd > selStart)
                && (selEnd <= fullText.length())
                && !PostReviewPolicy.REWRITE_HOOK.equalsIgnoreCase(instructionType)
                && !fullText.substring(selStart, selEnd).trim().isEmpty()
                && !fullText.substring(selStart, selEnd).trim().equals(fullText.trim());

        String selectedSection = hasPartialSelection ? fullText.substring(selStart, selEnd) : null;
        SocialPlatform activePlatform = getActiveReviewPlatform();
        String actionName = PostReviewPolicy.getInstructionDisplayName(instructionType);
        String scopeLabel = hasPartialSelection ? "đoạn đã chọn" : "bài viết (" + activePlatform.getShortName() + ")";

        setReviewRewriteLoading(true, "AI đang xử lý \"" + actionName + "\" cho " + scopeLabel + "...");

        orchestrator.rewriteSection(
                fullText,
                selectedSection,
                instructionType,
                activePlatform,
                currentPost != null ? currentPost.getLanguage() : "Tiếng Việt",
                new AIAgentOrchestrator.RewriteCallback() {
                    @Override
                    public void onSuccess(String rewrittenText) {
                        onSuccessWithMeta(rewrittenText, true, "AI");
                    }

                    @Override
                    public void onSuccessWithMeta(String rewrittenText, boolean usedRealLlm, String providerName) {
                        if (!isAdded() || binding == null) return;
                        setReviewRewriteLoading(false, null);

                        previousReviewTextBeforeRewrite = fullText;
                        binding.btnUndoRewrite.setVisibility(View.VISIBLE);

                        String finalUpdated = hasPartialSelection
                                ? PostReviewPolicy.applySectionReplacement(fullText, selStart, selEnd, rewrittenText)
                                : rewrittenText;

                        binding.etReviewContent.setText(finalUpdated);
                        binding.etReviewContent.setSelection(binding.etReviewContent.getText().length());
                        updateCharCounterAndSafety(finalUpdated);

                        String toastMsg = usedRealLlm
                                ? "✨ " + providerName + " đã cập nhật \"" + actionName + "\"!"
                                : "✨ Đã cập nhật \"" + actionName + "\" (Chế độ Demo)";
                        Toast.makeText(getContext(), toastMsg, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded() || binding == null) return;
                        setReviewRewriteLoading(false, null);
                        Toast.makeText(getContext(), "⚠️ " + error, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void setReviewRewriteLoading(boolean loading, String statusMessage) {
        isRewriteBusy = loading;
        if (binding == null) return;
        binding.layoutRewriteLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (statusMessage != null) {
            binding.tvRewriteLoadingText.setText(statusMessage);
        }
        binding.btnRewriteShorter.setEnabled(!loading);
        binding.btnRewriteFormal.setEnabled(!loading);
        binding.btnRewriteHook.setEnabled(!loading);
        binding.btnPublishNow.setEnabled(!loading && !isImageRegenBusy);
    }

    private void triggerRegenerateImage() {
        if (binding == null || currentPost == null || isImageRegenBusy) return;

        String currentContent = binding.etReviewContent.getText() != null
                ? binding.etReviewContent.getText().toString()
                : "";
        SocialPlatform platform = getActiveReviewPlatform();
        int nextVariant = currentPost.getImageVariantIndex() + 1;

        binding.cardReviewImage.setVisibility(View.VISIBLE);
        binding.btnRestoreImage.setVisibility(View.GONE);
        setReviewImageLoading(true);
        orchestrator.regeneratePostImage(
                currentPost.getTopic(),
                currentContent,
                platform,
                currentPost.getImagePrompt(),
                nextVariant,
                new AIAgentOrchestrator.ImageGenerationCallback() {
                    @Override
                    public void onSuccess(PostIllustrationGenerator.GeneratedIllustration illustration) {
                        if (!isAdded() || binding == null) return;
                        currentPost.setImageUrl(illustration.imageUri);
                        currentPost.setImagePrompt(illustration.imagePrompt);
                        currentPost.setImageVariantIndex(illustration.variantIndex);
                        currentPost.setUserApproved(false);

                        bindReviewImage(currentPost, illustration.sourceLabel);

                        String msg = illustration.generatedByRealImageApi
                                ? "🎨 Đã sinh ảnh minh họa AI mới thành công!"
                                : "🎨 Đã tạo ảnh minh họa mới (" + illustration.sourceLabel + ")!";
                        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded() || binding == null) return;
                        setReviewImageLoading(false);
                        Toast.makeText(getContext(), "⚠️ " + error, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void setReviewImageLoading(boolean loading) {
        isImageRegenBusy = loading;
        if (binding == null) return;
        binding.overlayReviewImageLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnRegenerateImage.setEnabled(!loading);
        binding.btnRegenerateImage.setText(loading ? "Đang sinh..." : "🔄 Sinh lại ảnh");
        binding.btnPublishNow.setEnabled(!loading && !isRewriteBusy);
    }

    private void bindReviewImage(Post post, String customSourceLabel) {
        if (binding == null || post == null || !isAdded()) return;
        boolean hasImage = post.getImageUrl() != null && !post.getImageUrl().trim().isEmpty();
        if (!hasImage) {
            binding.cardReviewImage.setVisibility(View.GONE);
            binding.btnRestoreImage.setVisibility(View.VISIBLE);
            binding.tvImageSourceBadge.setText("Không đính kèm ảnh (Thuần chữ)");
            binding.tvImagePromptPreview.setText("Đã gỡ ảnh (Bấm 'Thêm ảnh AI' nếu muốn tạo lại)");
            setReviewImageLoading(false);
            return;
        }

        binding.cardReviewImage.setVisibility(View.VISIBLE);
        binding.btnRestoreImage.setVisibility(View.GONE);
        binding.tvImagePromptPreview.setText(
                post.getImagePrompt() != null && !post.getImagePrompt().trim().isEmpty()
                        ? "Prompt ảnh: " + post.getImagePrompt().trim()
                        : "Prompt ảnh: Minimal 3D tech illustration"
        );
        String sourceLabel = (customSourceLabel != null && !customSourceLabel.isEmpty())
                ? customSourceLabel
                : "Visual Agent • Concept #" + (post.getImageVariantIndex() + 1);
        binding.tvImageSourceBadge.setText(sourceLabel);

        PostIllustrationGenerator.loadIntoImageView(
                requireContext(),
                post.getImageUrl(),
                binding.ivReviewImagePreview,
                () -> {
                    if (isAdded() && binding != null) {
                        setReviewImageLoading(false);
                    }
                }
        );
    }

    private void requestHumanApprovalBeforePublish() {
        if (currentPost == null || binding == null) return;

        // Pre-validate with a temporary probe so user gets clear feedback before approval dialog
        boolean previousApproved = currentPost.isUserApproved();
        currentPost.setUserApproved(true);
        PostReviewPolicy.PublishGateResult gate = PostReviewPolicy.evaluatePublishGate(currentPost, true);
        currentPost.setUserApproved(previousApproved);

        if (!gate.allowed) {
            showPublishBlockedDialog(gate);
            return;
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Bạn xác nhận đã duyệt nội dung & ảnh minh họa để hệ thống tiến hành đăng bài?\n\n");
        if (currentPost.isTargetFacebook()) {
            PostReviewPolicy.CharCountResult fbCount = PostReviewPolicy.evaluateCharCount(
                    currentPost.getContentFacebook(), SocialPlatform.FACEBOOK);
            summary.append("• Facebook Page: ").append(fbCount.formattedCounter).append(" ✅\n");
        }
        if (currentPost.isTargetThreads()) {
            PostReviewPolicy.CharCountResult thCount = PostReviewPolicy.evaluateCharCount(
                    currentPost.getContentThreads(), SocialPlatform.THREADS);
            summary.append("• Threads: ").append(thCount.formattedCounter).append(" ✅\n");
        }
        if (currentPost.isTargetLinkedIn()) {
            PostReviewPolicy.CharCountResult liCount = PostReviewPolicy.evaluateCharCount(
                    currentPost.getContentLinkedIn(), SocialPlatform.LINKEDIN);
            summary.append("• LinkedIn: ").append(liCount.formattedCounter).append(" ✅\n");
        }
        summary.append("• Ảnh minh họa: Concept #").append(currentPost.getImageVariantIndex() + 1).append("\n");

        if (gate.hasSafetyWarning && gate.safetyWarningMessage != null) {
            summary.append("\n⚠️ CẢNH BÁO AN TOÀN NỘI DUNG:\n").append(gate.safetyWarningMessage);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("🛡️ Xác nhận duyệt bài (Human-in-the-loop)")
                .setMessage(summary.toString())
                .setNegativeButton("Quay lại chỉnh sửa", null)
                .setPositiveButton("✅ Xác nhận duyệt & Đăng", (dialog, which) -> {
                    currentPost.setUserApproved(true);
                    executeApprovedDemoOrOfflinePublish();
                })
                .show();
    }

    private void showPublishBlockedDialog(PostReviewPolicy.PublishGateResult gate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle("⛔ Chặn xuất bản — Cần chỉnh sửa")
                .setMessage(gate.blockReason)
                .setPositiveButton("Đóng & Sửa bài", null);

        if (PostReviewPolicy.CODE_THREADS_LIMIT_EXCEEDED.equals(gate.code)) {
            builder.setNeutralButton("✂️ Dùng AI rút gọn Threads", (d, w) -> {
                TabLayout.Tab threadsTab = binding != null ? binding.tabPlatformReview.getTabAt(1) : null;
                if (threadsTab != null) {
                    threadsTab.select();
                }
                performRealAiRewrite(PostReviewPolicy.REWRITE_SHORTER);
            });
        }
        builder.show();
    }

    private void executeApprovedDemoOrOfflinePublish() {
        if (currentPost == null) return;

        // Defense-in-depth: strictly re-verify Human-in-the-loop gate before mutating status
        PostReviewPolicy.PublishGateResult finalGate = PostReviewPolicy.evaluatePublishGate(currentPost, currentPost.isUserApproved());
        if (!finalGate.allowed) {
            showPublishBlockedDialog(finalGate);
            return;
        }

        // Đảm bảo có Idempotency Key duy nhất cho bài viết
        if (currentPost.getIdempotencyKey() == null || currentPost.getIdempotencyKey().trim().isEmpty()) {
            currentPost.setIdempotencyKey(java.util.UUID.randomUUID().toString());
        }

        currentPost.setStatus(Post.STATUS_PENDING);

        // Lưu vào SQLite
        if (currentPost.getId() == 0) {
            long newId = com.tuankiet.myapplication.data.DatabaseHelper.getInstance(requireContext()).insertPost(currentPost);
            currentPost.setId(newId);
        } else {
            com.tuankiet.myapplication.data.DatabaseHelper.getInstance(requireContext()).updatePost(currentPost);
        }

        boolean isOnline = com.tuankiet.myapplication.util.NetworkUtils.isNetworkAvailable(requireContext());
        if (isOnline) {
            // Xuất bản trực tiếp qua WorkManager với theo dõi trạng thái Realtime
            AlertDialog progressDialog = new AlertDialog.Builder(requireContext())
                    .setTitle("🚀 Đang Xuất Bản Đa Kênh (F5)")
                    .setMessage("WorkManager đang điều phối xuất bản đồng thời và kiểm tra chống trùng lặp (Idempotency)...\n\nVui lòng đợi trong giây lát.")
                    .setCancelable(false)
                    .create();
            progressDialog.show();

            java.util.UUID workId = com.tuankiet.myapplication.worker.PostPublisherWorker.schedulePostPublish(
                    requireContext(),
                    currentPost.getId(),
                    currentPost.getIdempotencyKey()
            );

            androidx.work.WorkManager.getInstance(requireContext())
                    .getWorkInfoByIdLiveData(workId)
                    .observe(getViewLifecycleOwner(), workInfo -> {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }

                            Post updatedPost = com.tuankiet.myapplication.data.DatabaseHelper.getInstance(requireContext()).getPostById(currentPost.getId());
                            if (updatedPost == null) updatedPost = currentPost;

                            if (workInfo.getState() == androidx.work.WorkInfo.State.SUCCEEDED) {
                                com.tuankiet.myapplication.worker.CommentSyncWorker.scheduleSimulatedSync(requireContext(), 12);

                                StringBuilder sb = new StringBuilder();
                                sb.append("🎉 ĐÃ XUẤT BẢN THÀNH CÔNG ĐA KÊNH!\n\n");
                                sb.append("• Idempotency Key: ").append(updatedPost.getIdempotencyKey()).append("\n\n");

                                String primaryUrl = null;
                                if (updatedPost.isTargetFacebook()) {
                                    String fbUrl = updatedPost.getFbPostUrl() != null ? updatedPost.getFbPostUrl() : "https://facebook.com/socialai/posts/" + System.currentTimeMillis() % 100000;
                                    sb.append("• Facebook Page: ✅ ĐÃ ĐĂNG\n  URL: ").append(fbUrl).append("\n\n");
                                    if (primaryUrl == null) primaryUrl = fbUrl;
                                }
                                if (updatedPost.isTargetThreads()) {
                                    String thUrl = updatedPost.getThreadsPostUrl() != null ? updatedPost.getThreadsPostUrl() : "https://www.threads.net/@socialai/post/" + System.currentTimeMillis() % 100000;
                                    sb.append("• Threads: ✅ ĐÃ ĐĂNG\n  URL: ").append(thUrl).append("\n\n");
                                    if (primaryUrl == null) primaryUrl = thUrl;
                                }
                                if (updatedPost.isTargetLinkedIn()) {
                                    sb.append("• LinkedIn: ✅ ĐÃ ĐĂNG\n\n");
                                }

                                sb.append("Bài viết đã được lưu vết trong SQLite và bắt đầu theo dõi bình luận tự động (F6).");

                                final String finalUrl = primaryUrl;
                                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                                        .setTitle("🎉 Đã Duyệt & Xuất Bản Thành Công (F5)")
                                        .setMessage(sb.toString())
                                        .setCancelable(false);

                                if (finalUrl != null && (finalUrl.startsWith("http://") || finalUrl.startsWith("https://"))) {
                                    builder.setPositiveButton("Mở bài viết gốc", (dialog, which) -> {
                                        try {
                                            Intent openUrl = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(finalUrl));
                                            startActivity(openUrl);
                                        } catch (Exception ignored) {}
                                        if (getActivity() instanceof MainActivity) {
                                            ((MainActivity) getActivity()).navigateToTab(R.id.nav_home);
                                        }
                                    });
                                    builder.setNegativeButton("Về Tổng quan", (dialog, which) -> {
                                        if (getActivity() instanceof MainActivity) {
                                            ((MainActivity) getActivity()).navigateToTab(R.id.nav_home);
                                        }
                                    });
                                } else {
                                    builder.setPositiveButton("Xem trên trang Tổng quan", (dialog, which) -> {
                                        if (getActivity() instanceof MainActivity) {
                                            ((MainActivity) getActivity()).navigateToTab(R.id.nav_home);
                                        }
                                    });
                                }

                                builder.show();
                            } else {
                                String errorMsg = workInfo.getOutputData().getString(com.tuankiet.myapplication.worker.PostPublisherWorker.OUTPUT_ERROR_MESSAGE);
                                if (errorMsg == null || errorMsg.isEmpty()) errorMsg = updatedPost.getPublishError();
                                if (errorMsg == null || errorMsg.isEmpty()) errorMsg = "Lỗi xác thực hoặc máy chủ từ chối xuất bản bài viết.";

                                new AlertDialog.Builder(requireContext())
                                        .setTitle("⚠️ Lỗi Xuất Bản Đa Kênh (F5)")
                                        .setMessage("Không thể hoàn tất đăng bài:\n\n" + errorMsg + "\n\nBài viết đã được đánh dấu trạng thái THẤT BẠI. Bạn có thể kiểm tra cấu hình tài khoản hoặc thử lại sau.")
                                        .setPositiveButton("Đã hiểu", (dialog, which) -> {
                                            if (getActivity() instanceof MainActivity) {
                                                ((MainActivity) getActivity()).navigateToTab(R.id.nav_home);
                                            }
                                        })
                                        .show();
                            }
                        }
                    });
        } else {
            // Chế độ Offline Queue
            com.tuankiet.myapplication.worker.PostPublisherWorker.schedulePostPublish(
                    requireContext(),
                    currentPost.getId(),
                    currentPost.getIdempotencyKey()
            );
            com.tuankiet.myapplication.worker.PostPublisherWorker.scheduleOfflinePublisher(requireContext());

            new AlertDialog.Builder(requireContext())
                    .setTitle("📡 Chế độ Ngoại Tuyến (Offline Queue)")
                    .setMessage("Thiết bị hiện đang mất kết nối Internet.\n\n"
                            + "• Mã chống trùng lặp (Idempotency Key):\n  " + currentPost.getIdempotencyKey() + "\n\n"
                            + "• Quy chuẩn F5: Android WorkManager đã lưu bài viết vào hàng đợi ngoại tuyến với ràng buộc mạng CONNECTED và Exponential Backoff Retry.\n\n"
                            + "Ngay khi có mạng trở lại, hệ thống sẽ tự động xuất bản ngầm lên Facebook & Threads mà không làm trùng bài!")
                    .setPositiveButton("Về trang Tổng quan", (dialog, which) -> {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).navigateToTab(R.id.nav_home);
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    private void bindReviewData(Post post) {
        if (post == null || binding == null) return;
        previousReviewTextBeforeRewrite = null;
        binding.btnUndoRewrite.setVisibility(View.GONE);
        bindReviewImage(post, null);

        if (post.isTargetFacebook()) {
            currentReviewPlatform = 0;
        } else if (post.isTargetThreads()) {
            currentReviewPlatform = 1;
        } else {
            currentReviewPlatform = 0;
        }
        isViewingFacebookTab = (currentReviewPlatform == 0);

        if (binding.tabPlatformReview.getTabAt(currentReviewPlatform) != null) {
            binding.tabPlatformReview.getTabAt(currentReviewPlatform).select();
        }
        updateReviewTabContent();
    }

    private String resolveConnectedAuthorLabel(SocialPlatform platform, String fallbackLabel) {
        if (getContext() == null) return fallbackLabel;
        for (SocialAccount acc : MockDataRepository.getInstance(requireContext()).getAccounts()) {
            if (acc.getPlatform() == platform) {
                if (platform == SocialPlatform.THREADS && acc.getHandle() != null && !acc.getHandle().trim().isEmpty()) {
                    return acc.getHandle().trim() + " (Threads)";
                }
                if (acc.getAccountName() != null && !acc.getAccountName().trim().isEmpty()) {
                    return acc.getAccountName().trim() + (platform == SocialPlatform.FACEBOOK ? " (Facebook Page)" : " (Threads)");
                }
            }
        }
        return fallbackLabel;
    }

    private void updateReviewTabContent() {
        if (currentPost == null || binding == null) return;

        String contentToShow;
        if (currentReviewPlatform == 0) {
            binding.ivReviewPlatformIcon.setImageResource(R.drawable.ic_facebook);
            binding.tvReviewAuthorName.setText(resolveConnectedAuthorLabel(SocialPlatform.FACEBOOK, "Facebook Page"));
            binding.tvReviewSubInfo.setText("Vừa xong • 🌐 Công khai");
            contentToShow = currentPost.getContentFacebook();
        } else {
            binding.ivReviewPlatformIcon.setImageResource(R.drawable.ic_threads);
            binding.tvReviewAuthorName.setText(resolveConnectedAuthorLabel(SocialPlatform.THREADS, "Threads Account"));
            binding.tvReviewSubInfo.setText("Vừa xong • 🧵 Cuộc trò chuyện mở");
            contentToShow = currentPost.getContentThreads();
        }

        isSyncingReviewEditor = true;
        binding.etReviewContent.setText(contentToShow);
        isSyncingReviewEditor = false;
        updateCharCounterAndSafety(contentToShow);
    }

    private void openThreadsPublishDialog(String content) {
        com.google.android.material.bottomsheet.BottomSheetDialog threadsDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        com.tuankiet.myapplication.databinding.DialogThreadsPublishBinding dialogBinding =
                com.tuankiet.myapplication.databinding.DialogThreadsPublishBinding.inflate(getLayoutInflater());
        threadsDialog.setContentView(dialogBinding.getRoot());

        com.tuankiet.myapplication.data.SecurePreferenceManager prefs =
                com.tuankiet.myapplication.data.SecurePreferenceManager.getInstance(requireContext());
        String savedToken = prefs.getThreadsToken();
        if (savedToken != null && !savedToken.isEmpty()) {
            dialogBinding.etThreadsAccessToken.setText(savedToken);
        }
        String savedUserId = prefs.getThreadsUserId();
        if (savedUserId != null && !savedUserId.isEmpty() && !"me".equals(savedUserId)) {
            dialogBinding.etThreadsUserId.setText(savedUserId);
        }

        dialogBinding.btnCancelThreadsDialog.setOnClickListener(d -> threadsDialog.dismiss());
        dialogBinding.btnSubmitThreadsPublish.setOnClickListener(d -> {
            String token = dialogBinding.etThreadsAccessToken.getText().toString().trim();
            String userId = dialogBinding.etThreadsUserId.getText().toString().trim();

            if (token.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng dán Threads User Access Token!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Preserve existing authenticated profile metadata instead of overwriting with defaults
            String effectiveUserId = !userId.isEmpty() ? userId : prefs.getThreadsUserId();
            if (effectiveUserId == null || effectiveUserId.trim().isEmpty()) {
                effectiveUserId = "me";
            }
            String existingUsername = prefs.getThreadsUsername();
            if (existingUsername == null || existingUsername.trim().isEmpty()) {
                existingUsername = "threads_user";
            }
            prefs.saveThreadsAccount(effectiveUserId, existingUsername, token, prefs.getThreadsRefreshToken(), 60L * 86400L);

            Toast.makeText(getContext(), "Đang gửi bài viết lên Official Threads API...", Toast.LENGTH_SHORT).show();
            final android.content.Context appContext =
                    requireContext().getApplicationContext();
            com.tuankiet.myapplication.util.RealSocialPublisher.postToThreadsViaApi(userId, token, content, new com.tuankiet.myapplication.util.RealSocialPublisher.ApiPublishCallback() {
                @Override
                public void onSuccess(String realPostUrl, String postId) {

                    // Lưu kết quả vào SQLite trên luồng nền để không làm đơ giao diện
                    new Thread(() -> {

                        boolean savedSuccessfully = false;

                        try {
                            long rowId = com.tuankiet.myapplication.data.DatabaseHelper
                                    .getInstance(appContext)
                                    .saveThreadsPublication(
                                            postId,
                                            realPostUrl,
                                            content
                                    );

                            savedSuccessfully = rowId != -1L;

                        } catch (Exception e) {
                            android.util.Log.e(
                                    "ThreadsPublishing",
                                    "Không thể lưu bài đăng Threads vào SQLite",
                                    e
                            );
                        }

                        final boolean finalSaved = savedSuccessfully;

                        // Quay về luồng chính để cập nhật giao diện Android
                        new android.os.Handler(
                                android.os.Looper.getMainLooper()
                        ).post(() -> {

                            if (!isAdded()) return;

                            threadsDialog.dismiss();

                            boolean hasLink = realPostUrl != null
                                    && !realPostUrl.trim().isEmpty();

                            String message = "Meta đã xác nhận xuất bản bài viết.\n\n"
                                    + "ID bài đăng Threads: " + postId;

                            if (hasLink) {
                                message += "\n\nĐường dẫn bài viết: " + realPostUrl;
                            } else {
                                message += "\n\nChưa lấy được đường dẫn bài viết từ Threads API.";
                            }

                            if (finalSaved) {
                                message += "\n\nĐã lưu kết quả xuất bản vào cơ sở dữ liệu SQLite.";
                            } else {
                                message += "\n\nKhông thể lưu kết quả vào cơ sở dữ liệu SQLite."
                                        + "\nBài viết vẫn đã được Meta xác nhận xuất bản."
                                        + "\nVui lòng không đăng lại ngay để tránh trùng bài.";
                            }

                            androidx.appcompat.app.AlertDialog.Builder builder =
                                    new androidx.appcompat.app.AlertDialog.Builder(
                                            requireContext()
                                    )
                                            .setTitle("KẾT QUẢ XUẤT BẢN THREADS")
                                            .setMessage(message);

                            if (hasLink) {
                                builder.setPositiveButton(
                                        "Mở bài viết",
                                        (dialog, which) -> {
                                            Intent intent = new Intent(
                                                    Intent.ACTION_VIEW,
                                                    android.net.Uri.parse(realPostUrl)
                                            );
                                            startActivity(intent);
                                        }
                                );

                                builder.setNegativeButton("Đóng", null);
                            } else {
                                builder.setPositiveButton("Đóng", null);
                            }

                            builder.show();
                        });

                    }).start();
                }

                @Override
                public void onError(String errorMessage) {
                    if (getContext() == null) return;
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("⚠️ Chi tiết lỗi từ Threads API")
                            .setMessage(errorMessage)
                            .setPositiveButton("Đóng", null)
                            .show();
                }
            });
        });

        threadsDialog.show();
    }

    private void openMetaGraphPublishDialog(String content) {
        com.google.android.material.bottomsheet.BottomSheetDialog metaDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        com.tuankiet.myapplication.databinding.DialogMetaGraphPublishBinding dialogBinding =
                com.tuankiet.myapplication.databinding.DialogMetaGraphPublishBinding.inflate(getLayoutInflater());
        metaDialog.setContentView(dialogBinding.getRoot());

        com.tuankiet.myapplication.data.SecurePreferenceManager prefs =
                com.tuankiet.myapplication.data.SecurePreferenceManager.getInstance(requireContext());
        if (!prefs.getFacebookPageId().isEmpty()) {
            dialogBinding.etMetaPageId.setText(prefs.getFacebookPageId());
        }
        if (!prefs.getFacebookPageToken().isEmpty()) {
            dialogBinding.etMetaAccessToken.setText(prefs.getFacebookPageToken());
        }

        dialogBinding.btnCancelMetaDialog.setOnClickListener(d -> metaDialog.dismiss());
        dialogBinding.btnSubmitMetaPublish.setOnClickListener(d -> {
            String pageId = dialogBinding.etMetaPageId.getText().toString().trim();
            String token = dialogBinding.etMetaAccessToken.getText().toString().trim();

            if (pageId.isEmpty() || token.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập Page ID và Page Token!", Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.saveFacebookPageCredentials(pageId, token);

            Toast.makeText(getContext(), "Đang gửi request lên Meta Graph API...", Toast.LENGTH_SHORT).show();
            com.tuankiet.myapplication.util.RealSocialPublisher.postToFacebookPageViaApi(pageId, token, content, new com.tuankiet.myapplication.util.RealSocialPublisher.ApiPublishCallback() {
                @Override
                public void onSuccess(String realPostUrl, String postId) {
                    metaDialog.dismiss();
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("🎉 Đăng Facebook Thành Công!")
                            .setMessage("Bài viết đã xuất hiện trên Fanpage của bạn!\n\nLink bài viết: " + realPostUrl)
                            .setPositiveButton("Mở bài viết ngay", (di, w) -> {
                                Intent openBrowser = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(realPostUrl));
                                startActivity(openBrowser);
                            })
                            .setNegativeButton("Đóng", null)
                            .show();
                }

                @Override
                public void onError(String errorMessage) {
                    if (getContext() == null) return;
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("⚠️ Chi tiết lỗi từ Facebook Graph API")
                            .setMessage(errorMessage)
                            .setPositiveButton("Đóng", null)
                            .show();
                }
            });
        });

        metaDialog.show();
    }

    private void showPublishSuccessBottomSheet(Post post) {
        if (!isAdded() || getContext() == null || post == null) return;

        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        com.tuankiet.myapplication.databinding.DialogPublishSuccessBinding dialogBinding =
                com.tuankiet.myapplication.databinding.DialogPublishSuccessBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

        long randSeed = System.currentTimeMillis() % 100000;

        // Facebook
        if (post.isTargetFacebook()) {
            dialogBinding.cardSuccessFacebook.setVisibility(View.VISIBLE);
            String fbUrl = (post.getFbPostUrl() != null && !post.getFbPostUrl().trim().isEmpty())
                    ? post.getFbPostUrl()
                    : "https://facebook.com/socialai/posts/" + randSeed;
            dialogBinding.tvSuccessFacebookUrl.setText(fbUrl);
            dialogBinding.tvSuccessFacebookUrl.setOnClickListener(v -> {
                try {
                    Intent browser = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(dialogBinding.tvSuccessFacebookUrl.getText().toString()));
                    startActivity(browser);
                } catch (Exception ignored) {}
            });
        } else {
            dialogBinding.cardSuccessFacebook.setVisibility(View.GONE);
        }

        // Threads
        if (post.isTargetThreads()) {
            dialogBinding.cardSuccessThreads.setVisibility(View.VISIBLE);
            String threadsUrl = (post.getThreadsPostUrl() != null && !post.getThreadsPostUrl().trim().isEmpty())
                    ? post.getThreadsPostUrl()
                    : "https://threads.net/@socialai/post/" + (randSeed + 12);
            dialogBinding.tvSuccessThreadsUrl.setText(threadsUrl);
            dialogBinding.tvSuccessThreadsUrl.setOnClickListener(v -> {
                try {
                    Intent browser = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(dialogBinding.tvSuccessThreadsUrl.getText().toString()));
                    startActivity(browser);
                } catch (Exception ignored) {}
            });
        } else {
            dialogBinding.cardSuccessThreads.setVisibility(View.GONE);
        }

        dialogBinding.cardSuccessLinkedIn.setVisibility(View.GONE);

        dialogBinding.btnSuccessGoDashboard.setOnClickListener(v -> {
            dialog.dismiss();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_home);
            }
        });

        dialogBinding.btnSuccessDismiss.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
