package com.tuankiet.myapplication.ui.inbox;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.Data;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.tuankiet.myapplication.R;
import com.tuankiet.myapplication.agent.AIAgentOrchestrator;
import com.tuankiet.myapplication.agent.CommentSafetyPolicy;
import com.tuankiet.myapplication.data.SecurePreferenceManager;
import com.tuankiet.myapplication.databinding.DialogReplyCommentBinding;
import com.tuankiet.myapplication.databinding.FragmentInboxBinding;
import com.tuankiet.myapplication.model.CommentItem;
import com.tuankiet.myapplication.model.SocialPlatform;
import com.tuankiet.myapplication.repository.MockDataRepository;
import com.tuankiet.myapplication.util.RealSocialPublisher;
import com.tuankiet.myapplication.worker.CommentSyncWorker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InboxFragment extends Fragment {

    private FragmentInboxBinding binding;
    private CommentAdapter adapter;
    private MockDataRepository repository;
    private SecurePreferenceManager prefManager;
    private AIAgentOrchestrator orchestrator;

    private final List<CommentItem> cachedComments = new ArrayList<>();
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String selectedChannel = InboxFilterHelper.CHANNEL_ALL;
    private String selectedPost = InboxFilterHelper.POST_ALL;
    private String selectedStatus = InboxFilterHelper.STATUS_ALL;
    private long pendingOpenCommentId = -1L;
    private boolean isBotRunning = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentInboxBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = MockDataRepository.getInstance(requireContext());
        prefManager = SecurePreferenceManager.getInstance(requireContext());
        orchestrator = new AIAgentOrchestrator(requireContext());

        // Đảm bảo đồng bộ nền định kỳ bằng WorkManager luôn được đăng ký
        CommentSyncWorker.schedulePeriodicSync(requireContext());

        setupRecyclerView();
        setupFilters();
        setupSwipeRefresh();
        setupAutoReplyControls();
        loadComments();
        updateSyncStatusLabel();
        openPendingCommentIfNeeded();
    }

    public void openCommentByIdWhenReady(long commentId) {
        this.pendingOpenCommentId = commentId;
        if (binding != null && isAdded()) {
            loadComments();
            openPendingCommentIfNeeded();
        }
    }

    private void openPendingCommentIfNeeded() {
        if (pendingOpenCommentId <= 0 || binding == null || !isAdded()) {
            return;
        }
        CommentItem target = repository.getCommentById(pendingOpenCommentId);
        pendingOpenCommentId = -1L;
        if (target != null) {
            openReplyBottomSheet(target);
        }
    }

    private void setupRecyclerView() {
        adapter = new CommentAdapter();
        binding.rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvComments.setAdapter(adapter);
        adapter.setOnCommentClickListener(this::openReplyBottomSheet);
    }

    private void setupFilters() {
        // 1. Channel Filter
        binding.tabFilterAll.setOnClickListener(v -> selectChannelFilter(InboxFilterHelper.CHANNEL_ALL));
        binding.tabFilterFacebook.setOnClickListener(v -> selectChannelFilter(InboxFilterHelper.CHANNEL_FACEBOOK));
        binding.tabFilterThreads.setOnClickListener(v -> selectChannelFilter(InboxFilterHelper.CHANNEL_THREADS));

        // 2. Status Filter
        binding.tabStatusAll.setOnClickListener(v -> selectStatusFilter(InboxFilterHelper.STATUS_ALL));
        binding.tabStatusUnanswered.setOnClickListener(v -> selectStatusFilter(InboxFilterHelper.STATUS_UNANSWERED));
        binding.tabStatusAnswered.setOnClickListener(v -> selectStatusFilter(InboxFilterHelper.STATUS_ANSWERED));

        // 3. Post Filter Picker + Clear
        binding.layoutPostFilter.setOnClickListener(v -> showPostFilterDialog());
        binding.tvClearPostFilter.setOnClickListener(v -> {
            selectedPost = InboxFilterHelper.POST_ALL;
            applyAllFilters();
        });

        // Reset all filters
        View.OnClickListener resetAction = v -> {
            selectedChannel = InboxFilterHelper.CHANNEL_ALL;
            selectedPost = InboxFilterHelper.POST_ALL;
            selectedStatus = InboxFilterHelper.STATUS_ALL;
            applyAllFilters();
        };
        binding.tvResetFilters.setOnClickListener(resetAction);
        binding.btnEmptyResetFilters.setOnClickListener(resetAction);
    }

    private void selectChannelFilter(String channel) {
        selectedChannel = channel;
        // Nếu bài viết đang chọn không tồn tại trong kênh mới, tự reset về "Tất cả bài viết"
        if (!InboxFilterHelper.POST_ALL.equals(selectedPost)) {
            List<String> validPosts = InboxFilterHelper.extractDistinctPosts(cachedComments, selectedChannel);
            if (!validPosts.contains(selectedPost)) {
                selectedPost = InboxFilterHelper.POST_ALL;
            }
        }
        applyAllFilters();
    }

    private void selectStatusFilter(String status) {
        selectedStatus = status;
        applyAllFilters();
    }

    private void showPostFilterDialog() {
        List<String> distinctPosts = InboxFilterHelper.extractDistinctPosts(cachedComments, selectedChannel);
        List<String> options = new ArrayList<>();
        options.add("Tất cả bài viết");
        options.addAll(distinctPosts);

        int checkedIndex = 0;
        if (!InboxFilterHelper.POST_ALL.equals(selectedPost)) {
            for (int i = 1; i < options.size(); i++) {
                if (options.get(i).equalsIgnoreCase(selectedPost)) {
                    checkedIndex = i;
                    break;
                }
            }
        }

        CharSequence[] items = options.toArray(new CharSequence[0]);
        new AlertDialog.Builder(requireContext())
                .setTitle("Lọc bình luận theo bài viết")
                .setSingleChoiceItems(items, checkedIndex, (dialog, which) -> {
                    selectedPost = (which == 0) ? InboxFilterHelper.POST_ALL : options.get(which);
                    applyAllFilters();
                    dialog.dismiss();
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void applyAllFilters() {
        if (binding == null) return;

        adapter.applyFilters(selectedChannel, selectedPost, selectedStatus);

        // Update Channel Pill visuals
        int activeColor = getResources().getColor(R.color.primary);
        int textInactive = getResources().getColor(R.color.text_muted);
        applyFilterPill(binding.tabFilterAll, InboxFilterHelper.CHANNEL_ALL.equals(selectedChannel), activeColor, Color.WHITE, textInactive);
        applyFilterPill(binding.tabFilterFacebook, InboxFilterHelper.CHANNEL_FACEBOOK.equals(selectedChannel), activeColor, Color.WHITE, textInactive);
        applyFilterPill(binding.tabFilterThreads, InboxFilterHelper.CHANNEL_THREADS.equals(selectedChannel), activeColor, Color.WHITE, textInactive);

        // Update Status Pill visuals + live counts
        int[] statusCounts = InboxFilterHelper.countByStatus(cachedComments, selectedChannel, selectedPost);
        binding.tabStatusAll.setText("Tất cả (" + statusCounts[0] + ")");
        binding.tabStatusUnanswered.setText("Chưa trả lời (" + statusCounts[1] + ")");
        binding.tabStatusAnswered.setText("Đã trả lời (" + statusCounts[2] + ")");

        int cardBg = getResources().getColor(R.color.card_bg);
        int primaryDark = getResources().getColor(R.color.primary_dark);
        applyFilterPill(binding.tabStatusAll, InboxFilterHelper.STATUS_ALL.equals(selectedStatus), cardBg, primaryDark, textInactive);
        applyFilterPill(binding.tabStatusUnanswered, InboxFilterHelper.STATUS_UNANSWERED.equals(selectedStatus), cardBg, primaryDark, textInactive);
        applyFilterPill(binding.tabStatusAnswered, InboxFilterHelper.STATUS_ANSWERED.equals(selectedStatus), cardBg, primaryDark, textInactive);

        // Update Post Filter Pill label
        boolean isSpecificPost = !InboxFilterHelper.POST_ALL.equals(selectedPost);
        binding.tvSelectedPostFilter.setText(isSpecificPost ? selectedPost : "Tất cả bài viết");
        binding.tvClearPostFilter.setVisibility(isSpecificPost ? View.VISIBLE : View.GONE);

        // Summary + Reset link + Empty State
        int shown = adapter.getFilteredCount();
        int riskCount = adapter.getRiskAlertCountInDisplayed();
        String summaryText = "Hiển thị " + shown + " bình luận";
        if (riskCount > 0) {
            summaryText += " • " + riskCount + " cảnh báo an toàn";
        }
        binding.tvFilterSummary.setText(summaryText);

        boolean hasActiveFilter = !InboxFilterHelper.CHANNEL_ALL.equals(selectedChannel)
                || isSpecificPost
                || !InboxFilterHelper.STATUS_ALL.equals(selectedStatus);
        binding.tvResetFilters.setVisibility(hasActiveFilter ? View.VISIBLE : View.GONE);

        binding.rvComments.setVisibility(shown > 0 ? View.VISIBLE : View.GONE);
        binding.layoutEmptyComments.setVisibility(shown == 0 ? View.VISIBLE : View.GONE);
    }

    private void applyFilterPill(TextView tab, boolean active, int activeBgColor, int activeTextColor, int inactiveTextColor) {
        if (active) {
            tab.setBackgroundResource(R.drawable.bg_soft_pill);
            tab.setBackgroundTintList(ColorStateList.valueOf(activeBgColor));
            tab.setTextColor(activeTextColor);
        } else {
            tab.setBackground(null);
            tab.setBackgroundTintList(null);
            tab.setTextColor(inactiveTextColor);
        }
    }

    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeColors(
                getResources().getColor(R.color.primary),
                getResources().getColor(R.color.status_blue)
        );

        binding.swipeRefreshLayout.setOnRefreshListener(this::triggerManualCommentSync);
        binding.tvRefreshInbox.setOnClickListener(v -> {
            if (binding != null && !binding.swipeRefreshLayout.isRefreshing()) {
                binding.swipeRefreshLayout.setRefreshing(true);
                triggerManualCommentSync();
            }
        });
    }

    /**
     * Kéo để làm mới (Pull-to-refresh) kích hoạt trực tiếp CommentSyncWorker (WorkManager)
     * và lắng nghe trạng thái hoàn tất để cập nhật Inbox + thời gian đồng bộ.
     */
    private void triggerManualCommentSync() {
        if (!isAdded() || binding == null) return;

        UUID workId = CommentSyncWorker.enqueueImmediateSync(requireContext());
        final boolean[] handled = {false};
        WorkManager.getInstance(requireContext())
                .getWorkInfoByIdLiveData(workId)
                .observe(getViewLifecycleOwner(), workInfo -> {
                    if (handled[0] || workInfo == null || binding == null) return;
                    WorkInfo.State state = workInfo.getState();
                    if (state.isFinished()) {
                        handled[0] = true;
                        binding.swipeRefreshLayout.setRefreshing(false);
                        loadComments();
                        updateSyncStatusLabel();

                        Data output = workInfo.getOutputData();
                        String msg = output.getString(CommentSyncWorker.KEY_STATUS_MESSAGE);
                        if (msg == null || msg.trim().isEmpty()) {
                            msg = (state == WorkInfo.State.SUCCEEDED)
                                    ? "Đã đồng bộ bình luận thành công!"
                                    : "Đồng bộ bình luận chưa hoàn tất, vui lòng thử lại.";
                        }
                        if (getContext() != null) {
                            Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void updateSyncStatusLabel() {
        if (binding == null) return;
        long lastSync = prefManager.getLastCommentSyncTime();
        if (lastSync > 0L) {
            String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(lastSync));
            binding.tvSyncStatus.setText("Cập nhật lúc " + timeStr + " • Đồng bộ nền 15p (WorkManager)");
        } else {
            binding.tvSyncStatus.setText("Đồng bộ nền định kỳ 15p • WorkManager");
        }
    }

    private void setupAutoReplyControls() {
        boolean enabled = prefManager.isControlledAutoReplyEnabled();
        binding.switchAutoReplyMode.setChecked(enabled);
        updateAutoReplyModeVisuals(enabled);

        binding.switchAutoReplyMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefManager.setControlledAutoReplyEnabled(isChecked);
            updateAutoReplyModeVisuals(isChecked);
            if (getContext() != null) {
                Toast.makeText(
                        getContext(),
                        isChecked
                                ? "Đã BẬT chế độ tự động trả lời có kiểm soát (chỉ Khen ngợi & FAQ)"
                                : "Đã TẮT tự động trả lời — Mọi bình luận sẽ chờ duyệt thủ công",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        binding.btnRunControlledAutoReply.setOnClickListener(v -> runControlledAutoReplyBatch());
    }

    private void updateAutoReplyModeVisuals(boolean enabled) {
        if (binding == null) return;
        binding.tvAutoReplyModeHint.setText(enabled
                ? "BẬT • Chỉ tự trả lời Khen ngợi & FAQ • Khóa Tiêu cực, TT cá nhân, chưa chắc"
                : "TẮT • Chỉ phân loại AI & soạn gợi ý, không tự gửi trả lời");
        binding.btnRunControlledAutoReply.setEnabled(enabled && !isBotRunning);
        binding.btnRunControlledAutoReply.setAlpha(enabled && !isBotRunning ? 1.0f : 0.55f);
    }

    /**
     * Thực thi chế độ Tự động trả lời có kiểm soát (F6):
     * - CHỈ tự động trả lời nhóm Khen ngợi (PRAISE) và Câu hỏi thường gặp (FAQ QUESTION)
     *   khi độ tin cậy AI >= 75% và KHÔNG chứa thông tin cá nhân / rủi ro.
     * - TUYỆT ĐỐI KHÔNG tự trả lời: Tiêu cực, Spam, Góp ý, Câu hỏi chuyên sâu (non-FAQ),
     *   bình luận chứa thông tin cá nhân (PII) hoặc AI chưa chắc chắn (< 75%).
     */
    private void runControlledAutoReplyBatch() {
        if (isBotRunning || binding == null || !isAdded()) return;
        if (!prefManager.isControlledAutoReplyEnabled()) {
            Toast.makeText(getContext(), "Hãy bật công tắc Tự động trả lời có kiểm soát trước khi chạy Bot.", Toast.LENGTH_SHORT).show();
            return;
        }

        isBotRunning = true;
        binding.btnRunControlledAutoReply.setEnabled(false);
        binding.btnRunControlledAutoReply.setText("Đang kiểm tra an toàn & phản hồi...");

        final String threadsToken = prefManager.getThreadsToken();
        final String fbPageToken = prefManager.getFacebookPageToken();

        if (backgroundExecutor.isShutdown()) {
            isBotRunning = false;
            binding.btnRunControlledAutoReply.setText("Chạy Bot trả lời có kiểm soát");
            updateAutoReplyModeVisuals(prefManager.isControlledAutoReplyEnabled());
            return;
        }
        backgroundExecutor.execute(() -> {
            List<CommentItem> comments = repository.getComments();
            int autoRepliedPraise = 0;
            int autoRepliedFaq = 0;
            int blockedNegative = 0;
            int blockedSpam = 0;
            int blockedPii = 0;
            int blockedLowConfidence = 0;
            int blockedSuggestionOrComplex = 0;
            int apiFailedCount = 0;
            int liveApiSentCount = 0;

            for (CommentItem c : comments) {
                if (!InboxFilterHelper.matchesChannel(c, InboxFilterHelper.CHANNEL_ALL) || c.isAnswered()) {
                    continue;
                }

                CommentSafetyPolicy.AutoReplyDecision decision = CommentSafetyPolicy.evaluateAutoReply(c);
                if (!decision.allowed) {
                    if (CommentSafetyPolicy.CODE_BLOCKED_NEGATIVE.equals(decision.reasonCode)) {
                        blockedNegative++;
                    } else if (CommentSafetyPolicy.CODE_BLOCKED_SPAM.equals(decision.reasonCode)) {
                        blockedSpam++;
                    } else if (CommentSafetyPolicy.CODE_BLOCKED_PII.equals(decision.reasonCode)) {
                        blockedPii++;
                    } else if (CommentSafetyPolicy.CODE_BLOCKED_LOW_CONFIDENCE.equals(decision.reasonCode)) {
                        blockedLowConfidence++;
                    } else {
                        blockedSuggestionOrComplex++;
                    }
                    continue;
                }

                String reply = c.getAiSuggestedReply();
                if (reply == null || reply.trim().isEmpty()) {
                    CommentSafetyPolicy.CommentAnalysis analysis = orchestrator.analyzeCommentSync(
                            c.getPlatform(), c.getPostTitle(), c.getPostContent(), c.getAuthorName(), c.getContent()
                    );
                    CommentSafetyPolicy.applyAnalysisToComment(c, analysis);
                    repository.updateCommentAnalysis(c);

                    // Re-evaluate after fresh AI analysis (strict safety re-check)
                    decision = CommentSafetyPolicy.evaluateAutoReply(c);
                    if (!decision.allowed) {
                        blockedLowConfidence++;
                        continue;
                    }
                    reply = c.getAiSuggestedReply();
                }

                if (reply == null || reply.trim().isEmpty()) {
                    blockedLowConfidence++;
                    continue;
                }

                String remoteId = c.getEffectiveExternalCommentId();
                boolean sent = true;
                if (remoteId != null) {
                    if (c.getPlatform() == SocialPlatform.THREADS && threadsToken != null && !threadsToken.isEmpty()) {
                        String pubId = RealSocialPublisher.replyToThreadsCommentSync(threadsToken, remoteId, reply);
                        sent = (pubId != null);
                        if (sent) liveApiSentCount++;
                    } else if (c.getPlatform() == SocialPlatform.FACEBOOK && fbPageToken != null && !fbPageToken.isEmpty()) {
                        String pubId = RealSocialPublisher.replyToFacebookCommentSync(fbPageToken, remoteId, reply);
                        sent = (pubId != null);
                        if (sent) liveApiSentCount++;
                    }
                }

                if (sent) {
                    repository.replyToComment(c.getId(), reply, true);
                    if (CommentSafetyPolicy.CODE_ALLOWED_FAQ.equals(decision.reasonCode)) {
                        autoRepliedFaq++;
                    } else {
                        autoRepliedPraise++;
                    }
                } else {
                    apiFailedCount++;
                }
            }

            final int totalAuto = autoRepliedPraise + autoRepliedFaq;
            final int fPraise = autoRepliedPraise;
            final int fFaq = autoRepliedFaq;
            final int fNeg = blockedNegative;
            final int fPii = blockedPii;
            final int fLowConf = blockedLowConfidence;
            final int fSpam = blockedSpam;
            final int fManual = blockedSuggestionOrComplex;
            final int fApiFail = apiFailedCount;
            final int fLive = liveApiSentCount;

            mainHandler.post(() -> {
                isBotRunning = false;
                if (binding == null || !isAdded()) return;

                binding.btnRunControlledAutoReply.setText("Chạy Bot trả lời có kiểm soát");
                updateAutoReplyModeVisuals(prefManager.isControlledAutoReplyEnabled());
                loadComments();

                StringBuilder report = new StringBuilder();
                report.append("Tuân thủ nghiêm ngặt quy tắc Tự động trả lời có kiểm soát (F6):\n\n");
                report.append("✅ Đã tự động trả lời: ").append(totalAuto).append(" bình luận an toàn");
                if (totalAuto > 0) {
                    report.append(" (").append(fPraise).append(" Khen ngợi, ").append(fFaq).append(" FAQ)");
                }
                if (fLive > 0) {
                    report.append("\n   ↳ Đã gửi qua API thật: ").append(fLive);
                }
                report.append("\n\n🛡️ Đã CHẶN tự động để duyệt thủ công:");
                report.append("\n• Tiêu cực: ").append(fNeg);
                report.append("\n• Chứa thông tin cá nhân (PII): ").append(fPii);
                report.append("\n• AI chưa chắc chắn (<75%): ").append(fLowConf);
                report.append("\n• Spam: ").append(fSpam);
                report.append("\n• Góp ý / Câu hỏi chuyên sâu: ").append(fManual);
                if (fApiFail > 0) {
                    report.append("\n\n⚠️ Gửi qua API thất bại (giữ nguyên chưa trả lời): ").append(fApiFail);
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("🤖 Kết Quả Bot Phản Hồi Có Kiểm Soát")
                        .setMessage(report.toString())
                        .setPositiveButton("Xác nhận", null)
                        .show();
            });
        });
    }

    private void loadComments() {
        cachedComments.clear();
        cachedComments.addAll(repository.getComments());
        adapter.setComments(cachedComments);
        applyAllFilters();
    }

    private void openReplyBottomSheet(CommentItem item) {
        if (!isAdded()) return;
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        DialogReplyCommentBinding dialogBinding = DialogReplyCommentBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

        if (item.getPlatform() == SocialPlatform.THREADS) {
            dialogBinding.ivDialogPlatform.setImageResource(R.drawable.ic_threads);
        } else {
            dialogBinding.ivDialogPlatform.setImageResource(R.drawable.ic_facebook);
        }

        dialogBinding.tvDialogAuthorName.setText(item.getAuthorName() + " • " + (item.getTimeAgo() != null ? item.getTimeAgo() : ""));
        dialogBinding.tvDialogPostTitle.setText("Về bài: " + (item.getPostTitle() != null ? item.getPostTitle() : "Bài viết"));
        dialogBinding.tvDialogCommentText.setText(item.getContent());

        final boolean[] manualDraftRevealed = {false};
        bindDialogAnalysisState(dialogBinding, item, manualDraftRevealed[0]);

        if (item.isAnswered() && item.getReplyText() != null) {
            dialogBinding.etReplyInput.setText(item.getReplyText());
        } else if (!CommentSafetyPolicy.shouldHideAutoSuggestionByDefault(item)
                && item.getAiSuggestedReply() != null
                && !item.getAiSuggestedReply().trim().isEmpty()) {
            // Điền sẵn gợi ý vào ô soạn thảo để người dùng có thể chỉnh sửa trực tiếp trước khi gửi (chỉ với comment an toàn/góp ý/câu hỏi)
            if (!CommentSafetyPolicy.shouldShowSafetyAlert(item)) {
                dialogBinding.etReplyInput.setText(item.getAiSuggestedReply());
                dialogBinding.etReplyInput.setSelection(dialogBinding.etReplyInput.getText().length());
            }
        }

        // Hiển thị rõ kênh gửi (API thật hay Demo cục bộ)
        String remoteId = item.getEffectiveExternalCommentId();
        String threadsToken = prefManager.getThreadsToken();
        String fbPageToken = prefManager.getFacebookPageToken();
        boolean canSendThreadsApi = item.getPlatform() == SocialPlatform.THREADS
                && remoteId != null && threadsToken != null && !threadsToken.isEmpty();
        boolean canSendFbApi = item.getPlatform() == SocialPlatform.FACEBOOK
                && remoteId != null && fbPageToken != null && !fbPageToken.isEmpty();

        if (canSendThreadsApi) {
            dialogBinding.tvDeliveryChannelHint.setText("🌐 Sẽ gửi phản hồi trực tiếp lên Meta Threads API (reply_to_id=" + remoteId + ")");
        } else if (canSendFbApi) {
            dialogBinding.tvDeliveryChannelHint.setText("🌐 Sẽ gửi phản hồi trực tiếp lên Facebook Page Graph API (comment_id=" + remoteId + ")");
        } else {
            String platformName = item.getPlatform() != null ? item.getPlatform().getShortName() : "MXH";
            dialogBinding.tvDeliveryChannelHint.setText("💾 Chế độ Demo cục bộ (" + platformName + ") — cấu hình Token & đồng bộ bài thật để gửi qua API thật");
        }

        // Nút "Tạo lại" gợi ý theo ngữ cảnh bài viết bằng AI
        dialogBinding.btnRegenerateSuggestion.setOnClickListener(v -> {
            dialogBinding.btnRegenerateSuggestion.setEnabled(false);
            dialogBinding.tvAiSuggestedText.setText("⏳ AI đang phân tích ngữ cảnh bài viết & soạn gợi ý mới...");
            if (item.getPostContent() == null || item.getPostContent().trim().isEmpty()) {
                item.setPostContent(repository.findPostContentByTitle(item.getPostTitle()));
            }
            orchestrator.analyzeCommentAsync(item, analysis -> {
                if (!dialog.isShowing() || !isAdded()) return;
                dialogBinding.btnRegenerateSuggestion.setEnabled(true);
                CommentSafetyPolicy.applyAnalysisToComment(item, analysis);
                repository.updateCommentAnalysis(item);
                bindDialogAnalysisState(dialogBinding, item, manualDraftRevealed[0]);
                loadComments();
                Toast.makeText(getContext(), "✨ AI đã cập nhật phân loại & gợi ý theo ngữ cảnh bài viết!", Toast.LENGTH_SHORT).show();
            });
        });

        // Cho phép tạo bản nháp hòa giải (chỉ khi người dùng bấm thủ công cho bình luận Tiêu cực)
        dialogBinding.btnRequestManualDraft.setOnClickListener(v -> {
            manualDraftRevealed[0] = true;
            String draft = item.getAiSuggestedReply();
            if (draft == null || draft.trim().isEmpty()) {
                CommentSafetyPolicy.CommentAnalysis fallback = CommentSafetyPolicy.analyzeCommentLocally(
                        item.getPlatform(), item.getPostTitle(), item.getPostContent(), item.getAuthorName(), item.getContent()
                );
                draft = fallback.suggestedReply;
                item.setAiSuggestedReply(draft);
            }
            if (draft != null && !draft.trim().isEmpty()) {
                bindDialogAnalysisState(dialogBinding, item, true);
            }
        });

        dialogBinding.btnDismissDialog.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.btnSendReply.setOnClickListener(v -> {
            String reply = dialogBinding.etReplyInput.getText() != null
                    ? dialogBinding.etReplyInput.getText().toString().trim()
                    : "";
            if (reply.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập nội dung câu trả lời!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (canSendThreadsApi) {
                dialogBinding.btnSendReply.setEnabled(false);
                dialogBinding.btnSendReply.setText("Đang gửi Threads...");
                RealSocialPublisher.replyToThreadsCommentViaApi(threadsToken, remoteId, reply, new RealSocialPublisher.ApiPublishCallback() {
                    @Override
                    public void onSuccess(String realPostUrl, String postId) {
                        repository.replyToComment(item.getId(), reply, false);
                        if (isAdded()) {
                            loadComments();
                            dialog.dismiss();
                            Toast.makeText(getContext(), "🎉 Đã gửi trả lời thật lên Threads tới " + item.getAuthorName() + "!", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (isAdded() && dialog.isShowing()) {
                            dialogBinding.btnSendReply.setEnabled(true);
                            dialogBinding.btnSendReply.setText("Gửi trả lời");
                            Toast.makeText(getContext(), "⚠️ Lỗi gửi reply lên Threads: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            } else if (canSendFbApi) {
                dialogBinding.btnSendReply.setEnabled(false);
                dialogBinding.btnSendReply.setText("Đang gửi Facebook...");
                RealSocialPublisher.replyToFacebookCommentViaApi(fbPageToken, remoteId, reply, new RealSocialPublisher.ApiPublishCallback() {
                    @Override
                    public void onSuccess(String realPostUrl, String postId) {
                        repository.replyToComment(item.getId(), reply, false);
                        if (isAdded()) {
                            loadComments();
                            dialog.dismiss();
                            Toast.makeText(getContext(), "🎉 Đã gửi trả lời thật lên Facebook Page tới " + item.getAuthorName() + "!", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (isAdded() && dialog.isShowing()) {
                            dialogBinding.btnSendReply.setEnabled(true);
                            dialogBinding.btnSendReply.setText("Gửi trả lời");
                            Toast.makeText(getContext(), "⚠️ Lỗi gửi reply lên Facebook: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            } else {
                repository.replyToComment(item.getId(), reply, false);
                loadComments();
                dialog.dismiss();
                Toast.makeText(getContext(), "🎉 Đã lưu phản hồi gửi đến " + item.getAuthorName() + "!", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void bindDialogAnalysisState(DialogReplyCommentBinding dialogBinding, CommentItem item, boolean manualDraftRevealed) {
        CommentAdapter.bindCategoryBadge(dialogBinding.tvDialogCategoryBadge, item);

        CommentSafetyPolicy.AutoReplyDecision decision = CommentSafetyPolicy.evaluateAutoReply(item);
        int confPct = (int) Math.round(item.getAiConfidence() * 100.0);
        String meta = "Độ tin cậy AI: " + confPct + "% • "
                + (item.isAnswered()
                ? (item.isAutoReplied() ? "Bot đã tự động trả lời" : "Đã trả lời thủ công")
                : (decision.allowed ? "Đủ điều kiện tự động trả lời" : "Yêu cầu duyệt thủ công"));
        dialogBinding.tvDialogAiMeta.setText(meta);

        boolean showAlert = CommentSafetyPolicy.shouldShowSafetyAlert(item);
        boolean isNegative = CommentItem.CATEGORY_NEGATIVE.equalsIgnoreCase(item.getCategory());
        boolean hideSuggestion = CommentSafetyPolicy.shouldHideAutoSuggestionByDefault(item)
                && !(isNegative && manualDraftRevealed);

        if (showAlert) {
            dialogBinding.cardSafetyAlert.setVisibility(View.VISIBLE);
            String detail = item.getRiskWarning();
            if (detail == null || detail.trim().isEmpty()) {
                detail = decision.reasonMessage;
            }
            dialogBinding.tvSafetyAlertDetail.setText(detail);
            dialogBinding.btnRequestManualDraft.setVisibility(isNegative && !manualDraftRevealed ? View.VISIBLE : View.GONE);
        } else {
            dialogBinding.cardSafetyAlert.setVisibility(View.GONE);
        }

        if (hideSuggestion) {
            dialogBinding.cardAiSuggestedReply.setVisibility(View.GONE);
            dialogBinding.etReplyInput.setHint(
                    CommentItem.CATEGORY_SPAM.equalsIgnoreCase(item.getCategory())
                            ? "Khuyến nghị bỏ qua spam hoặc nhập phản hồi thủ công..."
                            : "Nhập câu trả lời thủ công sau khi cân nhắc kỹ..."
            );
        } else {
            dialogBinding.cardAiSuggestedReply.setVisibility(View.VISIBLE);
            dialogBinding.tvAiSuggestedLabel.setText(
                    isNegative
                            ? "Bản nháp hòa giải tham khảo (chỉ duyệt tay):"
                            : (showAlert
                            ? "Gợi ý thận trọng từ AI (chỉ duyệt tay):"
                            : "Gợi ý AI theo ngữ cảnh bài đăng:")
            );
            String suggested = item.getAiSuggestedReply();
            if (suggested == null || suggested.trim().isEmpty()) {
                suggested = "Cảm ơn bạn đã quan tâm và để lại bình luận cho bài viết!";
            }
            dialogBinding.tvAiSuggestedText.setText(suggested);
            final String finalSuggested = suggested;
            dialogBinding.btnApplySuggestion.setOnClickListener(v -> {
                dialogBinding.etReplyInput.setText(finalSuggested);
                dialogBinding.etReplyInput.setSelection(finalSuggested.length());
            });
            dialogBinding.etReplyInput.setHint("Chỉnh sửa gợi ý hoặc nhập phản hồi của bạn...");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadComments();
        updateSyncStatusLabel();
        openPendingCommentIfNeeded();
        RealSocialPublisher.setOnCommentsUpdatedListener(() -> {
            if (binding != null && isAdded()) {
                loadComments();
                updateSyncStatusLabel();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        RealSocialPublisher.setOnCommentsUpdatedListener(null);
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        backgroundExecutor.shutdownNow();
    }
}
