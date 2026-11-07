package com.tuankiet.myapplication.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.tuankiet.myapplication.data.DatabaseHelper;
import com.tuankiet.myapplication.model.Post;
import com.tuankiet.myapplication.util.NotificationHelper;

import java.util.List;

public class PostPublisherWorker extends Worker {

    public static final String UNIQUE_WORK_NAME = "social_ai_offline_publisher";

    public PostPublisherWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        List<Post> posts = dbHelper.getAllPosts();

        int publishedCount = 0;
        for (Post post : posts) {
            if (Post.STATUS_PENDING.equals(post.getStatus())) {
                // Simulate publishing delay
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}

                post.setStatus(Post.STATUS_PUBLISHED);
                post.setReachCount(150);
                post.setLikeCount(18);
                post.setCommentCount(4);
                dbHelper.updatePost(post);
                publishedCount++;

                // Notify User
                NotificationHelper.showNotification(
                        context,
                        "Bài Viết Đã Tự Động Đăng Thành Công!",
                        "Bài: '" + post.getTopic() + "' từ Hàng đợi ngoại tuyến đã được đẩy lên Facebook & LinkedIn."
                );
            }
        }

        return Result.success();
    }

    public static void scheduleOfflinePublisher(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest publishRequest = new OneTimeWorkRequest.Builder(PostPublisherWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                publishRequest
        );
    }
}

