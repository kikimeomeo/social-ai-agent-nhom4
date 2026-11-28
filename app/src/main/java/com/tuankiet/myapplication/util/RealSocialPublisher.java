package com.tuankiet.myapplication.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tuankiet.myapplication.agent.AIAgentOrchestrator;
import com.tuankiet.myapplication.agent.CommentSafetyPolicy;
import com.tuankiet.myapplication.data.DatabaseHelper;
import com.tuankiet.myapplication.data.SecurePreferenceManager;
import com.tuankiet.myapplication.model.CommentItem;
import com.tuankiet.myapplication.model.SocialPlatform;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RealSocialPublisher {

    public interface ApiPublishCallback {
        void onSuccess(String realPostUrl, String postId);
        void onError(String errorMessage);
    }

    public interface FetchCommentsCallback {
        void onResult(int newCommentsCount, String message);
    }

    public interface OnCommentsUpdatedListener {
        void onCommentsUpdated();
    }

    public static class SyncResult {
        public final int newCommentsCount;
        public final int autoRepliedCount;
        public final int blockedCount;
        public final String statusMessage;

        public SyncResult(int newCommentsCount, int autoRepliedCount, int blockedCount, String statusMessage) {
            this.newCommentsCount = newCommentsCount;
            this.autoRepliedCount = autoRepliedCount;
            this.blockedCount = blockedCount;
            this.statusMessage = statusMessage;
        }
    }

    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final Set<String> botPublishedReplyIds = Collections.synchronizedSet(new HashSet<>());
    private static boolean isAutoScannerRunning = false;
    private static final AtomicBoolean isScanningInProgress = new AtomicBoolean(false);
    private static boolean hasShownPermissionHint = false;
    private static OnCommentsUpdatedListener commentsUpdatedListener;

    public static void setOnCommentsUpdatedListener(OnCommentsUpdatedListener listener) {
        commentsUpdatedListener = listener;
    }

    public static void notifyCommentsUpdated() {
        mainHandler.post(() -> {
            if (commentsUpdatedListener != null) {
                commentsUpdatedListener.onCommentsUpdated();
            }
        });
    }

    /**
     * BỘ QUÉT BÌNH LUẬN TỰ ĐỘNG KHI APP ĐANG MỞ (KẾT HỢP CÙNG CommentSyncWorker ĐỊNH KỲ)
     */
    public static void startAutoCommentScanner(Context context) {
        if (isAutoScannerRunning || context == null) return;
        isAutoScannerRunning = true;
        final Context appContext = context.getApplicationContext();

        Runnable scannerRunnable = new Runnable() {
            @Override
            public void run() {
                SecurePreferenceManager prefs = SecurePreferenceManager.getInstance(appContext);
                String threadsToken = prefs.getThreadsToken();
                String fbPageId = prefs.getFacebookPageId();
                String fbToken = prefs.getFacebookPageToken();
                boolean hasAnyRealToken = (threadsToken != null && !threadsToken.trim().isEmpty())
                        || (!fbPageId.isEmpty() && fbToken != null && !fbToken.trim().isEmpty());

                if (hasAnyRealToken && !isScanningInProgress.get() && NetworkUtils.isNetworkAvailable(appContext)) {
                    new Thread(() -> {
                        SyncResult result = syncAllRealCommentsSync(appContext, prefs.isControlledAutoReplyEnabled());
                        if (result.newCommentsCount > 0) {
                            notifyCommentsUpdated();
                        }
                    }).start();
                }
                mainHandler.postDelayed(this, 30000); // Quét nhẹ mỗi 30 giây khi app foreground
            }
        };

        mainHandler.postDelayed(scannerRunnable, 5000);
    }

    /**
     * PHƯƠNG ÁN 1: Mở app mạng xã hội thật (Facebook, Threads) và tự động
     * sao chép nội dung AI vào Clipboard để người dùng paste & đăng bài thật ngay lập tức!
     */
    public static void shareToRealApp(Activity activity, SocialPlatform platform, String text) {
        if (activity == null || text == null) return;

        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("SocialAI Post", text);
            clipboard.setPrimaryClip(clip);
        }

        String targetPackage = "";
        String platformName = "";

        if (platform == SocialPlatform.FACEBOOK) {
            targetPackage = "com.facebook.katana";
            platformName = "Facebook";
        } else if (platform == SocialPlatform.THREADS) {
            targetPackage = "com.instagram.barcelona";
            platformName = "Threads";
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);

        boolean appLaunched = false;
        if (!targetPackage.isEmpty()) {
            intent.setPackage(targetPackage);
            try {
                activity.startActivity(intent);
                appLaunched = true;
                Toast.makeText(activity, "📋 Đã sao chép nội dung! Đang mở ứng dụng " + platformName + " để đăng...", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                appLaunched = false;
            }
        }

        if (!appLaunched) {
            Intent chooser = Intent.createChooser(new Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, text), "Chia sẻ bài viết SocialAI lên:");
            activity.startActivity(chooser);
            Toast.makeText(activity, "📋 Đã sao chép nội dung AI! Vui lòng chọn ứng dụng để đăng.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Mở ứng dụng Threads thật (Instagram Threads) để đăng bài
     */
    public static void shareToThreads(Activity activity, String text) {
        if (activity == null || text == null) return;

        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("SocialAI Threads", text);
            clipboard.setPrimaryClip(clip);
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.setPackage("com.instagram.barcelona");

        try {
            activity.startActivity(intent);
            Toast.makeText(activity, "📋 Đã sao chép nội dung! Đang mở Threads...", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            try {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://threads.net"));
                activity.startActivity(webIntent);
                Toast.makeText(activity, "📋 Đã sao chép nội dung! Đang mở Threads trên trình duyệt.", Toast.LENGTH_LONG).show();
            } catch (Exception ignored) {}
        }
    }

    /**
     * PHƯƠNG ÁN 3: ĐĂNG TỰ ĐỘNG 100% LÊN THREADS QUA META THREADS API CHÍNH THỐNG
     */
    public static void postToThreadsViaApi(String threadsUserId, String accessToken, String text, ApiPublishCallback callback) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onError("Thiếu Threads User Access Token!");
            return;
        }

        final String cleanToken = accessToken.trim();
        final String safeText = (text != null && text.length() > 490)
                ? text.substring(0, 487) + "..."
                : (text != null ? text : "");

        String inputUserId = (threadsUserId == null || threadsUserId.trim().isEmpty()) ? "me" : threadsUserId.trim();

        if (!inputUserId.matches("\\d+")) {
            String meUrl = "https://graph.threads.net/v1.0/me?fields=id,username&access_token=" + Uri.encode(cleanToken);
            Request meReq = new Request.Builder().url(meUrl).get().build();
            client.newCall(meReq).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> callback.onError("Lỗi kết nối Threads API (/me): " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        String json = response.body().string();
                        try {
                            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                            String resolvedId = obj.has("id") ? obj.get("id").getAsString() : "me";
                            createAndPublishThreadsContainer(resolvedId, cleanToken, safeText, callback);
                        } catch (Exception e) {
                            createAndPublishThreadsContainer("me", cleanToken, safeText, callback);
                        }
                    } else {
                        String err = response.body() != null ? response.body().string() : "Mã lỗi " + response.code();
                        mainHandler.post(() -> callback.onError("Token Threads không hợp lệ hoặc đã hết hạn: " + err));
                    }
                }
            });
        } else {
            createAndPublishThreadsContainer(inputUserId, cleanToken, safeText, callback);
        }
    }

    private static void createAndPublishThreadsContainer(String userId, String cleanToken, String safeText, ApiPublishCallback callback) {
        String createUrl = "https://graph.threads.net/v1.0/" + userId + "/threads";

        RequestBody formBody = new FormBody.Builder()
                .add("media_type", "TEXT")
                .add("text", safeText)
                .add("access_token", cleanToken)
                .build();

        Request createReq = new Request.Builder()
                .url(createUrl)
                .post(formBody)
                .build();

        client.newCall(createReq).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Lỗi kết nối máy chủ Threads: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    try {
                        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                        if (obj.has("id")) {
                            String creationId = obj.get("id").getAsString();
                            mainHandler.postDelayed(() -> publishThreadsContainer(userId, creationId, cleanToken, callback), 1500);
                        } else {
                            mainHandler.post(() -> callback.onError("Threads không trả về creation_id: " + json));
                        }
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onError("Lỗi phân tích phản hồi Threads: " + e.getMessage()));
                    }
                } else {
                    String err = response.body() != null ? response.body().string() : "Mã lỗi " + response.code();
                    if (err.contains("Unsupported post request")) {
                        mainHandler.post(() -> callback.onError("Thiếu quyền 'threads_content_publish' trên Token này!\n\nChi tiết gốc từ Meta:\n" + err));
                    } else {
                        mainHandler.post(() -> callback.onError("Threads API từ chối (UserID=" + userId + "):\n" + err));
                    }
                }
            }
        });
    }

    private static void publishThreadsContainer(String userId, String creationId, String accessToken, ApiPublishCallback callback) {
        String publishUrl = "https://graph.threads.net/v1.0/" + userId + "/threads_publish";

        RequestBody body = new FormBody.Builder()
                .add("creation_id", creationId)
                .add("access_token", accessToken.trim())
                .build();

        Request req = new Request.Builder()
                .url(publishUrl)
                .post(body)
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Lỗi kết nối xuất bản Threads: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    try {
                        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                        if (!obj.has("id")
                                || obj.get("id").isJsonNull()
                                || obj.get("id").getAsString().trim().isEmpty()) {

                            mainHandler.post(() ->
                                    callback.onError(
                                            "Threads Publish API không trả về ID bài đăng hợp lệ."
                                    )
                            );
                            return;
                        }

                        String publishedId = obj.get("id").getAsString();

                        botPublishedReplyIds.add(publishedId);

                        // Lấy permalink thật từ Threads API
                        fetchThreadsPermalink(publishedId, accessToken, callback);
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onError("Lỗi giải mã ID bài đăng Threads: " + e.getMessage()));
                    }
                } else {
                    String err = response.body() != null ? response.body().string() : "Mã lỗi " + response.code();
                    mainHandler.post(() -> callback.onError("Threads Publish API từ chối: " + err));
                }
            }
        });
    }

    private static void fetchThreadsPermalink(
            String publishedId,
            String accessToken,
            ApiPublishCallback callback
    ) {
        String url = "https://graph.threads.net/v1.0/"
                + Uri.encode(publishedId)
                + "?fields=id,permalink";

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + accessToken.trim())
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                // Bài đã được xuất bản, nhưng chưa lấy được link.
                mainHandler.post(() ->
                        callback.onSuccess("", publishedId)
                );
            }

            @Override
            public void onResponse(
                    Call call,
                    Response response
            ) throws IOException {

                String permalink = "";

                try (Response ignored = response) {
                    if (response.isSuccessful()
                            && response.body() != null) {

                        String json = response.body().string();
                        JsonObject obj =
                                JsonParser.parseString(json).getAsJsonObject();

                        if (obj.has("permalink")
                                && !obj.get("permalink").isJsonNull()) {

                            permalink = obj.get("permalink").getAsString();
                        }
                    }
                } catch (Exception e) {
                    // Không coi lỗi lấy link là lỗi xuất bản.
                }

                final String finalPermalink = permalink;

                mainHandler.post(() ->
                        callback.onSuccess(finalPermalink, publishedId)
                );
            }
        });
    }

    /**
     * ĐỒNG BỘ ĐỒNG THỜI BÌNH LUẬN THẬT TỪ THREADS API & FACEBOOK PAGE GRAPH API (SYNCHRONOUS)
     * Dùng chung cho CommentSyncWorker (WorkManager nền) và Pull-to-refresh.
     * - Phân loại AI 5 nhóm: Câu hỏi, Khen ngợi, Góp ý, Tiêu cực, Spam.
     * - Tự soạn gợi ý theo ngữ cảnh bài đăng.
     * - Chỉ tự động trả lời có kiểm soát khi bật chế độ VÀ đạt CommentSafetyPolicy (Khen ngợi / FAQ, không PII, AI chắc chắn).
     */
    public static SyncResult syncAllRealCommentsSync(Context context, boolean allowAutoReply) {
        if (context == null) {
            return new SyncResult(0, 0, 0, "Thiếu ngữ cảnh ứng dụng.");
        }
        if (!isScanningInProgress.compareAndSet(false, true)) {
            return new SyncResult(0, 0, 0, "Đang đồng bộ bình luận, vui lòng chờ giây lát...");
        }

        Context appContext = context.getApplicationContext();
        SecurePreferenceManager prefs = SecurePreferenceManager.getInstance(appContext);
        String threadsToken = prefs.getThreadsToken();
        String fbPageId = prefs.getFacebookPageId();
        String fbToken = prefs.getFacebookPageToken();

        int totalAdded = 0;
        int totalAutoReplied = 0;
        int totalBlocked = 0;
        List<String> syncedChannels = new ArrayList<>();
        List<String> errorNotes = new ArrayList<>();

        try {
            if (threadsToken != null && !threadsToken.trim().isEmpty()) {
                SyncResult threadsRes = syncThreadsCommentsInternal(appContext, threadsToken.trim(), allowAutoReply);
                totalAdded += threadsRes.newCommentsCount;
                totalAutoReplied += threadsRes.autoRepliedCount;
                totalBlocked += threadsRes.blockedCount;
                syncedChannels.add("Threads");
                if (threadsRes.statusMessage != null && threadsRes.statusMessage.startsWith("⚠️")) {
                    errorNotes.add(threadsRes.statusMessage);
                }
            }

            if (fbPageId != null && !fbPageId.trim().isEmpty()
                    && fbToken != null && !fbToken.trim().isEmpty()) {
                SyncResult fbRes = syncFacebookCommentsInternal(appContext, fbPageId.trim(), fbToken.trim(), allowAutoReply);
                totalAdded += fbRes.newCommentsCount;
                totalAutoReplied += fbRes.autoRepliedCount;
                totalBlocked += fbRes.blockedCount;
                syncedChannels.add("Facebook");
                if (fbRes.statusMessage != null && fbRes.statusMessage.startsWith("⚠️")) {
                    errorNotes.add(fbRes.statusMessage);
                }
            }

            prefs.setLastCommentSyncTime(System.currentTimeMillis());
            if (totalAdded > 0) {
                notifyCommentsUpdated();
            }

            String summary;
            if (syncedChannels.isEmpty()) {
                summary = "Chưa cấu hình Token Facebook Page hoặc Threads.";
            } else if (!errorNotes.isEmpty() && totalAdded == 0) {
                summary = errorNotes.get(0);
            } else if (totalAdded > 0) {
                summary = "Đã đồng bộ " + android.text.TextUtils.join(" & ", syncedChannels)
                        + ": +" + totalAdded + " bình luận mới"
                        + (totalAutoReplied > 0 ? " • Bot trả lời: " + totalAutoReplied : "")
                        + (totalBlocked > 0 ? " • Chặn an toàn: " + totalBlocked : "");
            } else {
                summary = "Đã quét " + android.text.TextUtils.join(" & ", syncedChannels) + ": Chưa có bình luận mới.";
            }
            return new SyncResult(totalAdded, totalAutoReplied, totalBlocked, summary);
        } finally {
            isScanningInProgress.set(false);
        }
    }

    /**
     * Wrapper bất đồng bộ giữ tương thích ngược cho các caller cũ gọi fetchRealThreadsComments.
     */
    public static void fetchRealThreadsComments(Context context, String accessToken, FetchCommentsCallback callback) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            if (callback != null) mainHandler.post(() -> callback.onResult(0, "Chưa cấu hình Threads Token."));
            return;
        }
        final Context appContext = context.getApplicationContext();
        final String cleanToken = accessToken.trim();
        new Thread(() -> {
            if (!isScanningInProgress.compareAndSet(false, true)) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onResult(0, "Đang đồng bộ Threads..."));
                }
                return;
            }
            try {
                boolean allowAutoReply = SecurePreferenceManager.getInstance(appContext).isControlledAutoReplyEnabled();
                SyncResult res = syncThreadsCommentsInternal(appContext, cleanToken, allowAutoReply);
                SecurePreferenceManager.getInstance(appContext).setLastCommentSyncTime(System.currentTimeMillis());
                if (res.newCommentsCount > 0) {
                    notifyCommentsUpdated();
                }
                if (callback != null) {
                    mainHandler.post(() -> callback.onResult(res.newCommentsCount, res.statusMessage));
                }
            } finally {
                isScanningInProgress.set(false);
            }
        }).start();
    }

    private static SyncResult syncThreadsCommentsInternal(Context appContext, String cleanToken, boolean allowAutoReply) {
        int addedCount = 0;
        int autoRepliedCount = 0;
        int blockedCount = 0;

        try {
            // 0. Lấy numeric userId & username từ /me để bỏ qua reply của chính kênh
            String myUserId = "me";
            String myUsername = "";
            String meUrl = "https://graph.threads.net/v1.0/me?fields=id,username&access_token=" + Uri.encode(cleanToken);
            try (Response meResp = client.newCall(new Request.Builder().url(meUrl).get().build()).execute()) {
                if (meResp.isSuccessful() && meResp.body() != null) {
                    JsonObject meObj = JsonParser.parseString(meResp.body().string()).getAsJsonObject();
                    if (meObj.has("id") && !meObj.get("id").isJsonNull()) {
                        myUserId = meObj.get("id").getAsString();
                    }
                    if (meObj.has("username") && !meObj.get("username").isJsonNull()) {
                        myUsername = meObj.get("username").getAsString();
                    }
                }
            } catch (Exception ignored) {}

            // 1. Lấy 5 bài viết mới nhất trên Threads
            String postsUrl = "https://graph.threads.net/v1.0/me/threads?fields=id,text&limit=5&access_token=" + Uri.encode(cleanToken);
            Request postsReq = new Request.Builder().url(postsUrl).get().build();
            try (Response postsResp = client.newCall(postsReq).execute()) {
                if (!postsResp.isSuccessful() || postsResp.body() == null) {
                    String err = postsResp.body() != null ? postsResp.body().string() : "HTTP " + postsResp.code();
                    return new SyncResult(0, 0, 0, "⚠️ Không đọc được danh sách bài Threads: " + err);
                }

                JsonObject postsRoot = JsonParser.parseString(postsResp.body().string()).getAsJsonObject();
                if (!postsRoot.has("data")) {
                    return new SyncResult(0, 0, 0, "Không tìm thấy bài đăng nào trên Threads.");
                }

                JsonArray postsData = postsRoot.getAsJsonArray("data");
                DatabaseHelper db = DatabaseHelper.getInstance(appContext);
                List<CommentItem> existing = db.getAllComments();
                AIAgentOrchestrator agentOrchestrator = new AIAgentOrchestrator(appContext);

                for (int i = 0; i < postsData.size(); i++) {
                    JsonObject postObj = postsData.get(i).getAsJsonObject();
                    String postId = postObj.get("id").getAsString();
                    String fullPostText = (postObj.has("text") && !postObj.get("text").isJsonNull())
                            ? postObj.get("text").getAsString()
                            : "Bài đăng Threads";
                    String shortPostTitle = fullPostText.length() > 48
                            ? fullPostText.substring(0, 45) + "..."
                            : fullPostText;

                    String repliesUrl = "https://graph.threads.net/v1.0/" + postId
                            + "/replies?fields=id,text,username,timestamp&access_token=" + Uri.encode(cleanToken);
                    Request repReq = new Request.Builder().url(repliesUrl).get().build();
                    try (Response repResp = client.newCall(repReq).execute()) {
                        if (repResp.isSuccessful() && repResp.body() != null) {
                            JsonObject repRoot = JsonParser.parseString(repResp.body().string()).getAsJsonObject();
                            if (!repRoot.has("data")) continue;

                            JsonArray repData = repRoot.getAsJsonArray("data");
                            for (int j = 0; j < repData.size(); j++) {
                                JsonObject r = repData.get(j).getAsJsonObject();
                                String replyId = r.get("id").getAsString();
                                String replyText = (r.has("text") && !r.get("text").isJsonNull())
                                        ? r.get("text").getAsString().trim()
                                        : "";
                                String username = (r.has("username") && !r.get("username").isJsonNull())
                                        ? r.get("username").getAsString()
                                        : "threads_user";

                                if (replyText.isEmpty()
                                        || botPublishedReplyIds.contains(replyId)
                                        || (!myUsername.isEmpty() && myUsername.equalsIgnoreCase(username))) {
                                    continue;
                                }

                                if (isDuplicateComment(existing, replyId, replyText)) {
                                    continue;
                                }

                                // Phân loại 5 nhóm bằng AI + kiểm tra PII + độ tin cậy + soạn gợi ý theo ngữ cảnh bài viết
                                CommentSafetyPolicy.CommentAnalysis analysis = agentOrchestrator.analyzeCommentSync(
                                        SocialPlatform.THREADS,
                                        shortPostTitle,
                                        fullPostText,
                                        "@" + username,
                                        replyText
                                );

                                CommentItem comment = new CommentItem();
                                comment.setPlatform(SocialPlatform.THREADS);
                                comment.setPostTitle(shortPostTitle);
                                comment.setPostContent(fullPostText);
                                comment.setAuthorName("@" + username);
                                comment.setAuthorInitial(username.isEmpty() ? "T" : username.substring(0, 1).toUpperCase(Locale.ROOT));
                                comment.setContent(replyText);
                                comment.setTimeAgo("Threads • Vừa đồng bộ");
                                comment.setExternalCommentId(replyId);
                                CommentSafetyPolicy.applyAnalysisToComment(comment, analysis);

                                CommentSafetyPolicy.AutoReplyDecision decision = CommentSafetyPolicy.evaluateAutoReply(comment);
                                boolean repliedLive = false;
                                if (allowAutoReply && decision.allowed
                                        && comment.getAiSuggestedReply() != null
                                        && !comment.getAiSuggestedReply().trim().isEmpty()) {
                                    String publishedReplyId = replyToThreadsCommentSync(
                                            myUserId, cleanToken, replyId, comment.getAiSuggestedReply()
                                    );
                                    if (publishedReplyId != null) {
                                        botPublishedReplyIds.add(publishedReplyId);
                                        comment.setAnswered(true);
                                        comment.setAutoReplied(true);
                                        comment.setReplyText(comment.getAiSuggestedReply());
                                        repliedLive = true;
                                        autoRepliedCount++;
                                    }
                                } else if (!decision.allowed) {
                                    blockedCount++;
                                }

                                db.insertComment(comment);
                                existing.add(comment);
                                addedCount++;

                                notifyIncomingComment(appContext, comment, repliedLive);
                            }
                        } else if (repResp.code() == 400 || repResp.code() == 403) {
                            String errBody = repResp.body() != null ? repResp.body().string() : "";
                            if (errBody.contains("permission") || errBody.contains("Unsupported get request")) {
                                String permMsg = "⚠️ Để đọc comment thật trên Threads, cần cấp quyền 'threads_read_replies' và 'threads_manage_replies' cho Token.";
                                if (!hasShownPermissionHint) {
                                    hasShownPermissionHint = true;
                                    mainHandler.post(() -> Toast.makeText(appContext, permMsg, Toast.LENGTH_LONG).show());
                                }
                                return new SyncResult(addedCount, autoRepliedCount, blockedCount, permMsg);
                            }
                        }
                    }
                }

                String msg = addedCount > 0
                        ? "Đã đồng bộ " + addedCount + " bình luận mới trên Threads."
                        : "Đã quét Threads: Chưa có bình luận mới.";
                return new SyncResult(addedCount, autoRepliedCount, blockedCount, msg);
            }
        } catch (Exception e) {
            return new SyncResult(addedCount, autoRepliedCount, blockedCount, "⚠️ Lỗi đồng bộ Threads: " + e.getMessage());
        }
    }

    /**
     * Đọc bình luận thật từ Facebook Page qua Meta Graph API v19.0
     * 1. GET https://graph.facebook.com/v19.0/{page_id}/posts?fields=id,message&limit=5
     * 2. GET https://graph.facebook.com/v19.0/{post_id}/comments?fields=id,message,from,created_time
     */
    private static SyncResult syncFacebookCommentsInternal(Context appContext,
                                                           String pageId,
                                                           String pageAccessToken,
                                                           boolean allowAutoReply) {
        int addedCount = 0;
        int autoRepliedCount = 0;
        int blockedCount = 0;

        try {
            String postsUrl = "https://graph.facebook.com/v19.0/" + Uri.encode(pageId)
                    + "/posts?fields=id,message&limit=5&access_token=" + Uri.encode(pageAccessToken);
            try (Response postsResp = client.newCall(new Request.Builder().url(postsUrl).get().build()).execute()) {
                if (!postsResp.isSuccessful() || postsResp.body() == null) {
                    String err = postsResp.body() != null ? postsResp.body().string() : "HTTP " + postsResp.code();
                    return new SyncResult(0, 0, 0, "⚠️ Không đọc được bài viết Facebook Page: " + err);
                }

                JsonObject postsRoot = JsonParser.parseString(postsResp.body().string()).getAsJsonObject();
                if (!postsRoot.has("data")) {
                    return new SyncResult(0, 0, 0, "Không tìm thấy bài viết trên Facebook Page.");
                }

                JsonArray postsData = postsRoot.getAsJsonArray("data");
                DatabaseHelper db = DatabaseHelper.getInstance(appContext);
                List<CommentItem> existing = db.getAllComments();
                AIAgentOrchestrator agentOrchestrator = new AIAgentOrchestrator(appContext);

                for (int i = 0; i < postsData.size(); i++) {
                    JsonObject postObj = postsData.get(i).getAsJsonObject();
                    String postId = postObj.get("id").getAsString();
                    String fullPostMessage = (postObj.has("message") && !postObj.get("message").isJsonNull())
                            ? postObj.get("message").getAsString()
                            : "Bài đăng Facebook Page";
                    String shortPostTitle = fullPostMessage.length() > 48
                            ? fullPostMessage.substring(0, 45) + "..."
                            : fullPostMessage;

                    String commentsUrl = "https://graph.facebook.com/v19.0/" + Uri.encode(postId)
                            + "/comments?fields=id,message,from,created_time&limit=15&access_token=" + Uri.encode(pageAccessToken);
                    try (Response cResp = client.newCall(new Request.Builder().url(commentsUrl).get().build()).execute()) {
                        if (!cResp.isSuccessful() || cResp.body() == null) continue;
                        JsonObject cRoot = JsonParser.parseString(cResp.body().string()).getAsJsonObject();
                        if (!cRoot.has("data")) continue;

                        JsonArray cData = cRoot.getAsJsonArray("data");
                        for (int j = 0; j < cData.size(); j++) {
                            JsonObject cObj = cData.get(j).getAsJsonObject();
                            String commentId = cObj.get("id").getAsString();
                            String commentText = (cObj.has("message") && !cObj.get("message").isJsonNull())
                                    ? cObj.get("message").getAsString().trim()
                                    : "";

                            String authorName = "Người dùng Facebook";
                            String authorId = "";
                            if (cObj.has("from") && cObj.get("from").isJsonObject()) {
                                JsonObject fromObj = cObj.getAsJsonObject("from");
                                if (fromObj.has("name") && !fromObj.get("name").isJsonNull()) {
                                    authorName = fromObj.get("name").getAsString();
                                }
                                if (fromObj.has("id") && !fromObj.get("id").isJsonNull()) {
                                    authorId = fromObj.get("id").getAsString();
                                }
                            }

                            if (commentText.isEmpty()
                                    || pageId.equals(authorId)
                                    || botPublishedReplyIds.contains(commentId)
                                    || isDuplicateComment(existing, commentId, commentText)) {
                                continue;
                            }

                            CommentSafetyPolicy.CommentAnalysis analysis = agentOrchestrator.analyzeCommentSync(
                                    SocialPlatform.FACEBOOK,
                                    shortPostTitle,
                                    fullPostMessage,
                                    authorName,
                                    commentText
                            );

                            CommentItem comment = new CommentItem();
                            comment.setPlatform(SocialPlatform.FACEBOOK);
                            comment.setPostTitle(shortPostTitle);
                            comment.setPostContent(fullPostMessage);
                            comment.setAuthorName(authorName);
                            comment.setAuthorInitial(authorName.isEmpty() ? "F" : authorName.substring(0, 1).toUpperCase(Locale.ROOT));
                            comment.setContent(commentText);
                            comment.setTimeAgo("Facebook • Vừa đồng bộ");
                            comment.setExternalCommentId(commentId);
                            CommentSafetyPolicy.applyAnalysisToComment(comment, analysis);

                            CommentSafetyPolicy.AutoReplyDecision decision = CommentSafetyPolicy.evaluateAutoReply(comment);
                            boolean repliedLive = false;
                            if (allowAutoReply && decision.allowed
                                    && comment.getAiSuggestedReply() != null
                                    && !comment.getAiSuggestedReply().trim().isEmpty()) {
                                String publishedReplyId = replyToFacebookCommentSync(
                                        pageAccessToken, commentId, comment.getAiSuggestedReply()
                                );
                                if (publishedReplyId != null) {
                                    botPublishedReplyIds.add(publishedReplyId);
                                    comment.setAnswered(true);
                                    comment.setAutoReplied(true);
                                    comment.setReplyText(comment.getAiSuggestedReply());
                                    repliedLive = true;
                                    autoRepliedCount++;
                                }
                            } else if (!decision.allowed) {
                                blockedCount++;
                            }

                            db.insertComment(comment);
                            existing.add(comment);
                            addedCount++;

                            notifyIncomingComment(appContext, comment, repliedLive);
                        }
                    }
                }

                String msg = addedCount > 0
                        ? "Đã đồng bộ " + addedCount + " bình luận mới trên Facebook Page."
                        : "Đã quét Facebook Page: Chưa có bình luận mới.";
                return new SyncResult(addedCount, autoRepliedCount, blockedCount, msg);
            }
        } catch (Exception e) {
            return new SyncResult(addedCount, autoRepliedCount, blockedCount, "⚠️ Lỗi đồng bộ Facebook: " + e.getMessage());
        }
    }

    private static boolean isDuplicateComment(List<CommentItem> existing, String externalId, String commentText) {
        if (existing == null) return false;
        String tagId = "ID:" + externalId;
        for (CommentItem ex : existing) {
            if (externalId != null && externalId.equals(ex.getExternalCommentId())) {
                return true;
            }
            if (ex.getTimeAgo() != null && ex.getTimeAgo().contains(tagId)) {
                return true;
            }
            if (ex.getReplyText() != null && ex.getReplyText().trim().equalsIgnoreCase(commentText)) {
                return true;
            }
        }
        return false;
    }

    private static void notifyIncomingComment(Context appContext, CommentItem comment, boolean repliedLive) {
        if (repliedLive) {
            NotificationHelper.showNotification(
                    appContext,
                    "🤖 [AI Agent] Tự động trả lời (" + comment.getPlatform().getShortName() + ")",
                    comment.getAuthorName() + ": \"" + comment.getContent() + "\"\n👉 Bot: \"" + comment.getReplyText() + "\"",
                    comment.getId()
            );
        } else if (CommentSafetyPolicy.shouldShowSafetyAlert(comment)) {
            NotificationHelper.showNotification(
                    appContext,
                    "🛡️ [Cảnh báo F6] Khóa tự động trả lời (" + comment.getPlatform().getShortName() + ")",
                    comment.getAuthorName() + ": \"" + comment.getContent() + "\"\n⚠️ "
                            + CommentSafetyPolicy.getSafetyBannerSummary(comment),
                    comment.getId()
            );
        } else {
            NotificationHelper.showCommentNotification(appContext, comment);
        }
    }

    /**
     * Gửi trả lời bình luận Threads đồng bộ (Synchronous) dành cho Worker & Controlled Auto-Reply Bot
     */
    public static String replyToThreadsCommentSync(String cleanToken, String replyToCommentId, String replyText) {
        if (cleanToken == null || cleanToken.trim().isEmpty() || replyToCommentId == null || replyToCommentId.trim().isEmpty()) {
            return null;
        }
        String token = cleanToken.trim();
        String userId = "me";
        try {
            String meUrl = "https://graph.threads.net/v1.0/me?fields=id&access_token=" + Uri.encode(token);
            try (Response meResp = client.newCall(new Request.Builder().url(meUrl).get().build()).execute()) {
                if (meResp.isSuccessful() && meResp.body() != null) {
                    JsonObject obj = JsonParser.parseString(meResp.body().string()).getAsJsonObject();
                    if (obj.has("id") && !obj.get("id").isJsonNull()) {
                        userId = obj.get("id").getAsString();
                    }
                }
            }
        } catch (Exception ignored) {}
        return replyToThreadsCommentSync(userId, token, replyToCommentId.trim(), replyText);
    }

    private static String replyToThreadsCommentSync(String resolvedUserId, String cleanToken, String replyToCommentId, String replyText) {
        try {
            String createUrl = "https://graph.threads.net/v1.0/" + resolvedUserId + "/threads";
            RequestBody formBody = new FormBody.Builder()
                    .add("media_type", "TEXT")
                    .add("text", replyText)
                    .add("reply_to_id", replyToCommentId)
                    .add("access_token", cleanToken)
                    .build();

            try (Response createResp = client.newCall(new Request.Builder().url(createUrl).post(formBody).build()).execute()) {
                if (!createResp.isSuccessful() || createResp.body() == null) {
                    return null;
                }
                JsonObject createObj = JsonParser.parseString(createResp.body().string()).getAsJsonObject();
                if (!createObj.has("id")) return null;
                String creationId = createObj.get("id").getAsString();

                Thread.sleep(1500);

                String publishUrl = "https://graph.threads.net/v1.0/" + resolvedUserId + "/threads_publish";
                RequestBody pubBody = new FormBody.Builder()
                        .add("creation_id", creationId)
                        .add("access_token", cleanToken)
                        .build();

                try (Response pubResp = client.newCall(new Request.Builder().url(publishUrl).post(pubBody).build()).execute()) {
                    if (pubResp.isSuccessful() && pubResp.body() != null) {
                        JsonObject pubObj = JsonParser.parseString(pubResp.body().string()).getAsJsonObject();
                        String pubId = pubObj.has("id") ? pubObj.get("id").getAsString() : creationId;
                        botPublishedReplyIds.add(pubId);
                        return pubId;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Gửi trả lời bình luận Facebook Page đồng bộ (Synchronous) qua Meta Graph API v19.0
     * POST https://graph.facebook.com/v19.0/{comment_id}/comments
     */
    public static String replyToFacebookCommentSync(String pageAccessToken, String commentId, String replyText) {
        if (pageAccessToken == null || pageAccessToken.trim().isEmpty()
                || commentId == null || commentId.trim().isEmpty()
                || replyText == null || replyText.trim().isEmpty()) {
            return null;
        }
        try {
            String url = "https://graph.facebook.com/v19.0/" + Uri.encode(commentId.trim()) + "/comments";
            RequestBody body = new FormBody.Builder()
                    .add("message", replyText.trim())
                    .add("access_token", pageAccessToken.trim())
                    .build();

            try (Response resp = client.newCall(new Request.Builder().url(url).post(body).build()).execute()) {
                if (resp.isSuccessful() && resp.body() != null) {
                    JsonObject obj = JsonParser.parseString(resp.body().string()).getAsJsonObject();
                    String newReplyId = obj.has("id") ? obj.get("id").getAsString() : commentId;
                    botPublishedReplyIds.add(newReplyId);
                    return newReplyId;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Trả lời trực tiếp bình luận Facebook Page bất đồng bộ qua Meta Graph API v19.0
     */
    public static void replyToFacebookCommentViaApi(String pageAccessToken,
                                                    String commentId,
                                                    String replyText,
                                                    ApiPublishCallback callback) {
        if (pageAccessToken == null || pageAccessToken.trim().isEmpty()) {
            if (callback != null) mainHandler.post(() -> callback.onError("Thiếu Facebook Page Access Token!"));
            return;
        }
        if (commentId == null || commentId.trim().isEmpty()) {
            if (callback != null) mainHandler.post(() -> callback.onError("Thiếu ID bình luận Facebook!"));
            return;
        }

        String url = "https://graph.facebook.com/v19.0/" + Uri.encode(commentId.trim()) + "/comments";
        RequestBody body = new FormBody.Builder()
                .add("message", replyText != null ? replyText.trim() : "")
                .add("access_token", pageAccessToken.trim())
                .build();

        client.newCall(new Request.Builder().url(url).post(body).build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("Lỗi kết nối Facebook Graph API: " + e.getMessage()));
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response resp = response) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        String json = resp.body().string();
                        String newId = commentId;
                        try {
                            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                            if (obj.has("id") && !obj.get("id").isJsonNull()) {
                                newId = obj.get("id").getAsString();
                                botPublishedReplyIds.add(newId);
                            }
                        } catch (Exception ignored) {}
                        final String finalId = newId;
                        if (callback != null) {
                            mainHandler.post(() -> callback.onSuccess("https://www.facebook.com/" + finalId, finalId));
                        }
                    } else {
                        String err = resp.body() != null ? resp.body().string() : "HTTP " + resp.code();
                        if (callback != null) {
                            mainHandler.post(() -> callback.onError("Facebook Graph API từ chối trả lời: " + err));
                        }
                    }
                }
            }
        });
    }

    /**
     * TRẢ LỜI TRỰC TIẾP COMMENT THẬT TRÊN THREADS QUA API (reply_to_id)
     */
    public static void replyToThreadsCommentViaApi(String accessToken, String replyToCommentId, String replyText, ApiPublishCallback callback) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onError("Thiếu Threads Access Token!");
            return;
        }
        final String cleanToken = accessToken.trim();
        String meUrl = "https://graph.threads.net/v1.0/me?fields=id&access_token=" + Uri.encode(cleanToken);
        client.newCall(new Request.Builder().url(meUrl).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Lỗi kết nối Threads: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String userId = "me";
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject obj = JsonParser.parseString(response.body().string()).getAsJsonObject();
                        if (obj.has("id")) userId = obj.get("id").getAsString();
                    } catch (Exception ignored) {}
                }
                final String resolvedUserId = userId;
                String createUrl = "https://graph.threads.net/v1.0/" + resolvedUserId + "/threads";
                RequestBody formBody = new FormBody.Builder()
                        .add("media_type", "TEXT")
                        .add("text", replyText)
                        .add("reply_to_id", replyToCommentId)
                        .add("access_token", cleanToken)
                        .build();

                client.newCall(new Request.Builder().url(createUrl).post(formBody).build()).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> callback.onError("Lỗi tạo reply container: " + e.getMessage()));
                    }

                    @Override
                    public void onResponse(Call call, Response resp) throws IOException {
                        if (resp.isSuccessful() && resp.body() != null) {
                            try {
                                JsonObject obj = JsonParser.parseString(resp.body().string()).getAsJsonObject();
                                String creationId = obj.get("id").getAsString();
                                mainHandler.postDelayed(() -> publishThreadsContainer(resolvedUserId, creationId, cleanToken, callback), 1500);
                            } catch (Exception e) {
                                mainHandler.post(() -> callback.onError("Lỗi đọc ID reply: " + e.getMessage()));
                            }
                        } else {
                            String err = resp.body() != null ? resp.body().string() : "HTTP " + resp.code();
                            mainHandler.post(() -> callback.onError("Threads từ chối trả lời comment: " + err));
                        }
                    }
                });
            }
        });
    }

    /**
     * PHƯƠNG ÁN 2: Đăng tự động qua Meta Graph API (Dành cho Facebook Page thật khi có Page Access Token)
     * Endpoint: POST https://graph.facebook.com/v19.0/{page_id}/feed
     */
    public static void postToFacebookPageViaApi(String pageId, String pageAccessToken, String message, ApiPublishCallback callback) {
        if (pageId == null || pageId.trim().isEmpty() || pageAccessToken == null || pageAccessToken.trim().isEmpty()) {
            callback.onError("Thiếu Page ID hoặc Page Access Token!");
            return;
        }

        String url = "https://graph.facebook.com/v19.0/" + pageId.trim() + "/feed";

        RequestBody body = new FormBody.Builder()
                .add("message", message)
                .add("access_token", pageAccessToken.trim())
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Lỗi kết nối mạng: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    try {
                        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                        String postId = obj.has("id") ? obj.get("id").getAsString() : "";
                        String postUrl = "https://www.facebook.com/" + postId;
                        mainHandler.post(() -> callback.onSuccess(postUrl, postId));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onError("Lỗi phân tích phản hồi Facebook: " + e.getMessage()));
                    }
                } else {
                    String err = response.body() != null ? response.body().string() : "Mã lỗi " + response.code();
                    mainHandler.post(() -> callback.onError("Meta Graph API từ chối: " + err));
                }
            }
        });
    }

    // ==========================================
    // F5: SYNCHRONOUS METHODS FOR WORKMANAGER
    // ==========================================

    public static class PublishResult {
        public final boolean success;
        public final String postUrl;
        public final String postId;
        public final String errorMessage;
        public final boolean isRetryable;

        public PublishResult(boolean success, String postUrl, String postId, String errorMessage, boolean isRetryable) {
            this.success = success;
            this.postUrl = postUrl;
            this.postId = postId;
            this.errorMessage = errorMessage;
            this.isRetryable = isRetryable;
        }

        public static PublishResult success(String postUrl, String postId) {
            return new PublishResult(true, postUrl, postId, null, false);
        }

        public static PublishResult retryable(String errorMessage) {
            return new PublishResult(false, null, null, errorMessage, true);
        }

        public static PublishResult failure(String errorMessage) {
            return new PublishResult(false, null, null, errorMessage, false);
        }
    }

    public static PublishResult postToFacebookPageSync(String pageId, String pageAccessToken, String message) {
        if (pageId == null || pageId.trim().isEmpty() || pageAccessToken == null || pageAccessToken.trim().isEmpty()) {
            return PublishResult.failure("Thiếu Page ID hoặc Facebook Page Access Token!");
        }

        String url = "https://graph.facebook.com/v19.0/" + pageId.trim() + "/feed";
        RequestBody body = new FormBody.Builder()
                .add("message", message != null ? message : "")
                .add("access_token", pageAccessToken.trim())
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                String postId = obj.has("id") ? obj.get("id").getAsString() : "";
                String postUrl = "https://www.facebook.com/" + postId;
                return PublishResult.success(postUrl, postId);
            } else {
                int code = response.code();
                String errBody = response.body() != null ? response.body().string() : "";
                if (code == 429 || errBody.contains("rate limit") || errBody.contains("too many requests") || code >= 500) {
                    return PublishResult.retryable("Facebook API giới hạn tần suất hoặc máy chủ bận (HTTP " + code + "): " + errBody);
                } else if (code == 401 || code == 403 || errBody.contains("OAuthException") || errBody.contains("permission")) {
                    return PublishResult.failure("Lỗi xác thực Facebook (HTTP " + code + "): Token hết hạn hoặc thiếu quyền. " + errBody);
                } else if (errBody.contains("policy") || errBody.contains("violation") || errBody.contains("blocked")) {
                    return PublishResult.failure("Nội dung vi phạm chính sách của Facebook: " + errBody);
                } else {
                    return PublishResult.failure("Meta Graph API từ chối (HTTP " + code + "): " + errBody);
                }
            }
        } catch (IOException e) {
            return PublishResult.retryable("Lỗi kết nối mạng khi đăng lên Facebook: " + e.getMessage());
        } catch (Exception e) {
            return PublishResult.failure("Lỗi xử lý phản hồi Facebook: " + e.getMessage());
        }
    }

    public static PublishResult postToThreadsSync(String threadsUserId, String accessToken, String text) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            return PublishResult.failure("Thiếu Threads Access Token!");
        }

        final String cleanToken = accessToken.trim();
        final String safeText = (text != null && text.length() > 490)
                ? text.substring(0, 487) + "..."
                : (text != null ? text : "");

        String userId = (threadsUserId == null || threadsUserId.trim().isEmpty()) ? "me" : threadsUserId.trim();

        try {
            if (!userId.matches("\\d+")) {
                String meUrl = "https://graph.threads.net/v1.0/me?fields=id,username&access_token=" + Uri.encode(cleanToken);
                Request meReq = new Request.Builder().url(meUrl).get().build();
                try (Response meResp = client.newCall(meReq).execute()) {
                    if (meResp.isSuccessful() && meResp.body() != null) {
                        JsonObject obj = JsonParser.parseString(meResp.body().string()).getAsJsonObject();
                        if (obj.has("id")) {
                            userId = obj.get("id").getAsString();
                        }
                    } else {
                        int code = meResp.code();
                        String err = meResp.body() != null ? meResp.body().string() : "";
                        if (code >= 500) {
                            return PublishResult.retryable("Máy chủ Threads bận (/me HTTP " + code + "): " + err);
                        } else {
                            return PublishResult.failure("Threads Token không hợp lệ hoặc đã hết hạn: " + err);
                        }
                    }
                }
            }

            String createUrl = "https://graph.threads.net/v1.0/" + userId + "/threads";
            RequestBody formBody = new FormBody.Builder()
                    .add("media_type", "TEXT")
                    .add("text", safeText)
                    .add("access_token", cleanToken)
                    .build();
            Request createReq = new Request.Builder().url(createUrl).post(formBody).build();

            String creationId;
            try (Response createResp = client.newCall(createReq).execute()) {
                if (createResp.isSuccessful() && createResp.body() != null) {
                    JsonObject obj = JsonParser.parseString(createResp.body().string()).getAsJsonObject();
                    if (obj.has("id")) {
                        creationId = obj.get("id").getAsString();
                    } else {
                        return PublishResult.failure("Threads không trả về creation_id");
                    }
                } else {
                    int code = createResp.code();
                    String err = createResp.body() != null ? createResp.body().string() : "";
                    if (code == 429 || code >= 500) {
                        return PublishResult.retryable("Lỗi kết nối máy chủ Threads khi tạo container (HTTP " + code + "): " + err);
                    } else if (err.contains("threads_content_publish") || code == 401 || code == 403) {
                        return PublishResult.failure("Thiếu quyền 'threads_content_publish' hoặc Token không hợp lệ: " + err);
                    } else {
                        return PublishResult.failure("Threads API từ chối tạo bài viết (HTTP " + code + "): " + err);
                    }
                }
            }

            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {}

            String publishUrl = "https://graph.threads.net/v1.0/" + userId + "/threads_publish";
            RequestBody pubBody = new FormBody.Builder()
                    .add("creation_id", creationId)
                    .add("access_token", cleanToken)
                    .build();
            Request pubReq = new Request.Builder().url(publishUrl).post(pubBody).build();

            String publishedId;
            try (Response pubResp = client.newCall(pubReq).execute()) {
                if (pubResp.isSuccessful() && pubResp.body() != null) {
                    JsonObject obj = JsonParser.parseString(pubResp.body().string()).getAsJsonObject();
                    if (obj.has("id")) {
                        publishedId = obj.get("id").getAsString();
                    } else {
                        return PublishResult.failure("Threads Publish API không trả về ID bài đăng hợp lệ");
                    }
                } else {
                    int code = pubResp.code();
                    String err = pubResp.body() != null ? pubResp.body().string() : "";
                    if (code == 429 || code >= 500) {
                        return PublishResult.retryable("Lỗi kết nối xuất bản Threads (HTTP " + code + "): " + err);
                    } else {
                        return PublishResult.failure("Threads Publish API từ chối: " + err);
                    }
                }
            }

            botPublishedReplyIds.add(publishedId);

            String permalink = "https://www.threads.net/@" + userId + "/post/" + publishedId;
            try {
                String linkUrl = "https://graph.threads.net/v1.0/" + Uri.encode(publishedId) + "?fields=id,permalink";
                Request linkReq = new Request.Builder()
                        .url(linkUrl)
                        .header("Authorization", "Bearer " + cleanToken)
                        .get()
                        .build();
                try (Response linkResp = client.newCall(linkReq).execute()) {
                    if (linkResp.isSuccessful() && linkResp.body() != null) {
                        JsonObject obj = JsonParser.parseString(linkResp.body().string()).getAsJsonObject();
                        if (obj.has("permalink") && !obj.get("permalink").isJsonNull()) {
                            permalink = obj.get("permalink").getAsString();
                        }
                    }
                }
            } catch (Exception ignored) {}

            return PublishResult.success(permalink, publishedId);

        } catch (IOException e) {
            return PublishResult.retryable("Lỗi kết nối mạng khi xuất bản lên Threads: " + e.getMessage());
        } catch (Exception e) {
            return PublishResult.failure("Lỗi xử lý Threads API: " + e.getMessage());
        }
    }
}
