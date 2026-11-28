package com.tuankiet.myapplication.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.tuankiet.myapplication.agent.AIAgentOrchestrator;
import com.tuankiet.myapplication.agent.CommentSafetyPolicy;
import com.tuankiet.myapplication.data.DatabaseHelper;
import com.tuankiet.myapplication.data.SecurePreferenceManager;
import com.tuankiet.myapplication.model.CommentItem;
import com.tuankiet.myapplication.model.Post;
import com.tuankiet.myapplication.model.SocialPlatform;
import com.tuankiet.myapplication.util.NetworkUtils;
import com.tuankiet.myapplication.util.NotificationHelper;
import com.tuankiet.myapplication.util.RealSocialPublisher;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WorkManager Worker đồng bộ bình luận cho Hộp thư tương tác (Inbox - F6):
 * - Hỗ trợ đồng bộ ngầm định kỳ mỗi 15 phút (PeriodicWorkRequest).
 * - Hỗ trợ kích hoạt đồng bộ tức thì khi người dùng kéo để làm mới (Pull-to-refresh).
 * - Đồng bộ thật từ Facebook Page Graph API & Meta Threads API khi có Token,
 *   hoặc mô phỏng thực tế (có qua đầy đủ pipeline AI phân loại 5 nhóm & kiểm soát an toàn) trong chế độ Demo.
 */
public class CommentSyncWorker extends Worker {

    public static final String WORK_NAME = "social_ai_comment_sync_worker";
    public static final String PERIODIC_WORK_NAME = "social_ai_periodic_comment_sync";
    public static final String IMMEDIATE_WORK_NAME = "social_ai_immediate_comment_sync";

    public static final String KEY_TRIGGER_SOURCE = "trigger_source";
    public static final String TRIGGER_PERIODIC = "PERIODIC";
    public static final String TRIGGER_MANUAL = "MANUAL";
    public static final String TRIGGER_POST_PUBLISH = "POST_PUBLISH";

    public static final String KEY_NEW_COUNT = "new_count";
    public static final String KEY_AUTO_REPLIED_COUNT = "auto_replied_count";
    public static final String KEY_BLOCKED_COUNT = "blocked_count";
    public static final String KEY_STATUS_MESSAGE = "status_message";

    private static final AtomicInteger demoScenarioCounter = new AtomicInteger(0);

    public CommentSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SecurePreferenceManager prefs = SecurePreferenceManager.getInstance(context);
        String triggerSource = getInputData().getString(KEY_TRIGGER_SOURCE);
        boolean isManual = TRIGGER_MANUAL.equals(triggerSource);
        boolean autoReplyEnabled = prefs.isControlledAutoReplyEnabled();

        String threadsToken = prefs.getThreadsToken();
        String fbPageId = prefs.getFacebookPageId();
        String fbPageToken = prefs.getFacebookPageToken();

        boolean hasThreads = threadsToken != null && !threadsToken.trim().isEmpty();
        boolean hasFacebook = fbPageId != null && !fbPageId.trim().isEmpty()
                && fbPageToken != null && !fbPageToken.trim().isEmpty();

        // 1. Có cấu hình Token thật (Threads / Facebook Page) -> Đồng bộ API thật đồng bộ trong Worker
        if (hasThreads || hasFacebook) {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                if (isManual) {
                    return Result.success(buildOutputData(
                            0, 0, 0,
                            "Đang ngoại tuyến — hiển thị bình luận đã lưu cục bộ."
                    ));
                }
                return Result.retry();
            }

            RealSocialPublisher.SyncResult syncResult =
                    RealSocialPublisher.syncAllRealCommentsSync(context, autoReplyEnabled);

