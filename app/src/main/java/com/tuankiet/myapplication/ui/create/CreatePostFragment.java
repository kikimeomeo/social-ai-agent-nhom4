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

        binding.btnSkipToReview.setOnClickListener(v -> {
            if (currentPost != null) {
                bindReviewData(currentPost);
                goToStep(3);
            }
        });
    }

    private void startPipeline(String topic, String tone, String language, boolean targetFb, boolean targetThreads, boolean targetLi) {
        goToStep(2);
        binding.tvLiveStreamOutput.setText("Bắt đầu khởi tạo AI Multi-Agent Pipeline...\nNgôn ngữ: " + language + "\n");

        orchestrator.generatePost(topic, tone, language, targetFb, targetThreads, targetLi, new AIAgentOrchestrator.AgentCallback() {
            @Override
            public void onStepChanged(int stepIndex, String stepTitle, String agentName) {
                if (binding == null) return;
                binding.tvCurrentAgent.setText(agentName);
                binding.tvPipelineStatus.setText(stepTitle);
            }

            @Override
            public void onStreamFacebookChunk(String fullCurrentText) {
                if (binding == null) return;
                binding.tvLiveStreamOutput.setText("[Facebook Agent Output]:\n\n" + fullCurrentText);
                binding.scrollStream.fullScroll(View.FOCUS_DOWN);
            }

            @Override
            public void onStreamThreadsChunk(String fullCurrentText) {
                if (binding == null) return;
                binding.tvLiveStreamOutput.setText("[Threads Agent Output]:\n\n" + fullCurrentText);
                binding.scrollStream.fullScroll(View.FOCUS_DOWN);
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
            Toast.makeText(getContext(), "Đang yêu cầu AI viết lại bản mới...", Toast.LENGTH_SHORT).show();
            goToStep(2);
            orchestrator.retryChannel(currentPost, currentReviewPlatform, new AIAgentOrchestrator.AgentCallback() {
                @Override
                public void onStepChanged(int stepIndex, String stepTitle, String agentName) {
                    if (binding == null) return;
                    binding.tvCurrentAgent.setText(agentName);
                    binding.tvPipelineStatus.setText(stepTitle);
                }

                @Override
                public void onStreamFacebookChunk(String fullCurrentText) {
                    if (binding == null) return;
                    binding.tvLiveStreamOutput.setText("[Facebook Retry Output]:\n\n" + fullCurrentText);
                }

                @Override
                public void onStreamThreadsChunk(String fullCurrentText) {
                    if (binding == null) return;
                    binding.tvLiveStreamOutput.setText("[Threads Retry Output]:\n\n" + fullCurrentText);
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

        // ĐĂNG THẬT LÊN CÁC APP TRÊN MÁY (Facebook, LinkedIn, Threads, Zalo,...)
        binding.btnShareToRealApp.setOnClickListener(v -> {
            if (currentPost == null) return;
            String content = binding.etReviewContent.getText().toString();
            if (content.trim().isEmpty()) {
                Toast.makeText(getContext(), "Nội dung bài viết đang trống!", Toast.LENGTH_SHORT).show();
                return;
            }

            CharSequence[] options = {
                    "🚀 TỰ ĐỘNG đăng lên Threads (Official Threads API - Không cần mở app)",
                    "🌐 TỰ ĐỘNG đăng lên Facebook Fanpage (Meta Graph API)",
                    "Mở App Threads (Dán bài thủ công)",
                    "Mở App Facebook (Dán bài thủ công)",
                    "Mở App LinkedIn (Dán bài thủ công)",
                    "Chia sẻ qua ứng dụng khác..."
            };
            new AlertDialog.Builder(requireContext())
                    .setTitle("Chọn phương thức đăng bài thật:")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            String threadsText = (currentReviewPlatform == 1)
                                    ? content
                                    : (!currentPost.getContentThreads().isEmpty() ? currentPost.getContentThreads() : content);
                            PostReviewPolicy.PublishGateResult gate = PostReviewPolicy.evaluateSingleChannelPublishGate(
                                    threadsText, SocialPlatform.THREADS, true
                            );
                            if (!gate.allowed) {
                                showPublishBlockedDialog(gate);
                                return;
                            }
                            openThreadsPublishDialog(threadsText);
                        } else if (which == 1) {
                            String fbText = (currentReviewPlatform == 0)
                                    ? content
                                    : (!currentPost.getContentFacebook().isEmpty() ? currentPost.getContentFacebook() : content);
                            PostReviewPolicy.PublishGateResult gate = PostReviewPolicy.evaluateSingleChannelPublishGate(
                                    fbText, SocialPlatform.FACEBOOK, true
                            );
                            if (!gate.allowed) {
                                showPublishBlockedDialog(gate);
                                return;
                            }
                            openMetaGraphPublishDialog(fbText);
                        } else if (which == 2) {
                            com.tuankiet.myapplication.util.RealSocialPublisher.shareToThreads(requireActivity(), content);
                        } else if (which == 3) {
                            com.tuankiet.myapplication.util.RealSocialPublisher.shareToRealApp(requireActivity(), SocialPlatform.FACEBOOK, content);
                        } else if (which == 4) {
                            com.tuankiet.myapplication.util.RealSocialPublisher.shareToRealApp(requireActivity(), SocialPlatform.LINKEDIN, content);
                        } else {
                            Intent shareIntent = new Intent(Intent.ACTION_SEND)
                                    .setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, content);
                            startActivity(Intent.createChooser(shareIntent, "Đăng bài SocialAI lên:"));
                        }
                    })
                    .show();
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

        boolean isOnline = com.tuankiet.myapplication.util.NetworkUtils.isNetworkAvailable(requireContext());
        if (isOnline) {
            currentPost.setStatus(Post.STATUS_PUBLISHED);
            currentPost.setPublishedFacebook(currentPost.isTargetFacebook());
            currentPost.setPublishedThreads(currentPost.isTargetThreads());
            currentPost.setPublishedLinkedIn(currentPost.isTargetLinkedIn());
            currentPost.setReachCount(180); // Initial reach metrics
            currentPost.setLikeCount(28);
            currentPost.setCommentCount(6);
            MockDataRepository.getInstance(requireContext()).updatePost(currentPost);

            // Lập lịch Worker chạy nền: 12 giây sau sẽ tự động nhận comment mới & Bot tự động phản hồi (F6)
            com.tuankiet.myapplication.worker.CommentSyncWorker.scheduleSimulatedSync(requireContext(), 12);

            StringBuilder sb = new StringBuilder();
            sb.append("Bài viết đã được BẠN DUYỆT và xuất bản thành công lên các kênh kết nối:\n\n");
            if (currentPost.isTargetFacebook()) {
                sb.append("• Facebook Page: ✅ ĐÃ ĐĂNG THÀNH CÔNG\n  URL: https://facebook.com/socialai/posts/").append(System.currentTimeMillis() % 100000).append("\n\n");
            }
            if (currentPost.isTargetThreads()) {
                sb.append("• Meta Threads: ✅ ĐÃ ĐĂNG THÀNH CÔNG\n  URL: https://threads.net/@aitech_vietnam/post/").append(System.currentTimeMillis() % 100000).append("\n\n");
            }
            if (currentPost.isTargetLinkedIn()) {
                sb.append("• LinkedIn Profile: ✅ ĐÃ ĐĂNG THÀNH CÔNG\n  URL: https://linkedin.com/feed/update/urn:li:activity:").append(System.currentTimeMillis() % 100000).append("\n\n");
            }
            sb.append("Bài viết đã được lưu vết và bắt đầu theo dõi tăng trưởng, tương tác & comment!");

            new AlertDialog.Builder(requireContext())
                    .setTitle("🎉 Đã Duyệt & Xuất Bản Thành Công")
                    .setMessage(sb.toString())
                    .setPositiveButton("Xem trên trang Tổng quan", (dialog, which) -> {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).navigateToTab(R.id.nav_home);
                        }
                    })
                    .setCancelable(false)
                    .show();
        } else {
            currentPost.setStatus(Post.STATUS_PENDING);
            MockDataRepository.getInstance(requireContext()).updatePost(currentPost);
            com.tuankiet.myapplication.worker.PostPublisherWorker.scheduleOfflinePublisher(requireContext());

            new AlertDialog.Builder(requireContext())
                    .setTitle("📡 Chế độ Ngoại Tuyến (Offline Queue)")
                    .setMessage("Thiết bị đang không có kết nối Internet.\n\nBài viết đã được bạn duyệt và đưa vào Hàng đợi ngoại tuyến (Offline Queue) bằng Android WorkManager.\n\nNgay khi có mạng trở lại, hệ thống sẽ tự động xuất bản lên Facebook Page & Threads!")
                    .setPositiveButton("Đã hiểu", (dialog, which) -> {
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

    private void updateReviewTabContent() {
        if (currentPost == null || binding == null) return;

        String contentToShow;
        if (currentReviewPlatform == 0) {
            binding.ivReviewPlatformIcon.setImageResource(R.drawable.ic_facebook);
            binding.tvReviewAuthorName.setText("AI Tech Vietnam (Facebook Page)");
            binding.tvReviewSubInfo.setText("Vừa xong • 🌐 Công khai");
            contentToShow = currentPost.getContentFacebook();
        } else if (currentReviewPlatform == 1) {
            binding.ivReviewPlatformIcon.setImageResource(R.drawable.ic_threads);
            binding.tvReviewAuthorName.setText("@aitech_vietnam (Threads)");
            binding.tvReviewSubInfo.setText("Vừa xong • 🧵 Cuộc trò chuyện mở");
            contentToShow = currentPost.getContentThreads();
        } else {
            binding.ivReviewPlatformIcon.setImageResource(R.drawable.ic_linkedin);
            binding.tvReviewAuthorName.setText("Nguyen Van A (Tech Lead)");
            binding.tvReviewSubInfo.setText("Vừa xong • 👔 Kết nối mạng lưới");
            contentToShow = currentPost.getContentLinkedIn();
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

        String savedToken = com.tuankiet.myapplication.data.SecurePreferenceManager.getInstance(requireContext()).getThreadsToken();
        if (savedToken != null && !savedToken.isEmpty()) {
            dialogBinding.etThreadsAccessToken.setText(savedToken);
        }

        dialogBinding.btnCancelThreadsDialog.setOnClickListener(d -> threadsDialog.dismiss());
        dialogBinding.btnSubmitThreadsPublish.setOnClickListener(d -> {
            String token = dialogBinding.etThreadsAccessToken.getText().toString().trim();
            String userId = dialogBinding.etThreadsUserId.getText().toString().trim();

            if (token.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng dán Threads User Access Token!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save token for next time
            com.tuankiet.myapplication.data.SecurePreferenceManager.getInstance(requireContext()).saveThreadsToken(token);

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

        dialogBinding.btnCancelMetaDialog.setOnClickListener(d -> metaDialog.dismiss());
        dialogBinding.btnSubmitMetaPublish.setOnClickListener(d -> {
            String pageId = dialogBinding.etMetaPageId.getText().toString().trim();
            String token = dialogBinding.etMetaAccessToken.getText().toString().trim();

            if (pageId.isEmpty() || token.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập Page ID và Page Token!", Toast.LENGTH_SHORT).show();
                return;
            }

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
