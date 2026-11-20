package com.tuankiet.myapplication.agent;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tuankiet.myapplication.model.CommentItem;
import com.tuankiet.myapplication.model.SocialPlatform;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Policy & AI classification engine for F6 Inbox:
 * - Classifies comments into 5 categories: QUESTION, PRAISE, SUGGESTION, NEGATIVE, SPAM.
 * - Detects personal information (PII) deterministically.
 * - Strictly enforces Controlled Auto-Reply rules:
 *   ONLY allows PRAISE and FAQ QUESTION with high AI confidence and zero PII / risk flags.
 *   STRICTLY BLOCKS NEGATIVE, SPAM, SUGGESTION, non-FAQ questions, PII, and uncertain AI results.
 */
public final class CommentSafetyPolicy {

    public static final double MIN_AUTO_REPLY_CONFIDENCE = 0.75;

    public static final String CODE_ALLOWED_PRAISE = "ALLOWED_PRAISE";
    public static final String CODE_ALLOWED_FAQ = "ALLOWED_FAQ";
    public static final String CODE_ALREADY_ANSWERED = "ALREADY_ANSWERED";
    public static final String CODE_BLOCKED_NEGATIVE = "BLOCKED_NEGATIVE";
    public static final String CODE_BLOCKED_SPAM = "BLOCKED_SPAM";
    public static final String CODE_BLOCKED_PII = "BLOCKED_PII";
    public static final String CODE_BLOCKED_LOW_CONFIDENCE = "BLOCKED_LOW_CONFIDENCE";
    public static final String CODE_BLOCKED_SUGGESTION = "BLOCKED_SUGGESTION";
    public static final String CODE_BLOCKED_NON_FAQ = "BLOCKED_NON_FAQ";
    public static final String CODE_BLOCKED_FLAGGED = "BLOCKED_FLAGGED";
    public static final String CODE_BLOCKED_OTHER = "BLOCKED_OTHER";

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    );
    private static final Pattern VN_PHONE_PATTERN = Pattern.compile(
            "(?<!\\d)(?:\\+?84|0)(?:3|5|7|8|9)(?:[\\s.\\-]*\\d){8}(?!\\d)"
    );
    private static final Pattern GENERAL_PHONE_PATTERN = Pattern.compile(
            "(?<!\\d)\\d{3,4}[\\s.\\-]\\d{3}[\\s.\\-]\\d{3,4}(?!\\d)"
    );
    private static final Pattern TWELVE_DIGIT_ID_PATTERN = Pattern.compile(
            "(?<!\\d)\\d{12}(?!\\d)"
    );
    private static final Pattern CARD_OR_BANK_DIGITS_PATTERN = Pattern.compile(
            "(?<!\\d)\\d{4}[\\s.\\-]?\\d{4}[\\s.\\-]?\\d{4}[\\s.\\-]?\\d{4}(?!\\d)"
    );
    private static final Pattern NINE_DIGIT_WITH_ID_CONTEXT_PATTERN = Pattern.compile(
            "(?i)(cccd|cmnd|căn cước|can cuoc|chứng minh|chung minh|stk|số tài khoản|so tai khoan|mã số thuế|ma so thue|hộ chiếu|ho chieu|passport).{0,25}\\d{6,}"
    );
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
            "(?i)(số\\s*\\d+[a-z]?(?:/\\d+)?\\s+(?:đường|duong|phố|pho|ngõ|ngo|hẻm|hem)|phường\\s+[\\p{L}\\d\\s]{2,20},?\\s*quận\\s+[\\p{L}\\d\\s]{1,20})"
    );
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)(https?://|www\\.|bit\\.ly/|tinyurl\\.com/|t\\.me/|zalo\\.me/)"
    );

    private CommentSafetyPolicy() {
    }

    public static class CommentAnalysis {
        public final String category;
        public final boolean isFaq;
        public final boolean hasPersonalInfo;
        public final double confidence;
        public final boolean negativeFlagged;
        public final String riskWarning;
        public final String suggestedReply;
        public final String autoReplyBlockedReason;

        public CommentAnalysis(String category,
                               boolean isFaq,
                               boolean hasPersonalInfo,
                               double confidence,
                               boolean negativeFlagged,
                               String riskWarning,
                               String suggestedReply,
                               String autoReplyBlockedReason) {
            this.category = category;
            this.isFaq = isFaq;
            this.hasPersonalInfo = hasPersonalInfo;
            this.confidence = Math.max(0.0, Math.min(1.0, confidence));
            this.negativeFlagged = negativeFlagged;
            this.riskWarning = riskWarning;
            this.suggestedReply = suggestedReply;
            this.autoReplyBlockedReason = autoReplyBlockedReason;
        }
    }

    public static class AutoReplyDecision {
        public final boolean allowed;
        public final String reasonCode;
        public final String reasonMessage;

        public AutoReplyDecision(boolean allowed, String reasonCode, String reasonMessage) {
            this.allowed = allowed;
            this.reasonCode = reasonCode;
            this.reasonMessage = reasonMessage;
        }
    }

    /**
     * Deterministic detection of Personal Identifiable Information (PII):
     * phone numbers, emails, national IDs (CCCD/CMND), bank accounts/cards, home addresses, OTPs.
     */
    public static boolean containsPersonalInfo(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        String trimmed = text.trim();
        if (EMAIL_PATTERN.matcher(trimmed).find()
                || VN_PHONE_PATTERN.matcher(trimmed).find()
                || GENERAL_PHONE_PATTERN.matcher(trimmed).find()
                || TWELVE_DIGIT_ID_PATTERN.matcher(trimmed).find()
                || CARD_OR_BANK_DIGITS_PATTERN.matcher(trimmed).find()
                || NINE_DIGIT_WITH_ID_CONTEXT_PATTERN.matcher(trimmed).find()
                || ADDRESS_PATTERN.matcher(trimmed).find()) {
            return true;
        }

        String lower = trimmed.toLowerCase(Locale.ROOT);
        return lower.contains("cccd")
                || lower.contains("cmnd")
                || lower.contains("căn cước")
                || lower.contains("số tài khoản")
                || lower.contains("so tai khoan")
                || lower.contains("mã otp")
                || lower.contains("ma otp")
                || lower.contains("địa chỉ nhà")
                || lower.contains("dia chi nha")
                || lower.contains("thẻ tín dụng")
                || lower.contains("the tin dung")
                || lower.contains("mật khẩu")
                || lower.contains("mat khau");
    }

    /**
     * Evaluates whether a comment is strictly eligible for Controlled Auto-Reply.
     */
    public static AutoReplyDecision evaluateAutoReply(CommentItem item) {
        if (item == null) {
            return new AutoReplyDecision(false, CODE_BLOCKED_OTHER, "Không có dữ liệu bình luận.");
        }
        if (item.isAnswered()) {
            return new AutoReplyDecision(false, CODE_ALREADY_ANSWERED, "Bình luận đã được trả lời.");
        }

        String category = item.getCategory();
        boolean piiDetected = item.hasPersonalInfo() || containsPersonalInfo(item.getContent());

        if (CommentItem.CATEGORY_NEGATIVE.equalsIgnoreCase(category)) {
            return new AutoReplyDecision(
                    false,
                    CODE_BLOCKED_NEGATIVE,
                    "Bình luận Tiêu cực — Tuyệt đối khóa tự động trả lời để tránh rủi ro thương hiệu."
            );
        }

        if (CommentItem.CATEGORY_SPAM.equalsIgnoreCase(category)) {
            return new AutoReplyDecision(
                    false,
                    CODE_BLOCKED_SPAM,
                    "Bình luận Spam — Không tự động phản hồi spam/liên kết rác."
            );
        }

        if (piiDetected) {
            return new AutoReplyDecision(
                    false,
                    CODE_BLOCKED_PII,
                    "Phát hiện thông tin cá nhân (SĐT/Email/CCCD/Địa chỉ) — Tuyệt đối khóa tự động trả lời để bảo vệ quyền riêng tư."
            );
        }

        if (item.getAiConfidence() < MIN_AUTO_REPLY_CONFIDENCE) {
            int pct = (int) Math.round(item.getAiConfidence() * 100.0);
            return new AutoReplyDecision(
                    false,
                    CODE_BLOCKED_LOW_CONFIDENCE,
                    "AI chưa đủ độ chắc chắn (" + pct + "% < 75%) — Khóa tự động trả lời, yêu cầu duyệt thủ công."
            );
        }

        if (item.isNegativeFlagged()) {
            return new AutoReplyDecision(
                    false,
                    CODE_BLOCKED_FLAGGED,
                    item.getRiskWarning() != null && !item.getRiskWarning().trim().isEmpty()
                            ? item.getRiskWarning()
                            : "Bình luận bị gắn cờ an toàn — Khóa tự động trả lời."
            );
        }

        if (CommentItem.CATEGORY_SUGGESTION.equalsIgnoreCase(category)) {
            return new AutoReplyDecision(
                    false,
                    CODE_BLOCKED_SUGGESTION,
                    "Nhóm Góp ý — Cần quản trị viên trực tiếp tiếp thu và chỉnh sửa câu trả lời."
            );
        }

        if (CommentItem.CATEGORY_PRAISE.equalsIgnoreCase(category)) {
            return new AutoReplyDecision(
                    true,
                    CODE_ALLOWED_PRAISE,
                    "Đủ điều kiện tự động trả lời (Khen ngợi an toàn)."
            );
        }

        if (CommentItem.CATEGORY_QUESTION.equalsIgnoreCase(category)) {
            if (item.isFaq()) {
                return new AutoReplyDecision(
                        true,
                        CODE_ALLOWED_FAQ,
                        "Đủ điều kiện tự động trả lời (Câu hỏi thường gặp - FAQ)."
                );
            }
            return new AutoReplyDecision(
                    false,
                    CODE_BLOCKED_NON_FAQ,
                    "Câu hỏi chuyên sâu / ngoài nhóm FAQ — Cần con người duyệt và chỉnh sửa trước khi gửi."
            );
        }

        return new AutoReplyDecision(
                false,
                CODE_BLOCKED_OTHER,
                "Nhóm bình luận chưa xác định — Khóa tự động trả lời."
        );
    }

    public static boolean shouldShowSafetyAlert(CommentItem item) {
        if (item == null) return false;
        String cat = item.getCategory();
        return CommentItem.CATEGORY_NEGATIVE.equalsIgnoreCase(cat)
                || CommentItem.CATEGORY_SPAM.equalsIgnoreCase(cat)
                || item.isNegativeFlagged()
                || item.hasPersonalInfo()
                || containsPersonalInfo(item.getContent())
                || item.getAiConfidence() < MIN_AUTO_REPLY_CONFIDENCE;
    }

    public static boolean shouldHideAutoSuggestionByDefault(CommentItem item) {
        if (item == null) return true;
        String cat = item.getCategory();
        return CommentItem.CATEGORY_NEGATIVE.equalsIgnoreCase(cat)
                || CommentItem.CATEGORY_SPAM.equalsIgnoreCase(cat);
    }

    public static String getSafetyBannerSummary(CommentItem item) {
        if (item == null) return "";
        if (CommentItem.CATEGORY_NEGATIVE.equalsIgnoreCase(item.getCategory())) {
            return "Cảnh báo AI: Bình luận tiêu cực • Khóa tự động trả lời";
        }
        if (CommentItem.CATEGORY_SPAM.equalsIgnoreCase(item.getCategory())) {
            return "Cảnh báo AI: Phát hiện Spam • Khóa tự động trả lời";
        }
        if (item.hasPersonalInfo() || containsPersonalInfo(item.getContent())) {
            return "Cảnh báo bảo mật: Chứa thông tin cá nhân • Khóa tự động trả lời";
        }
        if (item.getAiConfidence() < MIN_AUTO_REPLY_CONFIDENCE) {
            int pct = (int) Math.round(item.getAiConfidence() * 100.0);
            return "AI chưa chắc chắn (" + pct + "%) • Khóa tự động trả lời";
        }
        if (item.isNegativeFlagged()) {
            return "Cảnh báo an toàn AI • Khóa tự động trả lời";
        }
        return "";
    }

    /**
     * Parses structured JSON output from an LLM (Gemini / OpenAI / OpenRouter)
     * and applies deterministic safety guardrails on top.
     */
    public static CommentAnalysis parseLlmAnalysisJson(String rawResponse,
                                                       SocialPlatform platform,
                                                       String postTitle,
                                                       String postContent,
                                                       String authorName,
                                                       String commentText) {
        CommentAnalysis localFallback = analyzeCommentLocally(platform, postTitle, postContent, authorName, commentText);
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return localFallback;
        }

        try {
            String cleaned = stripCodeFences(rawResponse.trim());
            int firstBrace = cleaned.indexOf('{');
            int lastBrace = cleaned.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                cleaned = cleaned.substring(firstBrace, lastBrace + 1);
            }

            JsonObject obj = JsonParser.parseString(cleaned).getAsJsonObject();

            String rawCategory = getStringOrNull(obj, "category");
            String category = CommentItem.normalizeCategory(rawCategory);
            double confidence = getDoubleOrDefault(obj, "confidence", 0.85);
            if (category == null) {
                category = localFallback.category;
                confidence = Math.min(confidence, 0.55);
            }

            boolean llmIsFaq = getBooleanOrDefault(obj, "is_faq", false);
            boolean llmHasPii = getBooleanOrDefault(obj, "has_personal_info", false);
            String llmRiskWarning = getStringOrNull(obj, "risk_warning");
            String llmReply = getStringOrNull(obj, "suggested_reply");

            // Defense-in-depth deterministic checks
            boolean deterministicPii = containsPersonalInfo(commentText);
            boolean hasPii = llmHasPii || deterministicPii;

            if (CommentItem.CATEGORY_SPAM.equals(localFallback.category)
                    && !CommentItem.CATEGORY_NEGATIVE.equals(category)) {
                category = CommentItem.CATEGORY_SPAM;
            } else if (CommentItem.CATEGORY_NEGATIVE.equals(localFallback.category)
                    && localFallback.confidence >= 0.85
                    && (CommentItem.CATEGORY_PRAISE.equals(category) || CommentItem.CATEGORY_QUESTION.equals(category))) {
                // Prevent dangerous misclassification of explicitly hostile comments as safe praise/question
                category = CommentItem.CATEGORY_NEGATIVE;
                confidence = Math.min(confidence, 0.65);
            }

            boolean isFaq = CommentItem.CATEGORY_QUESTION.equals(category) && llmIsFaq && !hasPii;
            boolean negativeFlagged = CommentItem.CATEGORY_NEGATIVE.equals(category)
                    || CommentItem.CATEGORY_SPAM.equals(category)
                    || hasPii
                    || confidence < MIN_AUTO_REPLY_CONFIDENCE;

            String riskWarning = buildRiskWarning(category, hasPii, confidence, llmRiskWarning);
            String finalReply = (llmReply != null && !llmReply.trim().isEmpty())
                    ? sanitizeSuggestedReply(llmReply, platform)
                    : localFallback.suggestedReply;

            if (CommentItem.CATEGORY_SPAM.equals(category)) {
                finalReply = null;
            } else if (hasPii && (finalReply == null || finalReply.trim().isEmpty())) {
                finalReply = buildPiiSafeDraft(authorName, platform);
            }

            CommentItem probe = new CommentItem();
            probe.setCategory(category);
            probe.setFaq(isFaq);
            probe.setHasPersonalInfo(hasPii);
            probe.setAiConfidence(confidence);
            probe.setNegativeFlagged(negativeFlagged);
            probe.setRiskWarning(riskWarning);
            probe.setContent(commentText);
            AutoReplyDecision decision = evaluateAutoReply(probe);

            return new CommentAnalysis(
                    category,
                    isFaq,
                    hasPii,
                    confidence,
                    negativeFlagged,
                    riskWarning,
                    finalReply,
                    decision.allowed ? null : decision.reasonMessage
            );
        } catch (Exception ignored) {
            return localFallback;
        }
    }

    /**
     * Local AI heuristic fallback (Demo mode / offline / LLM timeout) supporting all 5 categories,
     * deterministic PII detection, confidence scoring, and post-context-aware suggested replies.
     */
    public static CommentAnalysis analyzeCommentLocally(SocialPlatform platform,
                                                        String postTitle,
                                                        String postContent,
                                                        String authorName,
                                                        String commentText) {
        String text = commentText != null ? commentText.trim() : "";
        String lower = text.toLowerCase(Locale.ROOT);
        boolean hasPii = containsPersonalInfo(text);

        String category;
        boolean isFaq = false;
        double confidence;

        if (isSpamText(lower, text)) {
            category = CommentItem.CATEGORY_SPAM;
            confidence = 0.96;
        } else if (isNegativeText(lower)) {
            category = CommentItem.CATEGORY_NEGATIVE;
            confidence = 0.91;
        } else if (isSuggestionText(lower)) {
            category = CommentItem.CATEGORY_SUGGESTION;
            confidence = 0.88;
        } else if (isQuestionText(lower)) {
            category = CommentItem.CATEGORY_QUESTION;
            if (isComplexOrUncertainQuestion(lower, text)) {
                isFaq = false;
                confidence = isUncertainText(lower, text) ? 0.54 : 0.82;
            } else if (isFaqQuestion(lower)) {
                isFaq = !hasPii;
                confidence = 0.91;
            } else {
                isFaq = false;
                confidence = 0.72; // Uncertain whether FAQ -> requires human review
            }
        } else if (isPraiseText(lower)) {
            if (hasSarcasmSignals(lower)) {
                category = CommentItem.CATEGORY_NEGATIVE;
                confidence = 0.62;
            } else {
                category = CommentItem.CATEGORY_PRAISE;
                confidence = 0.93;
            }
        } else {
            // Ambiguous / unrecognized comment -> explicitly low confidence so bot NEVER auto-replies!
            category = CommentItem.CATEGORY_QUESTION;
            isFaq = false;
            confidence = 0.50;
        }

        boolean negativeFlagged = CommentItem.CATEGORY_NEGATIVE.equals(category)
                || CommentItem.CATEGORY_SPAM.equals(category)
                || hasPii
                || confidence < MIN_AUTO_REPLY_CONFIDENCE;

        String riskWarning = buildRiskWarning(category, hasPii, confidence, null);
        String suggestedReply = buildContextualReply(
                platform, postTitle, postContent, authorName, text, category, isFaq, hasPii
        );

        CommentItem probe = new CommentItem();
        probe.setCategory(category);
        probe.setFaq(isFaq);
        probe.setHasPersonalInfo(hasPii);
        probe.setAiConfidence(confidence);
        probe.setNegativeFlagged(negativeFlagged);
        probe.setRiskWarning(riskWarning);
        probe.setContent(text);
        AutoReplyDecision decision = evaluateAutoReply(probe);

        return new CommentAnalysis(
                category,
                isFaq,
                hasPii,
                confidence,
                negativeFlagged,
                riskWarning,
                suggestedReply,
                decision.allowed ? null : decision.reasonMessage
        );
    }

    public static void applyAnalysisToComment(CommentItem item, CommentAnalysis analysis) {
        if (item == null || analysis == null) return;
        item.setCategory(analysis.category);
        item.setFaq(analysis.isFaq);
        item.setHasPersonalInfo(analysis.hasPersonalInfo);
        item.setAiConfidence(analysis.confidence);
        item.setNegativeFlagged(analysis.negativeFlagged);
        item.setRiskWarning(analysis.riskWarning);
        item.setAiSuggestedReply(analysis.suggestedReply);
        item.setAutoReplyBlockedReason(analysis.autoReplyBlockedReason);
    }

    private static boolean isSpamText(String lower, String raw) {
        if (URL_PATTERN.matcher(raw).find() && (lower.contains("nhận ngay") || lower.contains("khóa học")
                || lower.contains("làm giàu") || lower.contains("kiếm tiền") || lower.contains("spam")
                || lower.contains("tặng") || lower.contains("miễn phí 100%") || lower.contains("click"))) {
            return true;
        }
        return lower.contains("làm giàu cấp tốc")
                || lower.contains("kiếm tiền tại nhà")
                || lower.contains("tuyển ctv")
                || lower.contains("vay vốn nhanh")
                || lower.contains("nhà cái")
                || lower.contains("cá cược")
                || lower.contains("bấm vào link")
                || lower.contains("bit.ly/spam")
                || lower.contains("inbox nhận lộc")
                || lower.contains("hoa hồng khủng");
    }

    private static boolean isNegativeText(String lower) {
        return lower.contains("lừa đảo")
                || lower.contains("scam")
                || lower.contains("nói suông")
                || lower.contains("giống copy")
                || lower.contains("đạo nhái")
                || lower.contains("ăn cắp")
                || lower.contains("giống bot")
                || lower.contains("thất vọng")
                || lower.contains("quá tệ")
                || lower.contains("rất tệ")
                || lower.contains("dở quá")
                || lower.contains("nhảm nhí")
                || lower.contains("rác rưởi")
                || lower.contains("vô dụng")
                || lower.contains("tẩy chay")
                || lower.contains("bóc phốt")
                || lower.contains("lỗi hoài")
                || lower.contains("kém chất lượng")
                || lower.contains("chán quá")
                || lower.contains("phí thời gian")
                || lower.contains("đừng có nói")
                || containsStandaloneWord(lower, "tệ")
                || containsStandaloneWord(lower, "dở")
                || containsStandaloneWord(lower, "ngu")
                || containsStandaloneWord(lower, "ghét")
                || containsStandaloneWord(lower, "rác")
                || containsStandaloneWord(lower, "nhảm");
    }

    private static boolean isSuggestionText(String lower) {
        return lower.contains("góp ý")
                || lower.contains("gop y")
                || lower.contains("đề xuất")
                || lower.contains("de xuat")
                || lower.contains("nên thêm")
                || lower.contains("nên bổ sung")
                || lower.contains("bổ sung thêm")
                || lower.contains("nên làm thêm")
                || lower.contains("nên có thêm")
                || lower.contains("cải thiện")
                || lower.contains("cải tiến")
                || lower.contains("sẽ hay hơn nếu")
                || lower.contains("sẽ tốt hơn nếu")
                || lower.contains("giá như")
                || lower.contains("khuyến nghị")
                || lower.contains("mong admin thêm");
    }

    private static boolean isQuestionText(String lower) {
        return lower.contains("?")
                || lower.contains("cho hỏi")
                || lower.contains("cho em hỏi")
                || lower.contains("cho mình hỏi")
                || lower.contains("cho tôi hỏi")
                || lower.contains("làm sao")
                || lower.contains("như thế nào")
                || lower.contains("như nào")
                || lower.contains("ntn")
                || lower.contains("có hỗ trợ")
                || lower.contains("bao nhiêu")
                || lower.contains("dùng thử")
                || lower.contains("ở đâu")
                || lower.contains("khi nào")
                || lower.contains("tư vấn")
                || lower.contains("hướng dẫn")
                || lower.contains("tại sao")
                || lower.contains("có được không")
                || lower.contains("có bị")
                || lower.contains("có miễn phí");
    }

    private static boolean isFaqQuestion(String lower) {
        return lower.contains("api key")
                || lower.contains("byok")
                || lower.contains("gemini")
                || lower.contains("miễn phí")
                || lower.contains("dùng thử")
                || lower.contains("demo")
                || lower.contains("hỗ trợ")
                || lower.contains("cài đặt")
                || lower.contains("tải ở đâu")
                || lower.contains("link tải")
                || lower.contains("cách dùng")
                || lower.contains("hướng dẫn")
                || lower.contains("delay")
                || lower.contains("streaming")
                || lower.contains("android")
                || lower.contains("threads")
                || lower.contains("facebook")
                || lower.contains("giá");
    }

    private static boolean isComplexOrUncertainQuestion(String lower, String raw) {
        return lower.contains("hợp đồng")
                || lower.contains("sla")
                || lower.contains("bồi thường")
                || lower.contains("pháp lý")
                || lower.contains("hoàn tiền")
                || lower.contains("khiếu nại")
                || lower.contains("on-premise")
                || lower.contains("doanh nghiệp")
                || lower.contains("chính sách bảo mật")
                || lower.contains("điều khoản")
                || raw.length() < 10;
    }

    private static boolean isUncertainText(String lower, String raw) {
        return lower.contains("sla")
                || lower.contains("bồi thường")
                || lower.contains("pháp lý")
                || lower.contains("on-premise")
                || raw.length() < 10;
    }

    private static boolean isPraiseText(String lower) {
        return lower.contains("hay quá")
                || lower.contains("rất hay")
                || lower.contains("bài viết hay")
                || lower.contains("tuyệt vời")
                || lower.contains("sâu sắc")
                || lower.contains("hữu ích")
                || lower.contains("thiết thực")
                || lower.contains("xuất sắc")
                || lower.contains("chất lượng")
                || lower.contains("cảm ơn")
                || lower.contains("cám ơn")
                || lower.contains("thanks")
                || lower.contains("thank you")
                || lower.contains("rất mượt")
                || lower.contains("đồng tình")
                || lower.contains("ủng hộ")
                || lower.contains("quá chuẩn")
                || lower.contains("rất thích")
                || lower.contains("repost")
                || lower.contains("đỉnh quá")
                || lower.contains("xịn quá")
                || containsStandaloneWord(lower, "hay")
                || containsStandaloneWord(lower, "tuyệt");
    }

    private static boolean hasSarcasmSignals(String lower) {
        return lower.contains("hay quá cơ")
                || lower.contains("tuyệt vời ghê á")
                || lower.contains("giỏi quá ha")
                || lower.contains("chắc tin được");
    }

    private static boolean containsStandaloneWord(String lower, String word) {
        return Pattern.compile("(?<![\\p{L}\\d])" + Pattern.quote(word) + "(?![\\p{L}\\d])")
                .matcher(lower)
                .find();
    }

    private static String buildRiskWarning(String category, boolean hasPii, double confidence, String llmWarning) {
        if (CommentItem.CATEGORY_NEGATIVE.equals(category)) {
            return llmWarning != null && !llmWarning.trim().isEmpty()
                    ? llmWarning
                    : "CẢNH BÁO AN TOÀN AI: Bình luận có sắc thái tiêu cực hoặc rủi ro uy tín thương hiệu. " +
                    "Chế độ tự động trả lời đã bị KHÓA tuyệt đối. Vui lòng xem xét và phản hồi thủ công.";
        }
        if (CommentItem.CATEGORY_SPAM.equals(category)) {
            return llmWarning != null && !llmWarning.trim().isEmpty()
                    ? llmWarning
                    : "CẢNH BÁO SPAM: Phát hiện nội dung quảng cáo rác hoặc liên kết đáng ngờ. " +
                    "Hệ thống đã khóa tự động trả lời; khuyến nghị bỏ qua hoặc ẩn bình luận.";
        }
        if (hasPii) {
            return "CẢNH BÁO BẢO MẬT (PII): Bình luận chứa thông tin cá nhân nhạy cảm (SĐT, Email, CCCD/Địa chỉ...). " +
                    "Tuyệt đối KHÔNG tự động trả lời công khai. Vui lòng duyệt thủ công và hướng dẫn khách nhắn tin riêng (DM).";
        }
        if (confidence < MIN_AUTO_REPLY_CONFIDENCE) {
            int pct = (int) Math.round(confidence * 100.0);
            return "CẢNH BÁO ĐỘ TIN CẬY AI (" + pct + "%): AI chưa chắc chắn về ý định hoặc ngữ cảnh của bình luận này. " +
                    "Hệ thống đã khóa tự động trả lời để tránh phát ngôn sai lệch. Vui lòng kiểm tra và chỉnh sửa thủ công.";
        }
        return llmWarning;
    }

    private static String buildPiiSafeDraft(String authorName, SocialPlatform platform) {
        String greeting = formatAuthorGreeting(authorName, platform);
        return "Chào " + greeting + ", để bảo mật thông tin cá nhân của bạn, bạn vui lòng nhắn tin trực tiếp (Inbox/DM) cho kênh để đội ngũ hỗ trợ riêng nhé!";
    }

    private static String buildContextualReply(SocialPlatform platform,
                                               String postTitle,
                                               String postContent,
                                               String authorName,
                                               String commentText,
                                               String category,
                                               boolean isFaq,
                                               boolean hasPii) {
        if (CommentItem.CATEGORY_SPAM.equals(category)) {
            return null;
        }
        String greeting = formatAuthorGreeting(authorName, platform);
        String topicRef = extractTopicReference(postTitle, postContent);

        if (hasPii) {
            return buildPiiSafeDraft(authorName, platform);
        }

        if (CommentItem.CATEGORY_NEGATIVE.equals(category)) {
            return "Chào " + greeting + ", cảm ơn bạn đã thẳng thắn chia sẻ góc nhìn về bài \"" + topicRef + "\". " +
                    "Đội ngũ xin tiếp nhận phản hồi và sẽ kiểm tra, làm rõ thông tin cụ thể nhất đến bạn.";
        }

        if (CommentItem.CATEGORY_SUGGESTION.equals(category)) {
            return "Cảm ơn góp ý rất giá trị từ " + greeting + " về chủ đề \"" + topicRef + "\"! " +
                    "Đội ngũ đã ghi nhận đề xuất của bạn để bổ sung và nâng cấp trong các bài viết tiếp theo nhé.";
        }

        if (CommentItem.CATEGORY_QUESTION.equals(category)) {
            String lower = commentText.toLowerCase(Locale.ROOT);
            if (lower.contains("api key") || lower.contains("byok") || lower.contains("gemini") || lower.contains("miễn phí")) {
                return "Chào " + greeting + ", ứng dụng hỗ trợ nhập trực tiếp API Key cá nhân (BYOK: Gemini, OpenAI, OpenRouter) lưu mã hóa trên máy và có sẵn Chế độ Demo miễn phí để bạn trải nghiệm ngay về chủ đề \"" + topicRef + "\" nhé!";
            }
            if (lower.contains("delay") || lower.contains("streaming") || lower.contains("android")) {
                return "Chào " + greeting + ", liên quan đến bài \"" + topicRef + "\", khi gọi REST API kết hợp xử lý bất đồng bộ và cập nhật UI mượt mà, độ trễ rất thấp và chạy ổn định trên Android Native bạn nhé!";
            }
            if (lower.contains("dùng thử") || lower.contains("giá") || lower.contains("bao nhiêu")) {
                return "Chào " + greeting + ", bạn hoàn toàn có thể bật Chế độ Demo dùng thử miễn phí ngay trên app để trải nghiệm các tính năng trong bài \"" + topicRef + "\". Nếu cần thêm thông tin chi tiết cứ nhắn mình nhé!";
            }
            if (isFaq) {
                return "Chào " + greeting + ", cảm ơn câu hỏi của bạn về bài \"" + topicRef + "\"! Bạn có thể trải nghiệm trực tiếp tính năng này trong ứng dụng SocialAI (hỗ trợ cả Facebook Page & Threads) nhé ✨";
            }
            return "Chào " + greeting + ", cảm ơn bạn đã đặt câu hỏi chi tiết về bài \"" + topicRef + "\". " +
                    "Admin đang kiểm tra thông tin chuyên sâu và sẽ phản hồi chính xác nhất cho bạn ngay nhé!";
        }

        // PRAISE
        if (platform == SocialPlatform.THREADS) {
            return "Cảm ơn " + greeting + " rất nhiều vì đã ủng hộ bài chia sẻ về \"" + topicRef + "\" nhé! Sắp tới kênh sẽ lên thêm nhiều góc nhìn thực tế nữa 🙌";
        }
        return "Dạ cảm ơn " + greeting + " đã dành lời khen và quan tâm đến bài viết \"" + topicRef + "\"! " +
                "Đây là động lực lớn để đội ngũ tiếp tục ra mắt thêm nhiều nội dung chất lượng hơn nữa ạ ✨";
    }

    private static String formatAuthorGreeting(String authorName, SocialPlatform platform) {
        if (authorName == null || authorName.trim().isEmpty()) {
            return "bạn";
        }
        String clean = authorName.trim();
        if (platform == SocialPlatform.THREADS && clean.startsWith("@")) {
            return clean;
        }
        if (clean.startsWith("@")) {
            return clean;
        }
        String[] parts = clean.split("\\s+");
        return parts.length > 1 ? "bạn " + parts[parts.length - 1] : clean;
    }

    private static String extractTopicReference(String postTitle, String postContent) {
        String source = (postTitle != null && !postTitle.trim().isEmpty())
                ? postTitle.trim()
                : (postContent != null ? postContent.trim() : "bài viết này");
        source = source.replaceFirst("^\\[Threads\\]\\s*", "")
                .replaceFirst("^\\[Facebook\\]\\s*", "")
                .replace("\n", " ")
                .trim();
        if (source.length() > 55) {
            return source.substring(0, 52) + "...";
        }
        return source;
    }

    private static String sanitizeSuggestedReply(String reply, SocialPlatform platform) {
        String cleaned = reply.trim();
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() > 2) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        int maxLen = (platform == SocialPlatform.THREADS) ? 450 : 800;
        if (cleaned.length() > maxLen) {
            cleaned = cleaned.substring(0, maxLen - 3) + "...";
        }
        return cleaned;
    }

    private static String stripCodeFences(String text) {
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline > 0) {
                text = text.substring(firstNewline + 1);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
        }
        return text.trim();
    }

    private static String getStringOrNull(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) return null;
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return null;
        String val = el.getAsString();
        return (val != null && !val.trim().isEmpty() && !"null".equalsIgnoreCase(val.trim())) ? val.trim() : null;
    }

    private static boolean getBooleanOrDefault(JsonObject obj, String key, boolean def) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return def;
        try {
            return obj.get(key).getAsBoolean();
        } catch (Exception e) {
            return def;
        }
    }

    private static double getDoubleOrDefault(JsonObject obj, String key, double def) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return def;
        try {
            return obj.get(key).getAsDouble();
        } catch (Exception e) {
            return def;
        }
    }
}
