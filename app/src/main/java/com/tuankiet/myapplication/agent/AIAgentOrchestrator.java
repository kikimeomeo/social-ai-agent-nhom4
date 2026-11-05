package com.tuankiet.myapplication.agent;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tuankiet.myapplication.data.DatabaseHelper;
import com.tuankiet.myapplication.data.SecurePreferenceManager;
import com.tuankiet.myapplication.model.AgentLog;
import com.tuankiet.myapplication.model.LLMConfig;
import com.tuankiet.myapplication.model.Post;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIAgentOrchestrator {

    public interface AgentCallback {
        void onStepChanged(int stepIndex, String stepTitle, String agentName);
        void onStreamFacebookChunk(String fullCurrentText);
        void onStreamLinkedInChunk(String fullCurrentText);
        void onCompleted(Post post);
        void onError(String message);
    }

    public interface ValidationCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface RewriteCallback {
        void onSuccess(String rewrittenText);
        void onError(String error);
    }

    public static class SafetyCheckResult {
        public final boolean hasWarning;
        public final String warningMessage;

        public SafetyCheckResult(boolean hasWarning, String warningMessage) {
            this.hasWarning = hasWarning;
            this.warningMessage = warningMessage;
        }
    }

    private final Context context;
    private final Handler mainHandler;
    private final ExecutorService executorService;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private boolean isCancelled = false;

    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    public AIAgentOrchestrator(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executorService = Executors.newFixedThreadPool(2);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    public void cancel() {
        isCancelled = true;
    }

    public void generatePost(String topic, String tone, boolean targetFb, boolean targetLi, AgentCallback callback) {
        isCancelled = false;
        executorService.execute(() -> {
            try {
                SecurePreferenceManager prefManager = SecurePreferenceManager.getInstance(context);
                LLMConfig config = prefManager.getConfig();
                boolean useRealApi = !config.isDemoMode() && config.getApiKey() != null && !config.getApiKey().trim().isEmpty();

                // Step 1: Orchestrator Synthesizes Brief
                notifyStep(callback, 1, "Orchestrator đang lập brief chiến lược...", "Master Orchestrator");
                Thread.sleep(600);
                if (isCancelled) return;

                String brief = "Brief: Topic '" + topic + "' phong cách '" + tone + "'. Kênh: " + (targetFb ? "Facebook " : "") + (targetLi ? "LinkedIn" : "");
                logStep("Orchestrator Synthesis", "Master Orchestrator", "Topic: " + topic + ", Tone: " + tone, brief, 350, 0.0014);

                String fbFull = "";
                String liFull = "";

                // Step 2: Facebook Sub-agent Stream
                if (targetFb) {
                    notifyStep(callback, 2, "Facebook Sub-agent đang viết bài & tạo hook...", "Facebook Agent");
                    if (useRealApi) {
                        try {
                            String prompt = "Viết bài Facebook Page về chủ đề: " + topic + ". Phong cách: " + tone +
                                    ". Yêu cầu: Có hook mở đầu bắt mắt, emoji sinh động, danh sách luận điểm ngắn gọn, câu hỏi thảo luận kêu gọi tương tác và hashtags phù hợp.";
                            fbFull = callLlmApi(config, prompt, "Bạn là chuyên gia sáng tạo nội dung Facebook Page chuyên nghiệp.");
                        } catch (Exception ex) {
                            // Fallback to local draft
                            fbFull = generateFacebookDraft(topic, tone);
                        }
                    } else {
                        fbFull = generateFacebookDraft(topic, tone);
                    }

                    streamText(fbFull, callback, true);
                    if (isCancelled) return;
                    logStep("Facebook Sub-agent", "Facebook Specialist", "Generate FB post from brief", fbFull.substring(0, Math.min(80, fbFull.length())) + "...", 580, 0.0023);
                }

                // Step 3: LinkedIn Sub-agent Stream
                if (targetLi) {
                    notifyStep(callback, 3, "LinkedIn Sub-agent đang tối ưu hóa B2B insights...", "LinkedIn Agent");
                    if (useRealApi) {
                        try {
                            String prompt = "Write a high-impact LinkedIn thought leadership post about: " + topic +
                                    ". Tone: " + tone + ". Format: Strong opening hook, 3 key architectural/strategic takeaways with bullet points, conclusion calling for professional discussion, relevant hashtags.";
                            liFull = callLlmApi(config, prompt, "You are a senior tech leader and LinkedIn thought leadership strategist.");
                        } catch (Exception ex) {
                            liFull = generateLinkedInDraft(topic, tone);
                        }
                    } else {
                        liFull = generateLinkedInDraft(topic, tone);
                    }

                    streamText(liFull, callback, false);
                    if (isCancelled) return;
                    logStep("LinkedIn Sub-agent", "LinkedIn Specialist", "Generate LI thought leadership", liFull.substring(0, Math.min(80, liFull.length())) + "...", 720, 0.0029);
                }

                // Step 4: Visual Agent & Packaging
                notifyStep(callback, 4, "Visual Agent đang tạo prompt & dựng ảnh minh họa...", "Visual Agent");
                Thread.sleep(500);
                if (isCancelled) return;

                String imagePrompt = "Modern minimal aesthetic 3D tech illustration for: " + topic + ", 4k cinematic lighting, vibrant indigo & cyan accents";
                logStep("Visual Agent", "Visual Designer", imagePrompt, "Generated vector placeholder graphic asset.", 240, 0.001);

                // Record token metrics in SecurePreferenceManager
                int totalNewTokens = (targetFb ? 580 : 0) + (targetLi ? 720 : 0) + 350 + 240;
                double totalNewCost = totalNewTokens * 0.000004; // ~$0.004 per 1k tokens
                prefManager.addTokens(totalNewTokens, totalNewCost);

                // Build Final Post
                Post post = new Post();
                post.setTopic(topic);
                post.setTone(tone);
                post.setContentFacebook(targetFb ? fbFull : "");
                post.setContentLinkedIn(targetLi ? liFull : "");
                post.setTargetFacebook(targetFb);
                post.setTargetLinkedIn(targetLi);
                post.setStatus(Post.STATUS_DRAFT);
                post.setImageUrl("https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe");
                post.setImagePrompt(imagePrompt);
                post.setCreatedAt(System.currentTimeMillis());

                mainHandler.post(() -> callback.onCompleted(post));

            } catch (InterruptedException e) {
                mainHandler.post(() -> callback.onError("Quá trình sinh bài viết đã bị hủy."));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Lỗi AI Pipeline: " + e.getMessage()));
            }
        });
    }

    public void retryChannel(Post currentPost, boolean isFacebook, AgentCallback callback) {
        isCancelled = false;
        executorService.execute(() -> {
            try {
                if (isFacebook) {
                    notifyStep(callback, 2, "Facebook Sub-agent đang viết lại bản mới...", "Facebook Agent");
                    String newFb = generateFacebookDraft(currentPost.getTopic(), currentPost.getTone());
                    streamText(newFb, callback, true);
                    currentPost.setContentFacebook(newFb);
                } else {
                    notifyStep(callback, 3, "LinkedIn Sub-agent đang tái cấu trúc bài viết...", "LinkedIn Agent");
                    String newLi = generateLinkedInDraft(currentPost.getTopic(), currentPost.getTone());
                    streamText(newLi, callback, false);
                    currentPost.setContentLinkedIn(newLi);
                }
                mainHandler.post(() -> callback.onCompleted(currentPost));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Không thể chạy lại: " + e.getMessage()));
            }
        });
    }

    // --- REWRITE TOOLS ("Ngắn hơn", "Trang trọng hơn", "Đổi Hook") ---
    public void rewriteSection(String currentText, String instructionType, RewriteCallback callback) {
        executorService.execute(() -> {
            try {
                Thread.sleep(600); // UI visual feedback
                String result;
                if ("SHORTER".equalsIgnoreCase(instructionType)) {
                    // Make concise: pick first 2-3 lines and summarize
                    String[] lines = currentText.split("\n");
                    StringBuilder sb = new StringBuilder();
                    int count = 0;
                    for (String line : lines) {
                        if (!line.trim().isEmpty() && count < 3) {
                            sb.append(line.trim()).append("\n\n");
                            count++;
                        }
                    }
                    sb.append("👉 Tóm tắt: Tối ưu hóa thời gian và gia tăng hiệu suất với AI! #SocialAI");
                    result = sb.toString();
                } else if ("FORMAL".equalsIgnoreCase(instructionType)) {
                    // Make formal: executive framing
                    result = "Kính gửi quý độc giả và đồng nghiệp,\n\n" +
                            "Chúng tôi xin trân trọng chia sẻ phân tích chiến lược liên quan đến các chuyển dịch gần đây trong ngành:\n\n" +
                            currentText.replace("🔥", "•").replace("😂", "").replace("chém gió", "thảo luận") +
                            "\n\nTrân trọng cảm ơn sự theo dõi của quý vị.";
                } else if ("HOOK".equalsIgnoreCase(instructionType)) {
                    // Change opening hook
                    String body = currentText.contains("\n") ? currentText.substring(currentText.indexOf("\n")) : currentText;
                    result = "🚨 DỪNG LƯỚT 30 GIÂY: Đây là phát hiện quan trọng nhất tuần này mà các kỹ sư phần mềm không nên bỏ lỡ!" + body;
                } else {
                    result = currentText;
                }

                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Lỗi khi viết lại: " + e.getMessage()));
            }
        });
    }

    // --- AI AGENT AUTO-REPLY TO SOCIAL COMMENTS (F6) ---
    public String generateSmartCommentReplySync(String postText, String commenterUsername, String commentText) {
        try {
            SecurePreferenceManager prefManager = SecurePreferenceManager.getInstance(context);
            LLMConfig config = prefManager.getConfig();
            boolean useRealApi = !config.isDemoMode() && config.getApiKey() != null && !config.getApiKey().trim().isEmpty();

            if (useRealApi) {
                String systemPrompt = "Bạn là AI Agent quản trị kênh mạng xã hội Threads chuyên nghiệp, thân thiện. " +
                        "Hãy viết câu trả lời ngắn gọn (1-2 câu, dưới 250 ký tự), tự nhiên, lịch sự bằng tiếng Việt để phản hồi bình luận của độc giả. " +
                        "Chỉ trả về đúng nội dung câu trả lời, không để trong ngoặc kép.";
                String prompt = "Bài viết gốc: \"" + postText + "\"\n" +
                        "Người bình luận @" + commenterUsername + " viết: \"" + commentText + "\"\n" +
                        "Hãy viết câu trả lời phù hợp nhất:";
                String aiReply = callLlmApi(config, prompt, systemPrompt).trim();
                if (aiReply.startsWith("\"") && aiReply.endsWith("\"") && aiReply.length() > 2) {
                    aiReply = aiReply.substring(1, aiReply.length() - 1).trim();
                }
                if (aiReply.length() > 450) {
                    aiReply = aiReply.substring(0, 445) + "...";
                }
                logStep("Auto-Reply Sub-agent", "Threads Comment Agent", "Comment: " + commentText, aiReply, 180, 0.0005);
                prefManager.addTokens(180, 0.0005);
                return aiReply;
            }
        } catch (Exception ignored) {
            // Fallback below if quota/network error
        }

        String lower = commentText.toLowerCase();
        if (lower.contains("?") || lower.contains("sao") || lower.contains("như nào") || lower.contains("ntn") || lower.contains("giá") || lower.contains("bao nhiêu")) {
            return "Chào @" + commenterUsername + ", cảm ơn câu hỏi rất hay của bạn! Mình đã ghi nhận và xin gửi thông tin giải đáp chi tiết ngay nhé ✨";
        }
        return "Cảm ơn @" + commenterUsername + " rất nhiều vì đã quan tâm và để lại bình luận tương tác trên kênh nhé! 🙌✨";
    }

    // --- CONTENT SAFETY & SENSITIVE WORD CHECKER ---
    public SafetyCheckResult checkContentSafety(String content) {
        if (content == null || content.isEmpty()) {
            return new SafetyCheckResult(false, null);
        }

        String lower = content.toLowerCase();
        if (lower.contains("cam kết 100%") || lower.contains("làm giàu cấp tốc") || lower.contains("chắc chắn kiếm") || lower.contains("hoa hồng khủng")) {
            return new SafetyCheckResult(true, "CẢNH BÁO NỘI DUNG: Phát hiện cụm từ cam kết lợi nhuận quá mức có thể vi phạm tiêu chuẩn cộng đồng của Facebook & LinkedIn.");
        }

        if ((lower.contains("theo thống kê") || lower.contains("số liệu chứng minh") || lower.contains("theo nghiên cứu"))
                && !lower.contains("nguồn") && !lower.contains("trích")) {
            return new SafetyCheckResult(true, "LƯU Ý: Bài viết có đề cập đến thống kê/nghiên cứu nhưng chưa dẫn nguồn tham chiếu cụ thể.");
        }

        return new SafetyCheckResult(false, null);
    }

    // --- VALIDATE API KEY METHOD ---
    public void validateApiKey(String provider, String apiKey, ValidationCallback callback) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            mainHandler.post(() -> callback.onError("Vui lòng nhập API Key trước khi kiểm tra!"));
            return;
        }

        final String cleanKey = apiKey.trim().replace("\"", "").replace("'", "").replace("\n", "").replace("\r", "");

        executorService.execute(() -> {
            try {
                if (LLMConfig.PROVIDER_GEMINI.equalsIgnoreCase(provider)) {
                    String[] modelsToTry = {"gemini-3.1-flash-lite", "gemini-3.5-flash-lite", "gemini-3.8-flash", "gemini-2.5-flash-lite", "gemini-2.0-flash", "gemini-1.5-flash"};
                    boolean validated = false;
                    String lastError = "Không thể kết nối đến Google Gemini";

                    for (String model : modelsToTry) {
                        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + cleanKey;
                        JsonObject root = new JsonObject();
                        JsonArray contents = new JsonArray();
                        JsonObject item = new JsonObject();
                        JsonArray parts = new JsonArray();
                        JsonObject textObj = new JsonObject();
                        textObj.addProperty("text", "Hi");
                        parts.add(textObj);
                        item.add("parts", parts);
                        contents.add(item);
                        root.add("contents", contents);

                        Request req = new Request.Builder()
                                .url(url)
                                .post(RequestBody.create(root.toString(), JSON_MEDIA))
                                .build();

                        try (Response resp = httpClient.newCall(req).execute()) {
                            if (resp.isSuccessful()) {
                                validated = true;
                                String modelDisplayName = "gemini-3.1-flash-lite".equals(model) ? "Gemini 3.1 Flash Lite (500 lượt/ngày)" : model;
                                mainHandler.post(() -> callback.onSuccess("Google Gemini API Key hoàn toàn hợp lệ!\n(Đã kết nối: " + modelDisplayName + ")"));
                                break;
                            } else {
                                if (resp.body() != null) {
                                    try {
                                        String bodyStr = resp.body().string();
                                        JsonObject errJson = JsonParser.parseString(bodyStr).getAsJsonObject();
                                        if (errJson.has("error") && errJson.getAsJsonObject("error").has("message")) {
                                            lastError = errJson.getAsJsonObject("error").get("message").getAsString();
                                        }
                                    } catch (Exception ignored) {}
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    if (!validated) {
                        final String finalErr = lastError;
                        mainHandler.post(() -> callback.onError("Google từ chối key: " + finalErr));
                    }
                } else { // OpenAI or OpenRouter
                    String url = LLMConfig.PROVIDER_OPENROUTER.equalsIgnoreCase(provider) ?
                            "https://openrouter.ai/api/v1/chat/completions" :
                            "https://api.openai.com/v1/chat/completions";

                    JsonObject root = new JsonObject();
                    root.addProperty("model", "gpt-4o-mini");
                    JsonArray msgs = new JsonArray();
                    JsonObject m = new JsonObject();
                    m.addProperty("role", "user");
                    m.addProperty("content", "ping");
                    msgs.add(m);
                    root.add("messages", msgs);

                    Request req = new Request.Builder()
                            .url(url)
                            .header("Authorization", "Bearer " + cleanKey)
                            .post(RequestBody.create(root.toString(), JSON_MEDIA))
                            .build();

                    try (Response resp = httpClient.newCall(req).execute()) {
                        if (resp.isSuccessful()) {
                            mainHandler.post(() -> callback.onSuccess(provider + " API Key hợp lệ!"));
                        } else {
                            mainHandler.post(() -> callback.onError("Key không hợp lệ (HTTP " + resp.code() + "). Vui lòng kiểm tra lại!"));
                        }
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Lỗi kết nối kiểm tra: " + e.getMessage()));
            }
        });
    }

    // --- REAL API CALL VIA OKHTTP ---
    private String callLlmApi(LLMConfig config, String prompt, String systemPrompt) throws IOException {
        String cleanApiKey = config.getApiKey() != null ? config.getApiKey().trim().replace("\"", "").replace("'", "") : "";
        if (LLMConfig.PROVIDER_GEMINI.equalsIgnoreCase(config.getProvider())) {
            String[] modelsToTry = {"gemini-3.1-flash-lite", "gemini-3.5-flash-lite", "gemini-3.8-flash", "gemini-2.5-flash-lite", "gemini-2.0-flash", "gemini-1.5-flash"};
            IOException lastEx = null;

            for (String model : modelsToTry) {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + cleanApiKey;

                JsonObject root = new JsonObject();
                JsonArray contents = new JsonArray();
                JsonObject item = new JsonObject();
                JsonArray parts = new JsonArray();
                JsonObject textObj = new JsonObject();
                textObj.addProperty("text", systemPrompt + "\n\n" + prompt);
                parts.add(textObj);
                item.add("parts", parts);
                contents.add(item);
                root.add("contents", contents);

                Request req = new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(root.toString(), JSON_MEDIA))
                        .build();

                try (Response resp = httpClient.newCall(req).execute()) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        String json = resp.body().string();
                        JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();
                        return parsed.getAsJsonArray("candidates")
                                .get(0).getAsJsonObject()
                                .getAsJsonObject("content")
                                .getAsJsonArray("parts")
                                .get(0).getAsJsonObject()
                                .get("text").getAsString();
                    } else {
                        lastEx = new IOException("Model " + model + " error: HTTP " + resp.code());
                    }
                } catch (IOException e) {
                    lastEx = e;
                }
            }

            if (lastEx != null) throw lastEx;
            throw new IOException("Không thể kết nối đến các model Gemini khả dụng.");
        } else { // OpenAI or OpenRouter
            String url = LLMConfig.PROVIDER_OPENROUTER.equalsIgnoreCase(config.getProvider()) ?
                    "https://openrouter.ai/api/v1/chat/completions" :
                    "https://api.openai.com/v1/chat/completions";

            JsonObject root = new JsonObject();
            root.addProperty("model", "gpt-4o-mini");
            JsonArray msgs = new JsonArray();

            JsonObject sys = new JsonObject();
            sys.addProperty("role", "system");
            sys.addProperty("content", systemPrompt);
            msgs.add(sys);

            JsonObject usr = new JsonObject();
            usr.addProperty("role", "user");
            usr.addProperty("content", prompt);
            msgs.add(usr);

            root.add("messages", msgs);

            Request req = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + config.getApiKey().trim())
                    .post(RequestBody.create(root.toString(), JSON_MEDIA))
                    .build();

            try (Response resp = httpClient.newCall(req).execute()) {
                if (resp.isSuccessful() && resp.body() != null) {
                    String json = resp.body().string();
                    JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();
                    return parsed.getAsJsonArray("choices")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString();
                } else {
                    throw new IOException("OpenAI API error code: " + resp.code());
                }
            }
        }
    }

    private void streamText(String fullText, AgentCallback callback, boolean isFacebook) throws InterruptedException {
        String[] words = fullText.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (isCancelled) return;
            current.append(word).append(" ");
            final String textToEmit = current.toString();
            mainHandler.post(() -> {
                if (isFacebook) {
                    callback.onStreamFacebookChunk(textToEmit);
                } else {
                    callback.onStreamLinkedInChunk(textToEmit);
                }
            });
            Thread.sleep(30);
        }
    }

    private void notifyStep(AgentCallback callback, int stepIndex, String title, String agent) {
        mainHandler.post(() -> callback.onStepChanged(stepIndex, title, agent));
    }

    private void logStep(String step, String agent, String prompt, String output, int tokens, double cost) {
        AgentLog log = new AgentLog(0, System.currentTimeMillis(), step, agent, prompt, output, tokens, cost, 600);
        DatabaseHelper.getInstance(context).insertLog(log);
    }

    private String generateFacebookDraft(String topic, String tone) {
        if ("Hài hước".equalsIgnoreCase(tone)) {
            return "🔥 Tin chuẩn chưa anh em ơi?! Về chủ đề: " + topic + " 🚀\n\n" +
                    "Nhiều người bảo làm cái này khó lắm, nhưng thực ra khó thật chứ không đùa! 😂\n\n" +
                    "📌 3 điểm mấu chốt bạn cần nhớ:\n" +
                    "1. Bắt đầu từ những bước nhỏ nhất nhưng duy trì đều đặn.\n" +
                    "2. Tận dụng AI để tự động hóa 80% công việc lặp lại.\n" +
                    "3. Đừng quên dành thời gian tương tác với cộng đồng!\n\n" +
                    "👉 Anh em thấy chuẩn thì thả 1 tim và comment ý kiến bên dưới nhé!\n#SocialAI #AI2026 #TechHumor #Viral";
        } else if ("Thân thiện".equalsIgnoreCase(tone)) {
            return "Chào cả nhà! 👋 Hôm nay mình muốn chia sẻ một góc nhìn rất thú vị về: " + topic + " ✨\n\n" +
                    "Thời gian qua khi nghiên cứu về mảng này, mình nhận ra một bài học rất quý giá:\n" +
                    "💡 Sự đơn giản và tính nhất quán luôn mang lại kết quả bền vững nhất.\n" +
                    "💡 Công nghệ sinh ra là để giải phóng con người khỏi những thao tác thủ công rườm rà.\n\n" +
                    "Bạn nghĩ sao về điều này? Hãy cùng để lại bình luận để chúng mình cùng trò chuyện nhé! Chúc mọi người một ngày làm việc tràn đầy năng lượng! 🌿\n#ChiaSe #SocialAI #TechCommunity";
        } else { // Chuyên gia
            return "🚀 PHÂN TÍCH CHUYÊN SÂU: " + topic.toUpperCase() + "\n\n" +
                    "Trong bối cảnh công nghệ chuyển dịch mạnh mẽ sang tự động hóa và on-device intelligence, đây là những luận điểm chiến lược cần lưu ý:\n\n" +
                    "1️⃣ Định vị cốt lõi: Tối ưu hóa chuỗi giá trị và cắt giảm độ trễ vận hành.\n" +
                    "2️⃣ Cơ chế thực thi: Kết hợp sức mạnh của Multi-Agent System để chuyên môn hóa từng khâu.\n" +
                    "3️⃣ Quản trị rủi ro: Luôn có chốt chặn an toàn (Human-in-the-loop) trước khi công bố nội dung.\n\n" +
                    "👉 Bạn đã áp dụng mô hình này vào thực tế chưa? Thảo luận cùng chúng tôi bên dưới.\n#ChuyenGiaAI #KienTrucHeThong #TechInsights2026 #SocialAI";
        }
    }

    private String generateLinkedInDraft(String topic, String tone) {
        return "Navigating the Shift: Strategic Perspective on " + topic + " 🌐\n\n" +
                "As technology landscapes evolve toward autonomous workflows and intelligent client-side systems, engineering leaders must reassess key architectural assumptions.\n\n" +
                "Key Takeaways for Teams & Founders:\n" +
                "• Modular Specialization: Deploying targeted sub-agents significantly outperforms monolithic pipelines in domain fidelity.\n" +
                "• Edge-First Resilience: Keeping critical state and queue management on-device ensures uninterrupted operations.\n" +
                "• Responsible AI Governance: Enforcing human verification gates guarantees brand safety and compliance.\n\n" +
                "How is your organization structuring its automated workflows this year? Looking forward to your insights in the comments.\n\n" +
                "#Leadership #ArtificialIntelligence #SoftwareEngineering #TechStrategy #FutureOfWork";
    }
}