            return Result.success(buildOutputData(
                    syncResult.newCommentsCount,
                    syncResult.autoRepliedCount,
                    syncResult.blockedCount,
                    syncResult.statusMessage
            ));
        }

        // 2. Chế độ Demo (chưa cấu hình Token thật) -> Sinh tương tác Facebook & Threads và chạy qua AI Classifier + Policy thật
        return runDemoSyncWithAiPipeline(context, prefs, autoReplyEnabled);
    }

    private Result runDemoSyncWithAiPipeline(Context context,
                                             SecurePreferenceManager prefs,
                                             boolean autoReplyEnabled) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        List<Post> posts = dbHelper.getAllPosts();

        String postTitle = "5 Xu Hướng Trí Tuệ Nhân Tạo Năm 2026";
        String postContent = "";
        if (!posts.isEmpty()) {
            Post picked = posts.get(Math.abs(demoScenarioCounter.get()) % posts.size());
            if (picked.getTopic() != null && !picked.getTopic().trim().isEmpty()) {
                postTitle = picked.getTopic();
            }
            if (picked.getContentFacebook() != null && !picked.getContentFacebook().trim().isEmpty()) {
                postContent = picked.getContentFacebook();
            }
        }

        // Xoay vòng đa dạng kịch bản Facebook & Threads phủ đủ 5 nhóm + PII + AI chưa chắc chắn
        int idx = Math.abs(demoScenarioCounter.getAndIncrement()) % 6;
        SocialPlatform platform;
        String author;
        String content;

        switch (idx) {
            case 0: // PRAISE (Facebook) -> Đủ điều kiện tự động trả lời khi bật chế độ
                platform = SocialPlatform.FACEBOOK;
                author = "Phan Thanh Tùng";
                content = "Bài viết rất thiết thực và hữu ích cho team Marketing, cảm ơn kênh nhiều nhé!";
                break;
            case 1: // QUESTION FAQ (Threads) -> Đủ điều kiện tự động trả lời khi bật chế độ
                platform = SocialPlatform.THREADS;
                author = "@hoanglan.dev";
                content = "App có hỗ trợ dùng thử miễn phí và nhập Gemini API Key cá nhân không bạn?";
                break;
            case 2: // SUGGESTION (Facebook) -> KHÔNG tự động trả lời (chỉ gợi ý cho người duyệt)
                platform = SocialPlatform.FACEBOOK;
                author = "Vũ Minh Đức";
                content = "Nội dung khá ổn, góp ý thêm là bạn nên bổ sung ví dụ thực tế khi chạy nền với WorkManager nhé!";
                break;
            case 3: // NEGATIVE (Threads) -> TUYỆT ĐỐI CHẶN tự động trả lời
                platform = SocialPlatform.THREADS;
                author = "@hoanghai.critique";
                content = "Bài viết này nhìn giống bot tự sinh quá, rất tệ và thiếu cảm xúc thực tế!";
                break;
            case 4: // PII (Facebook) -> TUYỆT ĐỐI CHẶN tự động trả lời vì chứa SĐT/Email
                platform = SocialPlatform.FACEBOOK;
                author = "Ngọc Mai";
                content = "Mình cần tư vấn gói triển khai riêng, liên hệ giúp mình qua số 0987654321 hoặc mai.le@company.vn nhé!";
                break;
            default: // SPAM (Threads) -> TUYỆT ĐỐI CHẶN tự động trả lời
                platform = SocialPlatform.THREADS;
                author = "@promo_fast99";
                content = "Bấm vào link bit.ly/spam-promo nhận ngay khóa học làm giàu cấp tốc miễn phí 100%!";
                break;
        }

        AIAgentOrchestrator orchestrator = new AIAgentOrchestrator(context);
        CommentSafetyPolicy.CommentAnalysis analysis = orchestrator.analyzeCommentSync(
                platform, postTitle, postContent, author, content
        );

        CommentItem incoming = new CommentItem();
        incoming.setPlatform(platform);
        incoming.setPostTitle(postTitle);
        incoming.setPostContent(postContent);
        incoming.setAuthorName(author);
        String cleanAuthor = author.replace("@", "").trim();
        incoming.setAuthorInitial(cleanAuthor.isEmpty() ? "?" : cleanAuthor.substring(0, 1).toUpperCase(Locale.ROOT));
        incoming.setContent(content);
        incoming.setTimeAgo("Vừa xong");
        CommentSafetyPolicy.applyAnalysisToComment(incoming, analysis);

        CommentSafetyPolicy.AutoReplyDecision decision = CommentSafetyPolicy.evaluateAutoReply(incoming);
        int autoRepliedCount = 0;
        int blockedCount = 0;

        if (autoReplyEnabled && decision.allowed
                && incoming.getAiSuggestedReply() != null
                && !incoming.getAiSuggestedReply().trim().isEmpty()) {
            incoming.setAnswered(true);
            incoming.setAutoReplied(true);
            incoming.setReplyText(incoming.getAiSuggestedReply());
            autoRepliedCount = 1;
        } else {
            incoming.setAnswered(false);
            incoming.setAutoReplied(false);
            incoming.setReplyText(null);
            if (!decision.allowed) {
                blockedCount = 1;
            }
        }

        dbHelper.insertComment(incoming);
        prefs.setLastCommentSyncTime(System.currentTimeMillis());
        RealSocialPublisher.notifyCommentsUpdated();

        String categoryLabel = CommentItem.getCategoryDisplayName(incoming.getCategory());
        String statusMessage;
        if (autoRepliedCount > 0) {
            NotificationHelper.showNotification(
                    context,
                    "🤖 [SocialAI Bot] Tự động phản hồi (" + platform.getShortName() + ")",
                    author + " [" + categoryLabel + "]: \"" + content + "\"\n👉 Bot đã tự động phản hồi an toàn!",
                    incoming.getId()
            );
            statusMessage = "Đã đồng bộ 1 bình luận " + platform.getShortName()
                    + " (" + categoryLabel + ") • Bot đã tự động trả lời an toàn.";
        } else if (CommentSafetyPolicy.shouldShowSafetyAlert(incoming)) {
            NotificationHelper.showNotification(
                    context,
                    "🛡️ [SocialAI Cảnh báo F6] Khóa tự động trả lời (" + platform.getShortName() + ")",
                    author + " [" + categoryLabel + "]: \"" + content + "\"\n⚠️ "
                            + CommentSafetyPolicy.getSafetyBannerSummary(incoming),
                    incoming.getId()
            );
            statusMessage = "Đã đồng bộ 1 bình luận " + platform.getShortName()
                    + " (" + categoryLabel + ") • Đã khóa tự động trả lời (cần duyệt tay).";
        } else {
            NotificationHelper.showCommentNotification(context, incoming);
            statusMessage = "Đã đồng bộ 1 bình luận mới trên " + platform.getShortName()
                    + " (" + categoryLabel + ") • AI đã soạn gợi ý trả lời.";
        }

        return Result.success(buildOutputData(1, autoRepliedCount, blockedCount, statusMessage));
    }

    private static Data buildOutputData(int newCount, int autoRepliedCount, int blockedCount, String message) {
        return new Data.Builder()
                .putInt(KEY_NEW_COUNT, newCount)
                .putInt(KEY_AUTO_REPLIED_COUNT, autoRepliedCount)
                .putInt(KEY_BLOCKED_COUNT, blockedCount)
                .putString(KEY_STATUS_MESSAGE, message != null ? message : "Đồng bộ bình luận hoàn tất.")
                .build();
    }

    /**
     * Đăng ký đồng bộ bình luận ngầm định kỳ mỗi 15 phút bằng Android WorkManager.
     */
    public static void schedulePeriodicSync(Context context) {
        if (context == null) return;
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        Data input = new Data.Builder()
                .putString(KEY_TRIGGER_SOURCE, TRIGGER_PERIODIC)
                .build();

        PeriodicWorkRequest periodicRequest = new PeriodicWorkRequest.Builder(
                CommentSyncWorker.class,
                15,
                TimeUnit.MINUTES
        )
                .setConstraints(constraints)
                .setInputData(input)
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
        );
    }

    /**
     * Kích hoạt đồng bộ tức thì thông qua CommentSyncWorker (dùng cho Pull-to-refresh trên Inbox).
     * Trả về UUID của WorkRequest để UI theo dõi tiến trình qua LiveData.
     */
    public static UUID enqueueImmediateSync(Context context) {
        Data input = new Data.Builder()
                .putString(KEY_TRIGGER_SOURCE, TRIGGER_MANUAL)
                .build();

        OneTimeWorkRequest immediateRequest = new OneTimeWorkRequest.Builder(CommentSyncWorker.class)
                .setInputData(input)
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                immediateRequest
        );
        return immediateRequest.getId();
    }

    /**
     * Lập lịch tự động đồng bộ comment sau một khoảng thời gian (VD: 12 giây sau khi đăng bài).
     */
    public static void scheduleSimulatedSync(Context context, long delaySeconds) {
        Data input = new Data.Builder()
                .putString(KEY_TRIGGER_SOURCE, TRIGGER_POST_PUBLISH)
                .build();

        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(CommentSyncWorker.class)
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .setInputData(input)
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                WORK_NAME + "_" + System.currentTimeMillis(),
                ExistingWorkPolicy.REPLACE,
                syncRequest
        );
    }
}
