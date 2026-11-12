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
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;
import com.tuankiet.myapplication.MainActivity;
import com.tuankiet.myapplication.R;
import com.tuankiet.myapplication.agent.AIAgentOrchestrator;
import com.tuankiet.myapplication.databinding.FragmentCreatePostBinding;
import com.tuankiet.myapplication.model.Post;
import com.tuankiet.myapplication.repository.MockDataRepository;

public class CreatePostFragment extends Fragment {

    private FragmentCreatePostBinding binding;
    private AIAgentOrchestrator orchestrator;
    private Post currentPost;
    private boolean isViewingFacebookTab = true;

    private static final int FB_CHAR_LIMIT = 63206;
    private static final int LI_CHAR_LIMIT = 3000;

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

        // Stepper Visuals
        int activeColor = getResources().getColor(R.color.primary);
        int inactiveColor = getResources().getColor(R.color.stepper_inactive);
        int textActive = getResources().getColor(R.color.text_primary);
        int textInactive = getResources().getColor(R.color.text_muted);

        binding.tvStepBadge1.setBackgroundColor(step >= 1 ? activeColor : inactiveColor);
        binding.tvStepLabel1.setTextColor(step >= 1 ? textActive : textInactive);

        binding.lineStep1.setBackgroundColor(step >= 2 ? activeColor : inactiveColor);
        binding.tvStepBadge2.setBackgroundColor(step >= 2 ? activeColor : inactiveColor);
        binding.tvStepLabel2.setTextColor(step >= 2 ? textActive : textInactive);

