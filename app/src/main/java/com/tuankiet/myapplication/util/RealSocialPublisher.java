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
import com.tuankiet.myapplication.data.DatabaseHelper;
import com.tuankiet.myapplication.data.SecurePreferenceManager;
import com.tuankiet.myapplication.model.CommentItem;
import com.tuankiet.myapplication.model.SocialPlatform;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final Set<String> botPublishedReplyIds = Collections.synchronizedSet(new HashSet<>());
    private static boolean isAutoScannerRunning = false;
    private static boolean isScanningInProgress = false;
    private static boolean hasShownPermissionHint = false;
    private static OnCommentsUpdatedListener commentsUpdatedListener;

    public static void setOnCommentsUpdatedListener(OnCommentsUpdatedListener listener) {
        commentsUpdatedListener = listener;
    }

    /**
     * BỘ QUÉT BÌNH LUẬN THREADS TỰ ĐỘNG MỖI 12 GIÂY & AI AGENT TỰ ĐỘNG TRẢ LỜI (F6)
     */
    public static void startAutoCommentScanner(Context context) {
        if (isAutoScannerRunning || context == null) return;
        isAutoScannerRunning = true;
        final Context appContext = context.getApplicationContext();

        Runnable scannerRunnable = new Runnable() {
            @Override
            public void run() {
                String token = SecurePreferenceManager.getInstance(appContext).getThreadsToken();
                if (token != null && !token.trim().isEmpty() && !isScanningInProgress) {
                    fetchRealThreadsComments(appContext, token, (count, msg) -> {
                        if (count > 0 && commentsUpdatedListener != null) {
                            commentsUpdatedListener.onCommentsUpdated();
                        }
                    });
                }
                mainHandler.postDelayed(this, 12000); // Quét định kỳ mỗi 12 giây
            }
        };

        mainHandler.postDelayed(scannerRunnable, 4000);
    }

    /**
     * PHƯƠNG ÁN 1: Mở app mạng xã hội thật (Facebook, LinkedIn, Threads) và tự động
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
        } else if (platform == SocialPlatform.LINKEDIN) {
            targetPackage = "com.linkedin.android";
            platformName = "LinkedIn";
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
     * QUÉT COMMENT THẬT TỪ THREADS API & AI AGENT TỰ ĐỘNG PHẢN HỒI CÓ KIỂM SOÁT (F6)
     * 1. GET https://graph.threads.net/v1.0/me/threads?fields=id,text&limit=5
     * 2. GET https://graph.threads.net/v1.0/{post_id}/replies?fields=id,text,username,timestamp
     * 3. Nếu phát hiện comment mới:
     *    - Nếu An Toàn (Khen ngợi / Câu hỏi): Gọi Gemini AI Agent sinh câu trả lời và tự động bắn reply lên Threads!
     *    - Nếu Tiêu Cực (Rủi ro): Chặn tự động trả lời, báo động đỏ về Hộp thư để duyệt thủ công!
     */
    public static void fetchRealThreadsComments(Context context, String accessToken, FetchCommentsCallback callback) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            if (callback != null) mainHandler.post(() -> callback.onResult(0, "Chưa cấu hình Threads Token."));
            return;
        }
        if (isScanningInProgress) return;
        isScanningInProgress = true;

        final Context appContext = context.getApplicationContext();
        final String cleanToken = accessToken.trim();

        new Thread(() -> {
            try {
                // 0. Lấy numeric userId từ /me
                String myUserId = "me";
                String meUrl = "https://graph.threads.net/v1.0/me?fields=id,username&access_token=" + Uri.encode(cleanToken);
                try (Response meResp = client.newCall(new Request.Builder().url(meUrl).get().build()).execute()) {
                    if (meResp.isSuccessful() && meResp.body() != null) {
                        JsonObject meObj = JsonParser.parseString(meResp.body().string()).getAsJsonObject();
                        if (meObj.has("id")) myUserId = meObj.get("id").getAsString();
                    }
                } catch (Exception ignored) {}

                // 1. Lấy 5 bài viết mới nhất trên Threads
                String postsUrl = "https://graph.threads.net/v1.0/me/threads?fields=id,text&limit=5&access_token=" + Uri.encode(cleanToken);
                Request postsReq = new Request.Builder().url(postsUrl).get().build();
                try (Response postsResp = client.newCall(postsReq).execute()) {
                    if (!postsResp.isSuccessful() || postsResp.body() == null) {
                        String err = postsResp.body() != null ? postsResp.body().string() : "HTTP " + postsResp.code();
                        if (callback != null) mainHandler.post(() -> callback.onResult(0, "Không đọc được danh sách bài Threads: " + err));
                        return;
                    }

                    String postsJson = postsResp.body().string();
                    JsonObject postsRoot = JsonParser.parseString(postsJson).getAsJsonObject();
                    if (!postsRoot.has("data")) {
                        if (callback != null) mainHandler.post(() -> callback.onResult(0, "Không tìm thấy bài đăng nào trên Threads."));
                        return;
                    }

                    JsonArray postsData = postsRoot.getAsJsonArray("data");
                    DatabaseHelper db = DatabaseHelper.getInstance(appContext);
                    List<CommentItem> existing = db.getAllComments();
                    AIAgentOrchestrator agentOrchestrator = new AIAgentOrchestrator(appContext);

                    int addedCount = 0;
                    for (int i = 0; i < postsData.size(); i++) {
                        JsonObject postObj = postsData.get(i).getAsJsonObject();
                        String postId = postObj.get("id").getAsString();
                        String fullPostText = postObj.has("text") ? postObj.get("text").getAsString() : "Bài đăng Threads";
                        String shortPostTitle = fullPostText.length() > 45 ? fullPostText.substring(0, 42) + "..." : fullPostText;

                        String repliesUrl = "https://graph.threads.net/v1.0/" + postId + "/replies?fields=id,text,username,timestamp&access_token=" + Uri.encode(cleanToken);
                        Request repReq = new Request.Builder().url(repliesUrl).get().build();
                        try (Response repResp = client.newCall(repReq).execute()) {
                            if (repResp.isSuccessful() && repResp.body() != null) {
                                JsonObject repRoot = JsonParser.parseString(repResp.body().string()).getAsJsonObject();
                                if (repRoot.has("data")) {
                                    JsonArray repData = repRoot.getAsJsonArray("data");
                                    for (int j = 0; j < repData.size(); j++) {
                                        JsonObject r = repData.get(j).getAsJsonObject();
                                        String replyId = r.get("id").getAsString();
                                        String replyText = r.has("text") ? r.get("text").getAsString().trim() : "";
                                        String username = r.has("username") ? r.get("username").getAsString() : "threads_user";

                                        if (replyText.isEmpty() || botPublishedReplyIds.contains(replyId)) {
                                            continue;
                                        }

                                        String tagId = "ID:" + replyId;
                                        boolean alreadyExists = false;
                                        for (CommentItem ex : existing) {
                                            if (ex.getTimeAgo() != null && ex.getTimeAgo().contains(tagId)) {
                                                alreadyExists = true;
                                                break;
                                            }
                                            // Không bao giờ tự trả lời lại câu trả lời của chính Bot
                                            if (ex.getReplyText() != null && ex.getReplyText().trim().equalsIgnoreCase(replyText)) {
                                                alreadyExists = true;
                                                break;
                                            }
                                        }
                                        if (alreadyExists) continue;

                                        // Kiểm tra an toàn nội dung F6 (Tiêu cực vs An toàn)
                                        String lower = replyText.toLowerCase();
                                        boolean isNeg = lower.contains("tệ") || lower.contains("dở") || lower.contains("lừa")
                                                || lower.contains("chán") || lower.contains("kém") || lower.contains("nhảm")
                                                || lower.contains("scam") || lower.contains("ghét") || lower.contains("ngu")
                                                || lower.contains("rác");

                                        if (isNeg) {
                                            // Nhóm Tiêu cực: Chặn tự động trả lời (F6 Safety Rule)
                                            String riskWarn = "CẢNH BÁO AN TOÀN F6: Bình luận thật trên Threads có dấu hiệu tiêu cực. Agent đã khóa tự động trả lời!";
                                            CommentItem negComment = new CommentItem(
                                                    0,
                                                    SocialPlatform.FACEBOOK,
                                                    "[Threads] " + shortPostTitle,
                                                    "@" + username,
                                                    username.substring(0, 1).toUpperCase(),
                                                    replyText,
                                                    "Threads • " + tagId,
                                                    CommentItem.CATEGORY_NEGATIVE,
                                                    false,
                                                    null,
                                                    null,
                                                    true,
                                                    riskWarn
                                            );
                                            db.insertComment(negComment);
                                            existing.add(negComment);
                                            NotificationHelper.showNotification(
                                                    appContext,
                                                    "🛡️ [CẢNH BÁO F6] Bình luận tiêu cực trên Threads",
                                                    "@" + username + ": \"" + replyText + "\"\n⚠️ Agent đã chặn tự động trả lời. Nhấn để duyệt tay!"
                                            );
                                            addedCount++;
                                        } else {
                                            // Nhóm An toàn: Gọi AI Agent sinh câu trả lời & tự động bắn lên Threads!
                                            String category = (lower.contains("?") || lower.contains("sao") || lower.contains("như nào")
                                                    || lower.contains("ntn") || lower.contains("giá") || lower.contains("bao nhiêu") || lower.contains("hỏi"))
                                                    ? CommentItem.CATEGORY_QUESTION
                                                    : CommentItem.CATEGORY_PRAISE;

                                            String aiReply = agentOrchestrator.generateSmartCommentReplySync(fullPostText, username, replyText);
                                            String publishedReplyId = replyToThreadsCommentSync(myUserId, cleanToken, replyId, aiReply);
                                            boolean repliedLive = publishedReplyId != null;

                                            if (repliedLive) {
                                                botPublishedReplyIds.add(publishedReplyId);
                                            }

                                            CommentItem safeComment = new CommentItem(
                                                    0,
                                                    SocialPlatform.FACEBOOK,
                                                    "[Threads] " + shortPostTitle,
                                                    "@" + username,
                                                    username.substring(0, 1).toUpperCase(),
                                                    replyText,
                                                    "Threads • " + tagId,
                                                    category,
                                                    repliedLive,
                                                    repliedLive ? aiReply : null,
                                                    aiReply,
                                                    false,
                                                    null
                                            );
                                            db.insertComment(safeComment);
                                            existing.add(safeComment);

                                            final String finalUser = username;
                                            final String finalReply = aiReply;
                                            if (repliedLive) {
                                                NotificationHelper.showNotification(
                                                        appContext,
                                                        "🤖 [AI Agent] Đã tự động trả lời @" + username + " trên Threads",
                                                        "Khách: \"" + replyText + "\"\n👉 Agent: \"" + aiReply + "\""
                                                );
                                                mainHandler.post(() -> Toast.makeText(appContext,
                                                        "🤖 Agent vừa tự động trả lời comment của @" + finalUser + " trên Threads!",
                                                        Toast.LENGTH_LONG).show());
                                            } else {
                                                NotificationHelper.showCommentNotification(appContext, safeComment);
                                            }
                                            addedCount++;
                                        }
                                    }
                                }
                            } else if (repResp.code() == 400 || repResp.code() == 403) {
                                String errBody = repResp.body() != null ? repResp.body().string() : "";
                                if (errBody.contains("permission") || errBody.contains("Unsupported get request")) {
                                    String permMsg = "⚠️ Để Agent đọc được comment thật trên Threads, vào Meta Developer -> Tùy chỉnh -> Bấm '+ Thêm' quyền 'threads_read_replies' và 'threads_manage_replies' rồi tạo lại Token nhé!";
                                    if (!hasShownPermissionHint) {
                                        hasShownPermissionHint = true;
                                        mainHandler.post(() -> Toast.makeText(appContext, permMsg, Toast.LENGTH_LONG).show());
                                    }
                                    if (callback != null) mainHandler.post(() -> callback.onResult(0, permMsg));
                                    return;
                                }
                            }
                        }
                    }

                    final int finalAdded = addedCount;
                    if (callback != null) {
                        mainHandler.post(() -> callback.onResult(finalAdded, finalAdded > 0
                                ? "🤖 Đã phát hiện & xử lý tự động " + finalAdded + " bình luận mới trên Threads!"
                                : "Đã quét Threads: Chưa có bình luận mới nào (Bộ quét tự động 12s đang bật)."));
                    }
                }
            } catch (Exception e) {
                if (callback != null) mainHandler.post(() -> callback.onResult(0, "Lỗi đồng bộ comment Threads: " + e.getMessage()));
            } finally {
                isScanningInProgress = false;
            }
        }).start();
    }

    /**
     * Gửi trả lời bình luận đồng bộ (Synchronous) dành cho AI Agent chạy ngầm mỗi 12s
     */
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
                        return pubObj.has("id") ? pubObj.get("id").getAsString() : creationId;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
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
}
