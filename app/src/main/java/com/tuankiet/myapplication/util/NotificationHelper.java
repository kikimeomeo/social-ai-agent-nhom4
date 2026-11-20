package com.tuankiet.myapplication.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.tuankiet.myapplication.MainActivity;
import com.tuankiet.myapplication.R;
import com.tuankiet.myapplication.model.CommentItem;

public class NotificationHelper {

    private static final String CHANNEL_ID = "social_ai_channel";
    private static final String CHANNEL_NAME = "SocialAI Notifications";
    public static final String EXTRA_COMMENT_ID = "target_comment_id";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Thông báo bình luận mới và tiến trình xuất bản SocialAI");
            channel.enableVibration(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void showNotification(Context context, String title, String message) {
        showNotification(context, title, message, -1L);
    }

    public static void showNotification(Context context, String title, String message, long commentId) {
        createNotificationChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("target_tab", R.id.nav_inbox);
        if (commentId > 0) {
            intent.putExtra(EXTRA_COMMENT_ID, commentId);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int requestCode = commentId > 0 ? (int) (commentId & 0x7fffffff)
                : (int) (System.currentTimeMillis() & 0x7fffffff);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_sparkles)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify((int) (System.currentTimeMillis() & 0x7fffffff),
                    builder.build());
        } catch (SecurityException ignored) {
            // Android 13+ permission may be pending
        }
    }

    public static void showCommentNotification(Context context, CommentItem item) {
        String channelName = item.getPlatform() != null ? item.getPlatform().getDisplayName() : "SocialAI";
        String title = "Bình luận mới từ " + item.getAuthorName() + " (" + channelName + ")";
        String msg = "[" + CommentItem.getCategoryDisplayName(item.getCategory()) + "] " + item.getContent();
        showNotification(context, title, msg, item.getId());
    }
}