        binding.lineStep2.setBackgroundColor(step >= 3 ? activeColor : inactiveColor);
        binding.tvStepBadge3.setBackgroundColor(step >= 3 ? activeColor : inactiveColor);
        binding.tvStepLabel3.setTextColor(step >= 3 ? textActive : textInactive);
    }

    // --- STEP 1: NHẬP LIỆU ---
    private void setupStep1() {
        binding.chipSuggest1.setOnClickListener(v -> binding.etTopic.setText("5 Xu hướng Trí tuệ Nhân tạo định hình 2026"));
        binding.chipSuggest2.setOnClickListener(v -> binding.etTopic.setText("Tại sao On-Device AI là tương lai của Android Developer?"));
        binding.chipSuggest3.setOnClickListener(v -> binding.etTopic.setText("Ứng dụng kiến trúc Multi-Agent trong tự động hóa Social Media"));

        binding.btnStartGenerate.setOnClickListener(v -> {
            String topic = binding.etTopic.getText() != null ? binding.etTopic.getText().toString().trim() : "";
            if (topic.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập chủ đề bài viết!", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean targetFb = binding.cbFacebook.isChecked();
            boolean targetLi = binding.cbLinkedIn.isChecked();
            if (!targetFb && !targetLi) {
                Toast.makeText(getContext(), "Vui lòng chọn ít nhất 1 kênh đăng!", Toast.LENGTH_SHORT).show();
                return;
            }

            String tone = "Chuyên gia";
            if (binding.chipToneHumorous.isChecked()) {
                tone = "Hài hước";
            } else if (binding.chipToneFriendly.isChecked()) {
                tone = "Thân thiện";
            }

            startPipeline(topic, tone, targetFb, targetLi);
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

    private void startPipeline(String topic, String tone, boolean targetFb, boolean targetLi) {
        goToStep(2);
        binding.tvLiveStreamOutput.setText("Bắt đầu khởi tạo AI Multi-Agent Pipeline...\n");

        orchestrator.generatePost(topic, tone, targetFb, targetLi, new AIAgentOrchestrator.AgentCallback() {
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
                isViewingFacebookTab = (tab.getPosition() == 0);
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
                int length = s != null ? s.length() : 0;
                int limit = isViewingFacebookTab ? FB_CHAR_LIMIT : LI_CHAR_LIMIT;
                String channel = isViewingFacebookTab ? "Facebook" : "LinkedIn";
                binding.tvCharCounter.setText(length + " / " + limit + " ký tự (" + channel + ")");

                // Check Content Safety Warning
                AIAgentOrchestrator.SafetyCheckResult safety = orchestrator.checkContentSafety(s.toString());
                if (safety.hasWarning) {
                    binding.cardContentWarning.setVisibility(View.VISIBLE);
                    binding.tvContentWarningText.setText(safety.warningMessage);
                } else {
                    binding.cardContentWarning.setVisibility(View.GONE);
                }

                // Keep edited text in currentPost
                if (currentPost != null) {
                    if (isViewingFacebookTab) {
                        currentPost.setContentFacebook(s.toString());
                    } else {
                        currentPost.setContentLinkedIn(s.toString());
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Quick AI Rewrite Assistant Listeners
        binding.btnRewriteShorter.setOnClickListener(v -> {
            String currentText = binding.etReviewContent.getText().toString();
            Toast.makeText(getContext(), "AI đang tóm tắt ngắn hơn...", Toast.LENGTH_SHORT).show();
            orchestrator.rewriteSection(currentText, "SHORTER", new AIAgentOrchestrator.RewriteCallback() {
                @Override
                public void onSuccess(String rewrittenText) {
                    binding.etReviewContent.setText(rewrittenText);
                    Toast.makeText(getContext(), "✨ Đã rút gọn nội dung!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        binding.btnRewriteFormal.setOnClickListener(v -> {
            String currentText = binding.etReviewContent.getText().toString();
            Toast.makeText(getContext(), "AI đang chuyển đổi sang giọng văn trang trọng...", Toast.LENGTH_SHORT).show();
            orchestrator.rewriteSection(currentText, "FORMAL", new AIAgentOrchestrator.RewriteCallback() {
                @Override
                public void onSuccess(String rewrittenText) {
                    binding.etReviewContent.setText(rewrittenText);
                    Toast.makeText(getContext(), "👔 Đã chuyển sang phong cách trang trọng!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        binding.btnRewriteHook.setOnClickListener(v -> {
            String currentText = binding.etReviewContent.getText().toString();
            Toast.makeText(getContext(), "AI đang tạo Hook mở đầu mới...", Toast.LENGTH_SHORT).show();
            orchestrator.rewriteSection(currentText, "HOOK", new AIAgentOrchestrator.RewriteCallback() {
                @Override
                public void onSuccess(String rewrittenText) {
                    binding.etReviewContent.setText(rewrittenText);
                    Toast.makeText(getContext(), "⚡ Đã đổi câu mở đầu hấp dẫn hơn!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        binding.btnRetryCurrentChannel.setOnClickListener(v -> {
            if (currentPost == null) return;
            Toast.makeText(getContext(), "Đang yêu cầu AI viết lại bản mới...", Toast.LENGTH_SHORT).show();
            goToStep(2);
            orchestrator.retryChannel(currentPost, isViewingFacebookTab, new AIAgentOrchestrator.AgentCallback() {
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
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Chọn phương thức đăng bài thật:")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            openThreadsPublishDialog(content);
                        } else if (which == 1) {
                            openMetaGraphPublishDialog(content);
                        } else if (which == 2) {
                            com.tuankiet.myapplication.util.RealSocialPublisher.shareToThreads(requireActivity(), content);
                        } else if (which == 3) {
                            com.tuankiet.myapplication.util.RealSocialPublisher.shareToRealApp(requireActivity(), com.tuankiet.myapplication.model.SocialPlatform.FACEBOOK, content);
                        } else if (which == 4) {
                            com.tuankiet.myapplication.util.RealSocialPublisher.shareToRealApp(requireActivity(), com.tuankiet.myapplication.model.SocialPlatform.LINKEDIN, content);
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
            openMetaGraphPublishDialog(content);
        });

        // Save Draft
        binding.btnSaveDraft.setOnClickListener(v -> {
            if (currentPost != null) {
                currentPost.setStatus(Post.STATUS_DRAFT);
                MockDataRepository.getInstance(requireContext()).addPost(currentPost);
                Toast.makeText(getContext(), "Đã lưu bản nháp offline thành công!", Toast.LENGTH_LONG).show();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToTab(R.id.nav_home);
                }
            }
        });

        // Publish Now (F5 Autonomous Multi-channel Publishing & Offline Queue via WorkManager)
        binding.btnPublishNow.setOnClickListener(v -> {
            if (currentPost != null) {
                boolean isOnline = com.tuankiet.myapplication.util.NetworkUtils.isNetworkAvailable(requireContext());
                if (isOnline) {
                    currentPost.setStatus(Post.STATUS_PUBLISHED);
                    currentPost.setReachCount(180); // Initial reach metrics
                    currentPost.setLikeCount(28);
                    currentPost.setCommentCount(6);
                    MockDataRepository.getInstance(requireContext()).addPost(currentPost);

                    // Lập lịch Worker chạy nền: 12 giây sau sẽ tự động nhận comment mới & Bot tự động phản hồi (F6)
                    com.tuankiet.myapplication.worker.CommentSyncWorker.scheduleSimulatedSync(requireContext(), 12);

                    StringBuilder sb = new StringBuilder();
                    sb.append("Agent đã tự động xuất bản đồng thời lên các kênh mạng xã hội kết nối:\n\n");
                    if (currentPost.isTargetFacebook()) {
                        sb.append("• Facebook Page: ✅ ĐÃ ĐĂNG THÀNH CÔNG\n  URL: https://facebook.com/socialai/posts/").append(System.currentTimeMillis() % 100000).append("\n\n");
                    }
                    if (currentPost.isTargetLinkedIn()) {
                        sb.append("• LinkedIn Profile: ✅ ĐÃ ĐĂNG THÀNH CÔNG\n  URL: https://linkedin.com/feed/update/urn:li:activity:").append(System.currentTimeMillis() % 100000).append("\n\n");
                    }
                    sb.append("Bài viết đã được lưu vết và bắt đầu theo dõi tăng trưởng, tương tác & comment!");

                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("🎉 Xuất Bản Tự Động Thành Công (F5)")
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
                    MockDataRepository.getInstance(requireContext()).addPost(currentPost);
                    com.tuankiet.myapplication.worker.PostPublisherWorker.scheduleOfflinePublisher(requireContext());

                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("📡 Chế độ Ngoại Tuyến (Offline Queue)")
                            .setMessage("Thiết bị đang không có kết nối Internet.\n\nTheo quy chuẩn F5, bài viết đã được đưa vào Hàng đợi ngoại tuyến (Offline Queue) bằng Android WorkManager.\n\nNgay khi có mạng trở lại, hệ thống sẽ tự động xuất bản ngầm lên Facebook & LinkedIn!")
                            .setPositiveButton("Đã hiểu", (dialog, which) -> {
                                if (getActivity() instanceof MainActivity) {
                                    ((MainActivity) getActivity()).navigateToTab(R.id.nav_home);
                                }
                            })
                            .setCancelable(false)
                            .show();
                }
            }
        });
    }

    private void bindReviewData(Post post) {
        if (post == null || binding == null) return;
        binding.tvImagePromptPreview.setText(post.getImagePrompt() != null ? post.getImagePrompt() : "Tech 3D vector illustration");

        isViewingFacebookTab = post.isTargetFacebook() || !post.isTargetLinkedIn();
        if (binding.tabPlatformReview.getTabAt(isViewingFacebookTab ? 0 : 1) != null) {
            binding.tabPlatformReview.getTabAt(isViewingFacebookTab ? 0 : 1).select();
        }
        updateReviewTabContent();
    }

    private void updateReviewTabContent() {
        if (currentPost == null || binding == null) return;

        if (isViewingFacebookTab) {
            binding.ivReviewPlatformIcon.setImageResource(R.drawable.ic_facebook);
            binding.tvReviewAuthorName.setText("AI Tech Vietnam (Page)");
            binding.tvReviewSubInfo.setText("Vừa xong • 🌐 Công khai");
            binding.etReviewContent.setText(currentPost.getContentFacebook());
        } else {
            binding.ivReviewPlatformIcon.setImageResource(R.drawable.ic_linkedin);
            binding.tvReviewAuthorName.setText("Nguyen Van A (Tech Lead)");
            binding.tvReviewSubInfo.setText("Vừa xong • 👔 Kết nối mạng lưới");
            binding.etReviewContent.setText(currentPost.getContentLinkedIn());
        }
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
                            .setTitle("⚠️ Chi tiết lỗi từ Threads API")
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
