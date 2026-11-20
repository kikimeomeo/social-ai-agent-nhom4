package com.tuankiet.myapplication.agent;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tuankiet.myapplication.data.DatabaseHelper;
import com.tuankiet.myapplication.data.SecurePreferenceManager;
import com.tuankiet.myapplication.model.AgentLog;
import com.tuankiet.myapplication.model.LLMConfig;
import com.tuankiet.myapplication.model.Post;
import com.tuankiet.myapplication.model.SocialPlatform;
import com.tuankiet.myapplication.util.PostIllustrationGenerator;

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
        default void onStreamThreadsChunk(String fullCurrentText) {}
        void onCompleted(Post post);
        void onError(String message);
    }

    public interface ValidationCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface RewriteCallback {
        void onSuccess(String rewrittenText);
        default void onSuccessWithMeta(String rewrittenText, boolean usedRealLlm, String providerName) {
            onSuccess(rewrittenText);
        }
        void onError(String error);
    }

    public interface ImageGenerationCallback {
        void onSuccess(PostIllustrationGenerator.GeneratedIllustration illustration);
        void onError(String error);
    }

    private static class LlmResponse {
        final String text;
        final int totalTokens;

        LlmResponse(String text, int totalTokens) {
            this.text = text;
            this.totalTokens = totalTokens;
        }
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
    private volatile boolean isCancelled = false;
    private int lastCallTokens = 0;

    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    // Google Gemini API Endpoint & Model Constants (Issue #5)
    public static final String PRIMARY_GEMINI_MODEL = "gemini-3.1-flash-lite";
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String[] GEMINI_FALLBACK_MODELS = {
            PRIMARY_GEMINI_MODEL,
            "gemini-2.5-flash-lite",
            "gemini-2.0-flash",
            "gemini-1.5-flash"
    };
    private static final String[] GEMINI_IMAGE_MODELS = {
            "gemini-2.5-flash-image",
            "gemini-2.0-flash-exp-image-generation"
    };

    public static final String SYSTEM_PROMPT_VISUAL_AGENT =
            "You are a Creative Visual Director AI for social media illustrations (Facebook Page 16:9 and Meta Threads 1:1). " +
            "Your job is to output ONE concise, vivid, aesthetic English image-generation prompt with zero text or watermarks inside the image.";

    // Specialized System Prompts for Multi-Agent Architecture (Issue #5)
    public static final String SYSTEM_PROMPT_ORCHESTRATOR =
            "Bạn là Master Orchestrator trong hệ thống Multi-Agent sáng tạo nội dung mạng xã hội. " +
            "Nhiệm vụ của bạn là phân tích chủ đề, xác định thông điệp cốt lõi, góc tiếp cận và cấu trúc lập luận phù hợp cho từng nền tảng (Facebook Page & Threads).";

    public static final String SYSTEM_PROMPT_FACEBOOK_AGENT =
            "Bạn là Facebook Content Sub-Agent chuyên nghiệp, chuyên quản trị các Fanpage cộng đồng có tương tác cao tại Việt Nam.\n" +
            "Nguyên tắc viết bài Facebook:\n" +
            "1. Mở đầu bằng một câu Hook thu hút sự chú ý ngay 2 dòng đầu tiên.\n" +
            "2. Chia đoạn ngắn (2-3 câu/đoạn), sử dụng emoji hợp lý để tạo nhịp điệu thị giác.\n" +
            "3. Trình bày 3 luận điểm hoặc giá trị thực tiễn rõ ràng, dễ đọc trên thiết bị di động.\n" +
            "4. Kết bài bằng câu hỏi mở kích thích bình luận (CTA) và 3-5 hashtags tiếng Việt liên quan.";

    public static final String SYSTEM_PROMPT_THREADS_AGENT =
            "Bạn là Threads Sub-Agent chuyên nghiệp trên nền tảng Threads (Meta).\n" +
            "Đặc trưng văn phong Threads:\n" +
            "1. Viết ngắn gọn, súc tích (dưới 500 ký tự), giọng điệu chân thật (authentic), trò chuyện trực tiếp (conversational).\n" +
            "2. Đi thẳng vào vấn đề hoặc đưa ra một quan điểm/insight cá nhân sắc bén (hot take/unpopular opinion) mà không dài dòng văn vở.\n" +
            "3. Ngắt dòng thoáng mắt, dùng tối đa 1-2 emoji tinh tế, tránh nhồi nhét quá nhiều emoji.\n" +
            "4. Mở ra một luồng thảo luận cởi mở bằng câu hỏi kích thích mọi người để lại suy nghĩ/trải nghiệm bên dưới.";

    // Giữ alias tương thích nếu code cũ còn gọi
    public static final String SYSTEM_PROMPT_LINKEDIN_AGENT = SYSTEM_PROMPT_THREADS_AGENT;

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

    public String buildFacebookUserPrompt(String topic, String tone) {
        return buildFacebookUserPrompt(topic, tone, "Tiếng Việt");
    }

    public String buildFacebookUserPrompt(String topic, String tone, String language) {
        String langLabel = (language != null && !language.trim().isEmpty()) ? language : "Tiếng Việt";
        return "Hãy viết một bài đăng Facebook Page hoàn chỉnh bằng ngôn ngữ: " + langLabel + ".\n" +
                "- Chủ đề trọng tâm: " + topic + "\n" +
                "- Giọng văn (Tone of voice): " + tone + "\n" +
                "- Yêu cầu: Có hook mở đầu bắt mắt, emoji sinh động, danh sách luận điểm ngắn gọn, câu hỏi thảo luận kêu gọi tương tác và hashtags phù hợp. Không dùng markdown phức tạp.";
    }

    public String buildThreadsUserPrompt(String topic, String tone) {
        return buildThreadsUserPrompt(topic, tone, "Tiếng Việt");
    }

    public String buildThreadsUserPrompt(String topic, String tone, String language) {
        String langLabel = (language != null && !language.trim().isEmpty()) ? language : "Tiếng Việt";
        return "Hãy viết một bài đăng Threads (Meta) ngắn gọn, sắc bén bằng ngôn ngữ: " + langLabel + ".\n" +
                "- Chủ đề trọng tâm: " + topic + "\n" +
                "- Giọng văn: " + tone + "\n" +
                "- Yêu cầu: Dưới 500 ký tự, đi thẳng vào góc nhìn thực tế, chân thật, tự nhiên như chia sẻ cá nhân, đặt câu hỏi thảo luận tự nhiên ở cuối bài. Không dùng bullet points cứng nhắc.";
    }

    public String buildLinkedInUserPrompt(String topic, String tone) {
        return buildThreadsUserPrompt(topic, tone);
    }

    public void generatePost(String topic, String tone, boolean targetFb, boolean targetLi, AgentCallback callback) {
        generatePost(topic, tone, "Tiếng Việt", targetFb, false, targetLi, callback);
    }

    public void generatePost(String topic, String tone, String language, boolean targetFb, boolean targetThreads, boolean targetLi, AgentCallback callback) {
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

                String brief = "Brief: Topic '" + topic + "', Phong cách: '" + tone + "', Ngôn ngữ: '" + language + "'. Kênh: " +
                        (targetFb ? "Facebook " : "") + (targetThreads ? "Threads " : "") + (targetLi ? "LinkedIn" : "");
                logStep("Orchestrator Synthesis", "Master Orchestrator", "Topic: " + topic + ", Tone: " + tone + ", Lang: " + language, brief, 350, 0.0014);

                String fbFull = "";
                String threadsFull = "";
                String liFull = "";
                int fbTokens = 0;
                int threadsTokens = 0;
                int liTokens = 0;

                // Step 2: Facebook Sub-agent Stream
                if (targetFb) {
                    notifyStep(callback, 2, "Facebook Sub-agent đang viết bài & tạo hook...", "Facebook Agent");
                    String fbPrompt = buildFacebookUserPrompt(topic, tone, language);
                    if (useRealApi) {
                        try {
                            fbFull = callLlmApi(config, fbPrompt, SYSTEM_PROMPT_FACEBOOK_AGENT);
                            fbTokens = lastCallTokens > 0 ? lastCallTokens : 580;
                        } catch (Exception ex) {
                            // Fallback to local draft
                            fbFull = generateFacebookDraft(topic, tone, language);
                            fbTokens = 580;
                        }
                    } else {
                        fbFull = generateFacebookDraft(topic, tone, language);
                        fbTokens = 580;
                    }

                    streamText(fbFull, callback, true);
                    if (isCancelled) return;
                    logStep("Facebook Sub-agent", "Facebook Specialist", fbPrompt, fbFull.substring(0, Math.min(80, fbFull.length())) + "...", fbTokens, fbTokens * 0.000004);
                }

                // Step 3: Threads Sub-agent Stream
                if (targetThreads) {
                    notifyStep(callback, 3, "Threads Sub-agent đang tối ưu bài ngắn (≤ 500 ký tự)...", "Threads Agent");
                    String threadsPrompt = buildThreadsUserPrompt(topic, tone, language);
                    if (useRealApi) {
                        try {
                            threadsFull = callLlmApi(config, threadsPrompt, SYSTEM_PROMPT_THREADS_AGENT);
                            threadsTokens = lastCallTokens > 0 ? lastCallTokens : 420;
                        } catch (Exception ex) {
                            threadsFull = generateThreadsDraft(topic, tone, language);
                            threadsTokens = 420;
                        }
                    } else {
                        threadsFull = generateThreadsDraft(topic, tone, language);
                        threadsTokens = 420;
                    }

                    streamThreadsChunk(threadsFull, callback);
                    if (isCancelled) return;
                    logStep("Threads Sub-agent", "Threads Specialist", threadsPrompt, threadsFull.substring(0, Math.min(80, threadsFull.length())) + "...", threadsTokens, threadsTokens * 0.000004);
                }

                // Step 4: LinkedIn Sub-agent Stream (compat)
                if (targetLi) {
                    notifyStep(callback, 4, "LinkedIn Sub-agent đang tối ưu hóa B2B insights...", "LinkedIn Agent");
                    String liPrompt = buildLinkedInUserPrompt(topic, tone);
                    if (useRealApi) {
                        try {
                            liFull = callLlmApi(config, liPrompt, SYSTEM_PROMPT_LINKEDIN_AGENT);
                            liTokens = lastCallTokens > 0 ? lastCallTokens : 720;
                        } catch (Exception ex) {
                            liFull = generateLinkedInDraft(topic, tone, language);
                            liTokens = 720;
                        }
                    } else {
                        liFull = generateLinkedInDraft(topic, tone, language);
                        liTokens = 720;
                    }

                    streamText(liFull, callback, false);
                    if (isCancelled) return;
                    logStep("LinkedIn Sub-agent", "LinkedIn Strategist", liPrompt, liFull.substring(0, Math.min(80, liFull.length())) + "...", liTokens, liTokens * 0.000004);
                }

                // Step 5: Visual Agent & Packaging
                notifyStep(callback, 5, "Visual Agent đang tạo prompt & dựng ảnh minh họa...", "Visual Agent");
                if (isCancelled) return;

                SocialPlatform primaryVisualPlatform = targetFb
                        ? SocialPlatform.FACEBOOK
                        : (targetThreads ? SocialPlatform.THREADS : SocialPlatform.LINKEDIN);
                String primaryVisualContent = targetFb ? fbFull : (targetThreads ? threadsFull : liFull);
                PostIllustrationGenerator.GeneratedIllustration illustration = generateIllustrationSync(
                        topic, primaryVisualContent, primaryVisualPlatform, null, 0
                );
                if (isCancelled) return;

                // Record token metrics in SecurePreferenceManager
                int totalNewTokens = fbTokens + threadsTokens + liTokens + 350 + 240;
                double totalNewCost = totalNewTokens * 0.000004; // ~$0.004 per 1k tokens
                prefManager.addTokens(totalNewTokens, totalNewCost);

                // Build Final Post & dispatch to Main Thread
                // CHỐT CHẶN AN TOÀN (F4): Agent LUÔN đóng gói ở trạng thái DRAFT & userApproved = false,
                // tuyệt đối KHÔNG tự ý đăng bài khi người dùng chưa bấm xác nhận duyệt!
                Post post = new Post();
                post.setTopic(topic);
                post.setTone(tone);
                post.setLanguage(language);
                post.setContentFacebook(targetFb ? fbFull : "");
                post.setContentThreads(targetThreads ? threadsFull : "");
                post.setContentLinkedIn(targetLi ? liFull : "");
                post.setTargetFacebook(targetFb);
                post.setTargetThreads(targetThreads);
                post.setTargetLinkedIn(targetLi);
                post.setStatus(Post.STATUS_DRAFT);
                post.setUserApproved(false);
                post.setImageUrl(illustration.imageUri);
                post.setImagePrompt(illustration.imagePrompt);
                post.setImageVariantIndex(illustration.variantIndex);
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
        retryChannel(currentPost, isFacebook ? 0 : 2, callback);
    }

    public void retryChannel(Post currentPost, int platformIndex, AgentCallback callback) {
        isCancelled = false;
        executorService.execute(() -> {
            try {
                SecurePreferenceManager prefManager = SecurePreferenceManager.getInstance(context);
                LLMConfig config = prefManager.getConfig();
                boolean useRealApi = !config.isDemoMode() && config.getApiKey() != null && !config.getApiKey().trim().isEmpty();

                if (platformIndex == 0) {
                    notifyStep(callback, 2, "Facebook Sub-agent đang viết lại bản mới...", "Facebook Agent");
                    String newFb;
                    if (useRealApi) {
                        try {
                            newFb = callLlmApi(config, buildFacebookUserPrompt(currentPost.getTopic(), currentPost.getTone(), currentPost.getLanguage()), SYSTEM_PROMPT_FACEBOOK_AGENT);
                        } catch (Exception ex) {
                            newFb = generateFacebookDraft(currentPost.getTopic(), currentPost.getTone(), currentPost.getLanguage());
                        }
                    } else {
                        newFb = generateFacebookDraft(currentPost.getTopic(), currentPost.getTone(), currentPost.getLanguage());
                    }
                    streamText(newFb, callback, true);
                    currentPost.setContentFacebook(newFb);
                } else if (platformIndex == 1) {
                    notifyStep(callback, 3, "Threads Sub-agent đang viết lại bản ngắn mới...", "Threads Agent");
                    String newThreads;
                    if (useRealApi) {
                        try {
                            newThreads = callLlmApi(config, buildThreadsUserPrompt(currentPost.getTopic(), currentPost.getTone(), currentPost.getLanguage()), SYSTEM_PROMPT_THREADS_AGENT);
                        } catch (Exception ex) {
                            newThreads = generateThreadsDraft(currentPost.getTopic(), currentPost.getTone(), currentPost.getLanguage());
                        }
                    } else {
                        newThreads = generateThreadsDraft(currentPost.getTopic(), currentPost.getTone(), currentPost.getLanguage());
                    }
                    streamThreadsChunk(newThreads, callback);
                    currentPost.setContentThreads(newThreads);
                } else {
                    notifyStep(callback, 4, "LinkedIn Sub-agent đang tái cấu trúc bài viết...", "LinkedIn Agent");
                    String newLi;
                    if (useRealApi) {
                        try {
                            newLi = callLlmApi(config, buildLinkedInUserPrompt(currentPost.getTopic(), currentPost.getTone()), SYSTEM_PROMPT_LINKEDIN_AGENT);
                        } catch (Exception ex) {
                            newLi = generateLinkedInDraft(currentPost.getTopic(), currentPost.getTone(), currentPost.getLanguage());
                        }
                    } else {
                        newLi = generateLinkedInDraft(currentPost.getTopic(), currentPost.getTone(), currentPost.getLanguage());
                    }
                    streamText(newLi, callback, false);
                    currentPost.setContentLinkedIn(newLi);
                }
                currentPost.setStatus(Post.STATUS_DRAFT);
                currentPost.setUserApproved(false);
                mainHandler.post(() -> callback.onCompleted(currentPost));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Không thể chạy lại: " + e.getMessage()));
            }
        });
    }

    // --- REWRITE TOOLS ("Ngắn hơn", "Trang trọng hơn", "Đổi Hook mở đầu") ---
    public void rewriteSection(String currentText, String instructionType, RewriteCallback callback) {
        rewriteSection(currentText, null, instructionType, SocialPlatform.FACEBOOK, "Tiếng Việt", callback);
    }

    public void rewriteSection(String fullText,
                               String selectedSection,
                               String instructionType,
                               SocialPlatform platform,
                               String language,
                               RewriteCallback callback) {
        if (callback == null) return;
        String safeFull = fullText != null ? fullText.trim() : "";
        if (safeFull.isEmpty()) {
            mainHandler.post(() -> callback.onError("Nội dung đang trống, vui lòng nhập nội dung trước khi yêu cầu AI viết lại!"));
            return;
        }

        final SocialPlatform targetPlatform = platform != null ? platform : SocialPlatform.FACEBOOK;
        final String safeSelection = (selectedSection != null && !selectedSection.trim().isEmpty())
                ? selectedSection.trim()
                : null;
        final String actionLabel = PostReviewPolicy.getInstructionDisplayName(instructionType);

        executorService.execute(() -> {
            long startMs = System.currentTimeMillis();
            try {
                SecurePreferenceManager prefManager = SecurePreferenceManager.getInstance(context);
                LLMConfig config = prefManager.getConfig();
                boolean isDemo = config.isDemoMode();
                String apiKey = config.getApiKey() != null ? config.getApiKey().trim() : "";

                if (!isDemo) {
                    if (apiKey.isEmpty()) {
                        mainHandler.post(() -> callback.onError(
                                "Chưa cấu hình API Key LLM! Vui lòng vào Cài đặt để nhập API Key (" + config.getProvider() + ") hoặc bật Chế độ Demo."
                        ));
                        return;
                    }

                    String systemPrompt = PostReviewPolicy.buildRewriteSystemPrompt(targetPlatform);
                    String userPrompt = PostReviewPolicy.buildRewriteUserPrompt(
                            safeFull, safeSelection, instructionType, targetPlatform, language
                    );

                    LlmResponse response = callLlmApiWithMeta(config, userPrompt, systemPrompt);
                    String rewritten = PostReviewPolicy.sanitizeLlmRewriteOutput(
                            response.text, targetPlatform, instructionType
                    );
                    if (rewritten.isEmpty()) {
                        mainHandler.post(() -> callback.onError("LLM không trả về nội dung hợp lệ. Vui lòng thử lại!"));
                        return;
                    }

                    int tokens = response.totalTokens > 0 ? response.totalTokens : 280;
                    double cost = tokens * 0.000004;
                    long durationMs = Math.max(1L, System.currentTimeMillis() - startMs);
                    logStepWithDuration(
                            "AI Rewrite: " + actionLabel,
                            "Copy Editor Agent (" + config.getProvider() + ")",
                            userPrompt,
                            rewritten.substring(0, Math.min(120, rewritten.length())),
                            tokens,
                            cost,
                            durationMs
                    );
                    prefManager.addTokens(tokens, cost);

                    final String finalOutput = rewritten;
                    final String provider = config.getProvider();
                    mainHandler.post(() -> callback.onSuccessWithMeta(finalOutput, true, provider));
                    return;
                }

                // Demo Mode: deterministic smart rewrite (respects Threads <= 500 chars)
                Thread.sleep(450);
                String targetInput = (safeSelection != null && !PostReviewPolicy.REWRITE_HOOK.equalsIgnoreCase(instructionType))
                        ? safeSelection
                        : safeFull;
                String demoResult = PostReviewPolicy.rewriteLocallyForDemo(
                        targetInput, instructionType, targetPlatform, language
                );
                long durationMs = Math.max(1L, System.currentTimeMillis() - startMs);
                logStepWithDuration(
                        "AI Rewrite (Demo): " + actionLabel,
                        "Demo Copy Editor",
                        "[" + targetPlatform.getShortName() + "] " + actionLabel,
                        demoResult.substring(0, Math.min(120, demoResult.length())),
                        0,
                        0.0,
                        durationMs
                );

                final String finalDemo = demoResult;
                mainHandler.post(() -> callback.onSuccessWithMeta(finalDemo, false, "Chế độ Demo"));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(
                        "Lỗi gọi AI viết lại (" + actionLabel + "): " + e.getMessage()
                ));
            }
        });
    }

    // --- VISUAL AGENT: REGENERATE ILLUSTRATION FOR PREVIEW SCREEN (F4 / F2) ---
    public void regeneratePostImage(String topic,
                                    String postContent,
                                    SocialPlatform platform,
                                    String previousPrompt,
                                    int nextVariantIndex,
                                    ImageGenerationCallback callback) {
        if (callback == null) return;
        executorService.execute(() -> {
            try {
                PostIllustrationGenerator.GeneratedIllustration result = generateIllustrationSync(
                        topic, postContent, platform, previousPrompt, nextVariantIndex
                );
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Không thể sinh lại ảnh minh họa: " + e.getMessage()));
            }
        });
    }

    private PostIllustrationGenerator.GeneratedIllustration generateIllustrationSync(String topic,
                                                                                     String postContent,
                                                                                     SocialPlatform platform,
                                                                                     String previousPrompt,
                                                                                     int variantIndex) {
        long startMs = System.currentTimeMillis();
        SocialPlatform safePlatform = platform != null ? platform : SocialPlatform.FACEBOOK;
        SecurePreferenceManager prefManager = SecurePreferenceManager.getInstance(context);
        LLMConfig config = prefManager.getConfig();
        boolean useRealApi = !config.isDemoMode()
                && config.getApiKey() != null
                && !config.getApiKey().trim().isEmpty();

        // 1. Craft a fresh, distinct visual prompt (real LLM if BYOK configured, fallback otherwise)
        String fallbackPrompt = PostReviewPolicy.buildFallbackImagePrompt(topic, postContent, safePlatform, variantIndex);
        String finalImagePrompt = fallbackPrompt;
        int promptTokens = 0;

        if (useRealApi) {
            try {
                String promptReq = PostReviewPolicy.buildImagePromptUserPrompt(
                        topic, postContent, safePlatform, previousPrompt, variantIndex
                );
                LlmResponse promptResp = callLlmApiWithMeta(config, promptReq, SYSTEM_PROMPT_VISUAL_AGENT);
                finalImagePrompt = PostReviewPolicy.sanitizeImagePromptOutput(promptResp.text, fallbackPrompt);
                promptTokens = promptResp.totalTokens > 0 ? promptResp.totalTokens : 140;
            } catch (Exception ignored) {
                finalImagePrompt = fallbackPrompt;
            }
        }

        // 2. Attempt real BYOK AI Image Generation API (Gemini native image / OpenAI Images API)
        if (useRealApi) {
            byte[] generatedBytes = null;
            String imageModelUsed = null;
            try {
                if (LLMConfig.PROVIDER_GEMINI.equalsIgnoreCase(config.getProvider())) {
                    ImageApiResult geminiImg = callGeminiImageApi(config.getApiKey(), finalImagePrompt);
                    if (geminiImg != null) {
                        generatedBytes = geminiImg.imageBytes;
                        imageModelUsed = geminiImg.modelName;
                    }
                } else if (LLMConfig.PROVIDER_OPENAI.equalsIgnoreCase(config.getProvider())) {
                    ImageApiResult openaiImg = callOpenAiImageApi(config.getApiKey(), finalImagePrompt, safePlatform);
                    if (openaiImg != null) {
                        generatedBytes = openaiImg.imageBytes;
                        imageModelUsed = openaiImg.modelName;
                    }
                }
            } catch (Exception ignored) {
                generatedBytes = null;
            }

            if (generatedBytes != null && generatedBytes.length > 128) {
                String prefix = safePlatform == SocialPlatform.THREADS ? "threads_ai_" : "fb_ai_";
                String savedUri = PostIllustrationGenerator.saveImageBytes(context, generatedBytes, prefix);
                if (savedUri != null) {
                    int totalTokens = promptTokens + 240;
                    double cost = totalTokens * 0.000004 + 0.015;
                    long durationMs = Math.max(1L, System.currentTimeMillis() - startMs);
                    logStepWithDuration(
                            "Visual Agent (AI Image)",
                            "Visual Designer (" + imageModelUsed + ")",
                            finalImagePrompt,
                            "Generated real AI illustration: " + savedUri,
                            totalTokens,
                            cost,
                            durationMs
                    );
                    prefManager.addTokens(totalTokens, cost);
                    return new PostIllustrationGenerator.GeneratedIllustration(
                            savedUri,
                            finalImagePrompt,
                            "AI Image • " + imageModelUsed,
                            true,
                            variantIndex
                    );
                }
            }
        }

        // 3. Fallback: Render high-resolution channel-sized illustration template directly on-device
        PostIllustrationGenerator.GeneratedIllustration templateImg =
                PostIllustrationGenerator.renderTemplateIllustration(
                        context, topic, postContent, finalImagePrompt, safePlatform, variantIndex
                );

        long durationMs = Math.max(1L, System.currentTimeMillis() - startMs);
        double cost = promptTokens * 0.000004;
        if (promptTokens > 0) {
            prefManager.addTokens(promptTokens, cost);
        }
        logStepWithDuration(
                "Visual Agent (Regenerate Illustration)",
                useRealApi ? "Visual Agent (LLM Prompt + Template)" : "Visual Agent (Demo Template)",
                finalImagePrompt,
                templateImg.sourceLabel + " -> " + (templateImg.imageUri != null ? templateImg.imageUri : "placeholder"),
                promptTokens,
                cost,
                durationMs
        );
        return templateImg;
    }

    private static class ImageApiResult {
        final byte[] imageBytes;
        final String modelName;

        ImageApiResult(byte[] imageBytes, String modelName) {
            this.imageBytes = imageBytes;
            this.modelName = modelName;
        }
    }

    private ImageApiResult callGeminiImageApi(String apiKey, String imagePrompt) {
        String cleanKey = apiKey != null ? apiKey.trim().replace("\"", "").replace("'", "") : "";
        if (cleanKey.isEmpty()) return null;

        for (String model : GEMINI_IMAGE_MODELS) {
            try {
                String url = GEMINI_BASE_URL + model + ":generateContent?key=" + cleanKey;
                JsonObject root = new JsonObject();
                JsonArray contents = new JsonArray();
                JsonObject item = new JsonObject();
                item.addProperty("role", "user");
                JsonArray parts = new JsonArray();
                JsonObject textObj = new JsonObject();
                textObj.addProperty("text", "Generate a clean social media illustration without text overlays: " + imagePrompt);
                parts.add(textObj);
                item.add("parts", parts);
                contents.add(item);
                root.add("contents", contents);

                JsonObject genConfig = new JsonObject();
                JsonArray modalities = new JsonArray();
                modalities.add("TEXT");
                modalities.add("IMAGE");
                genConfig.add("responseModalities", modalities);
                root.add("generationConfig", genConfig);

                Request req = new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(root.toString(), JSON_MEDIA))
                        .build();

                try (Response resp = httpClient.newCall(req).execute()) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        String json = resp.body().string();
                        JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();
                        if (parsed.has("candidates") && parsed.getAsJsonArray("candidates").size() > 0) {
                            JsonObject contentObj = parsed.getAsJsonArray("candidates")
                                    .get(0).getAsJsonObject()
                                    .getAsJsonObject("content");
                            if (contentObj != null && contentObj.has("parts")) {
                                JsonArray respParts = contentObj.getAsJsonArray("parts");
                                for (JsonElement el : respParts) {
                                    JsonObject partObj = el.getAsJsonObject();
                                    JsonObject inlineData = partObj.has("inlineData")
                                            ? partObj.getAsJsonObject("inlineData")
                                            : (partObj.has("inline_data") ? partObj.getAsJsonObject("inline_data") : null);
                                    if (inlineData != null && inlineData.has("data")) {
                                        String b64 = inlineData.get("data").getAsString();
                                        byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                                        if (bytes != null && bytes.length > 128) {
                                            return new ImageApiResult(bytes, model);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // Try next image model or fall back to template renderer
            }
        }
        return null;
    }

    private ImageApiResult callOpenAiImageApi(String apiKey, String imagePrompt, SocialPlatform platform) {
        String cleanKey = apiKey != null ? apiKey.trim().replace("\"", "").replace("'", "") : "";
        if (cleanKey.isEmpty()) return null;

        String[] models = {"gpt-image-1", "dall-e-3"};
        String size = (platform == SocialPlatform.THREADS) ? "1024x1024" : "1792x1024";

        for (String model : models) {
            try {
                JsonObject root = new JsonObject();
                root.addProperty("model", model);
                root.addProperty("prompt", imagePrompt);
                root.addProperty("n", 1);
                root.addProperty("size", "gpt-image-1".equals(model) ? "1024x1024" : size);
                if (!"gpt-image-1".equals(model)) {
                    root.addProperty("response_format", "b64_json");
                }

                Request req = new Request.Builder()
                        .url("https://api.openai.com/v1/images/generations")
                        .header("Authorization", "Bearer " + cleanKey)
                        .post(RequestBody.create(root.toString(), JSON_MEDIA))
                        .build();

                try (Response resp = httpClient.newCall(req).execute()) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        JsonObject parsed = JsonParser.parseString(resp.body().string()).getAsJsonObject();
                        if (parsed.has("data") && parsed.getAsJsonArray("data").size() > 0) {
                            JsonObject first = parsed.getAsJsonArray("data").get(0).getAsJsonObject();
                            if (first.has("b64_json") && !first.get("b64_json").isJsonNull()) {
                                byte[] bytes = Base64.decode(first.get("b64_json").getAsString(), Base64.DEFAULT);
                                if (bytes != null && bytes.length > 128) {
                                    return new ImageApiResult(bytes, model);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // Try next model or fall back cleanly
            }
        }
        return null;
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
        PostReviewPolicy.SafetyCheckResult check = PostReviewPolicy.checkPostSafety(content);
        return new SafetyCheckResult(check.hasWarning, check.warningMessage);
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

    // --- REAL API CALL VIA OKHTTP (Google Gemini / OpenAI / OpenRouter) ---
    private String callLlmApi(LLMConfig config, String prompt, String systemPrompt) throws IOException {
        LlmResponse response = callLlmApiWithMeta(config, prompt, systemPrompt);
        lastCallTokens = response.totalTokens;
        return response.text;
    }

    private LlmResponse callLlmApiWithMeta(LLMConfig config, String prompt, String systemPrompt) throws IOException {
        String cleanApiKey = config.getApiKey() != null ? config.getApiKey().trim().replace("\"", "").replace("'", "") : "";
        if (LLMConfig.PROVIDER_GEMINI.equalsIgnoreCase(config.getProvider())) {
            IOException lastEx = null;

            for (String model : GEMINI_FALLBACK_MODELS) {
                String url = GEMINI_BASE_URL + model + ":generateContent?key=" + cleanApiKey;

                JsonObject root = new JsonObject();

                // Native Gemini system_instruction
                if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                    JsonObject sysInst = new JsonObject();
                    JsonArray sysParts = new JsonArray();
                    JsonObject sysText = new JsonObject();
                    sysText.addProperty("text", systemPrompt);
                    sysParts.add(sysText);
                    sysInst.add("parts", sysParts);
                    root.add("system_instruction", sysInst);
                }

                // User prompt contents
                JsonArray contents = new JsonArray();
                JsonObject item = new JsonObject();
                item.addProperty("role", "user");
                JsonArray parts = new JsonArray();
                JsonObject textObj = new JsonObject();
                textObj.addProperty("text", prompt);
                parts.add(textObj);
                item.add("parts", parts);
                contents.add(item);
                root.add("contents", contents);

                // Generation configuration
                JsonObject genConfig = new JsonObject();
                genConfig.addProperty("temperature", 0.7);
                genConfig.addProperty("maxOutputTokens", 1024);
                root.add("generationConfig", genConfig);

                Request req = new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(root.toString(), JSON_MEDIA))
                        .build();

                try (Response resp = httpClient.newCall(req).execute()) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        String json = resp.body().string();
                        JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();
                        int tokens = 0;
                        if (parsed.has("usageMetadata")) {
                            JsonObject usage = parsed.getAsJsonObject("usageMetadata");
                            if (usage.has("totalTokenCount")) {
                                tokens = usage.get("totalTokenCount").getAsInt();
                            }
                        }
                        if (parsed.has("candidates") && parsed.getAsJsonArray("candidates").size() > 0) {
                            JsonObject candidate = parsed.getAsJsonArray("candidates").get(0).getAsJsonObject();
                            if (candidate.has("content") && candidate.getAsJsonObject("content").has("parts")) {
                                JsonArray respParts = candidate.getAsJsonObject("content").getAsJsonArray("parts");
                                StringBuilder textBuilder = new StringBuilder();
                                for (JsonElement partEl : respParts) {
                                    JsonObject pObj = partEl.getAsJsonObject();
                                    if (pObj.has("text") && !pObj.get("text").isJsonNull()) {
                                        textBuilder.append(pObj.get("text").getAsString());
                                    }
                                }
                                if (textBuilder.length() > 0) {
                                    return new LlmResponse(textBuilder.toString(), tokens);
                                }
                            }
                        }
                        lastEx = new IOException("Model " + model + " trả về kết quả trống.");
                    } else {
                        String errDetail = "HTTP " + resp.code();
                        if (resp.body() != null) {
                            try {
                                String bodyStr = resp.body().string();
                                JsonObject errJson = JsonParser.parseString(bodyStr).getAsJsonObject();
                                if (errJson.has("error") && errJson.getAsJsonObject("error").has("message")) {
                                    errDetail = errJson.getAsJsonObject("error").get("message").getAsString();
                                }
                            } catch (Exception ignored) {}
                        }
                        lastEx = new IOException("Gemini (" + model + "): " + errDetail);
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
                    .header("Authorization", "Bearer " + cleanApiKey)
                    .post(RequestBody.create(root.toString(), JSON_MEDIA))
                    .build();

            try (Response resp = httpClient.newCall(req).execute()) {
                if (resp.isSuccessful() && resp.body() != null) {
                    String json = resp.body().string();
                    JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();
                    int tokens = 0;
                    if (parsed.has("usage") && parsed.getAsJsonObject("usage").has("total_tokens")) {
                        tokens = parsed.getAsJsonObject("usage").get("total_tokens").getAsInt();
                    }
                    String text = parsed.getAsJsonArray("choices")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString();
                    return new LlmResponse(text, tokens);
                } else {
                    String errDetail = "HTTP " + resp.code();
                    if (resp.body() != null) {
                        try {
                            String bodyStr = resp.body().string();
                            JsonObject errJson = JsonParser.parseString(bodyStr).getAsJsonObject();
                            if (errJson.has("error") && errJson.getAsJsonObject("error").has("message")) {
                                errDetail = errJson.getAsJsonObject("error").get("message").getAsString();
                            }
                        } catch (Exception ignored) {}
                    }
                    throw new IOException(config.getProvider() + " API lỗi: " + errDetail);
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

    private void streamThreadsChunk(String fullText, AgentCallback callback) throws InterruptedException {
        String[] words = fullText.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (isCancelled) return;
            current.append(word).append(" ");
            final String textToEmit = current.toString();
            mainHandler.post(() -> callback.onStreamThreadsChunk(textToEmit));
            Thread.sleep(25);
        }
    }

    private void notifyStep(AgentCallback callback, int stepIndex, String title, String agent) {
        mainHandler.post(() -> callback.onStepChanged(stepIndex, title, agent));
    }

    private void logStep(String step, String agent, String prompt, String output, int tokens, double cost) {
        logStepWithDuration(step, agent, prompt, output, tokens, cost, 600);
    }

    private void logStepWithDuration(String step, String agent, String prompt, String output, int tokens, double cost, long durationMs) {
        AgentLog log = new AgentLog(0, System.currentTimeMillis(), step, agent, prompt, output, tokens, cost, durationMs);
        DatabaseHelper.getInstance(context).insertLog(log);
    }

    private String generateThreadsDraft(String topic, String tone, String language) {
        if ("English".equalsIgnoreCase(language)) {
            return "Quick thoughts on " + topic + " 🧵👇\n\n" +
                    "AI Agents are completely changing how we create and distribute content.\n" +
                    "Key insight: On-device execution + human review is the winning formula.\n\n" +
                    "What's your take on this? Let's discuss below! ✨\n#SocialAI #Threads #Tech";
        }
        if ("Hài hước".equalsIgnoreCase(tone)) {
            return "Vừa phát hiện ra một sự thật chấn động về " + topic + " 🧵😂\n\n" +
                    "Tưởng phức tạp lắm nhưng hóa ra áp dụng AI vào cái nhàn hẳn luôn anh em ạ!\n" +
                    "Ai chưa thử thì nên thử ngay nhé.\n\n" +
                    "Anh em thấy đúng không? Thả nhẹ một reply bên dưới nhé 👇\n#ThreadsVN #SocialAI";
        } else if ("Thân thiện".equalsIgnoreCase(tone)) {
            return "Góc chia sẻ nhanh về " + topic + " hôm nay 🌿🧵\n\n" +
                    "Tự động hóa không phải để thay thế con người, mà để chúng ta có thêm thời gian cho những điều ý nghĩa hơn.\n\n" +
                    "Cả nhà nghĩ sao về xu hướng này? Chúc mọi người ngày mới vui vẻ! ✨\n#ThreadsLife #SocialAI";
        } else {
            return "Góc nhìn công nghệ về " + topic + " 🚀🧵\n\n" +
                    "1. Multi-Agent đang định hình lại cách sáng tạo nội dung.\n" +
                    "2. Kiểm soát Human-in-the-loop là yếu tố sống còn cho thương hiệu.\n\n" +
                    "Bạn đã sẵn sàng tích hợp AI Agent vào kênh của mình chưa? 👇\n#SocialAI #MetaThreads #Tech2026";
        }
    }

    private String generateFacebookDraft(String topic, String tone, String language) {
        if ("English".equalsIgnoreCase(language)) {
            return "🚀 Quick Insights on: " + topic + "\n\n" +
                    "Technology moves fast, but consistency and smart automation will always win.\n" +
                    "• 80% repetitive tasks automated with AI\n" +
                    "• 100% human-verified quality\n\n" +
                    "Share your thoughts in the comments below! 👍\n#SocialAI #AI2026";
        }
        return generateFacebookDraft(topic, tone);
    }

    private String generateLinkedInDraft(String topic, String tone, String language) {
        return generateLinkedInDraft(topic, tone);
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

    private String generateThreadsDraft(String topic, String tone) {
        if ("Hài hước".equalsIgnoreCase(tone)) {
            return "Nghĩ cũng buồn cười, cả tuần ngồi mò " + topic + " tưởng cao siêu lắm ai ngờ do thiếu mỗi dấu chấm phẩy 😂\n\n" +
                    "Bài học rút ra: Công nghệ xịn đến đâu thì AI cũng chỉ gánh 80%, 20% còn lại là độ kiên nhẫn của anh em. Ai chung cảnh ngộ không? 👀";
        } else if ("Thân thiện".equalsIgnoreCase(tone)) {
            return "Dạo này mình thử đổi cách tiếp cận với " + topic + " và thấy nhẹ đầu hẳn ✨\n\n" +
                    "Đôi khi không cần giải pháp quá phức tạp, cứ giữ mọi thứ đơn giản và duy trì đều đặn mỗi ngày là tự khắc thấy kết quả.\n\n" +
                    "Mọi người hôm nay thế nào rồi? Cùng chia sẻ bên dưới nhé!";
        } else { // Chuyên gia / Góc nhìn sâu
            return "Một góc nhìn nhanh về " + topic + " mà mình đúc kết được:\n\n" +
                    "Hầu hết mọi người tập trung vào công cụ, nhưng sự khác biệt thực sự nằm ở kiến trúc luồng dữ liệu và tư duy tối ưu hóa vận hành.\n\n" +
                    "Anh em đang làm mảng này thấy nhận định này thế nào? Cùng thảo luận nhé 🧵";
        }
    }

    private String generateLinkedInDraft(String topic, String tone) {
        return generateThreadsDraft(topic, tone);
    }
}
