package com.tuankiet.myapplication.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.tuankiet.myapplication.data.DatabaseHelper;
import com.tuankiet.myapplication.model.CommentItem;
import com.tuankiet.myapplication.model.SocialPlatform;
import com.tuankiet.myapplication.util.NotificationHelper;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class CommentSyncWorker extends Worker {

    public static final String WORK_NAME = "social_ai_comment_sync_worker";

    public CommentSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        String threadsToken = com.tuankiet.myapplication.data.SecurePreferenceManager.getInstance(context).getThreadsToken();
        if (threadsToken != null && !threadsToken.trim().isEmpty()) {
            // Nếu đã cấu hình Threads Token thật -> Quét comment thật trên Threads và cho Agent tự trả lời!
            com.tuankiet.myapplication.util.RealSocialPublisher.fetchRealThreadsComments(context, threadsToken, null);
            return Result.success();
        }

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);

        // Sinh 1 bình luận mới từ mạng xã hội để mô phỏng tương tác sau một thời gian
        Random random = new Random();
        boolean isSafeComment = random.nextInt(10) > 2; // 80% là an toàn, 20% là tiêu cực để test cả 2 luồng F6

        CommentItem incoming;
        if (isSafeComment) {
            String[] authors = {"Trần Hoàng Quân", "Lê Thị Thảo", "Vũ Minh Đức", "Bảo Trâm"};
            String[] contents = {
                    "Giải pháp tích hợp AI Agent này chạy rất mượt và tiết kiệm thời gian cho team Marketing!",
                    "Bài viết chia sẻ rất sâu sắc, ứng dụng thực tế cao. Cảm ơn tác giả!",
                    "Sản phẩm có phiên bản dùng thử cho doanh nghiệp nhỏ không bạn?"
            };
            String author = authors[random.nextInt(authors.length)];
            String content = contents[random.nextInt(contents.length)];
            String autoReply = "Dạ cảm ơn " + author + " đã quan tâm và để lại phản hồi tích cực! Bạn có thể xem thêm tài liệu tại trang chủ của SocialAI nhé.";

            incoming = new CommentItem(
                    0,
                    random.nextBoolean() ? SocialPlatform.FACEBOOK : SocialPlatform.LINKEDIN,
                    "Chiến lược Social Media Marketing 2026",
                    author,
                    author.substring(0, 1),
                    content,
                    "Vừa xong",
                    CommentItem.CATEGORY_PRAISE,
                    false,
                    null,
                    autoReply,
                    true, // Bot tự động trả lời thành công theo tiêu chuẩn F6
                    autoReply
            );

            dbHelper.insertComment(incoming);

            // Bắn Push Notification báo Bot đã tự động trả lời an toàn
            NotificationHelper.showNotification(
                    context,
                    "🤖 [SocialAI Bot] Tự động phản hồi (" + incoming.getPlatform().getDisplayName() + ")",
                    author + ": \"" + content + "\"\n👉 Bot đã tự động phân tích và gửi câu trả lời an toàn!"
            );
        } else {
            String author = "Hoàng Hải";
            String content = "Nội dung này nhìn giống bot tự sinh quá, thiếu cảm xúc cá nhân.";

            incoming = new CommentItem(
                    0,
                    SocialPlatform.FACEBOOK,
                    "Chiến lược Social Media Marketing 2026",
                    author,
                    "H",
                    content,
                    "Vừa xong",
                    CommentItem.CATEGORY_NEGATIVE,
                    true,
                    "Rủi ro thương hiệu: Bình luận nghi ngờ chất lượng AI. Tính năng tự động bị khóa.",
                    null,
                    false, // Chặn tự động theo F6
                    null
            );

            dbHelper.insertComment(incoming);

            // Bắn Push Notification cảnh báo an toàn
            NotificationHelper.showNotification(
                    context,
                    "🛡️ [SocialAI CẢNH BÁO F6] Nhận bình luận tiêu cực",
                    author + ": \"" + content + "\"\n⚠️ Bot đã khóa trả lời tự động để tránh rủi ro. Nhấn để duyệt thủ công!"
            );
        }

        return Result.success();
    }

    /**
     * Lập lịch tự động đồng bộ comment sau một khoảng thời gian (VD: 15 giây sau khi đăng bài để test ngay)
     */
    public static void scheduleSimulatedSync(Context context, long delaySeconds) {
        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(CommentSyncWorker.class)
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME + "_" + System.currentTimeMillis(),
                ExistingWorkPolicy.REPLACE,
                syncRequest
        );
    }
}

