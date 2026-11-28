package com.tuankiet.myapplication.agent;

import com.tuankiet.myapplication.model.Post;
import com.tuankiet.myapplication.model.SocialPlatform;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Human-in-the-loop review, accurate character counting, AI rewrite prompt/sanitization,
 * and publish safety gate for F4 (Preview & Approval screen).
 */
public final class PostReviewPolicy {

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

    // Official platform character limits
    public static final int FACEBOOK_CHAR_LIMIT = 63206;
    public static final int THREADS_CHAR_LIMIT = 500;
    public static final int LINKEDIN_CHAR_LIMIT = 3000;

    public static final double NEAR_LIMIT_RATIO = 0.90;

    // Rewrite instructions
    public static final String REWRITE_SHORTER = "SHORTER";
    public static final String REWRITE_FORMAL = "FORMAL";
    public static final String REWRITE_HOOK = "HOOK";

    // Safety gate decision codes
    public static final String CODE_APPROVED_READY = "APPROVED_READY";
    public static final String CODE_MISSING_HUMAN_APPROVAL = "MISSING_HUMAN_APPROVAL";
    public static final String CODE_NO_TARGET_CHANNEL = "NO_TARGET_CHANNEL";
    public static final String CODE_EMPTY_CONTENT = "EMPTY_CONTENT";
    public static final String CODE_THREADS_LIMIT_EXCEEDED = "THREADS_LIMIT_EXCEEDED";
    public static final String CODE_FACEBOOK_LIMIT_EXCEEDED = "FACEBOOK_LIMIT_EXCEEDED";
    public static final String CODE_LINKEDIN_LIMIT_EXCEEDED = "LINKEDIN_LIMIT_EXCEEDED";

    private static final String[] IMAGE_STYLE_PRESETS = {
            "modern minimalist 3D glassmorphism illustration, ocean blue and cyan luminous accents, clean studio lighting",
            "editorial isometric tech vector artwork, deep indigo and electric teal palette, crisp geometric depth",
            "clean modern infographic hero visual, emerald and ivory contrast, subtle architectural grid lines",
            "warm minimalist editorial studio visual, amber coral gradient lighting, refined contemporary typography space",
            "futuristic blueprint dark-slate composition, luminous sky-blue accents, high-contrast minimalist aesthetic",
            "soft abstract prism gradient artwork, royal violet and ocean cyan highlights, airy premium social media card"
    };

    private static final String[] DEMO_HOOKS_VI = {
            "🚀 DỪNG LƯỚT 15 GIÂY: Đây là góc nhìn thực tế nhất giúp bạn đi trước một bước!",
            "💡 Sự thật ít ai nói thẳng: Phần lớn mọi người đang bỏ lỡ cơ hội ngay trước mắt.",
            "⚡ Nếu bạn chỉ có 1 phút hôm nay, đừng bỏ qua bài học đắt giá này:",
            "🔎 Một thay đổi nhỏ nhưng tạo ra khác biệt gấp 3 lần hiệu quả — bạn đã thử chưa?"
    };

    private static final String[] DEMO_HOOKS_EN = {
            "🚀 STOP SCROLLING: Here is the sharpest practical insight you need this week.",
            "💡 Unpopular truth: Most teams are optimizing the wrong bottleneck right now.",
            "⚡ If you only read one post today, make it this breakdown:",
            "🔎 One subtle workflow shift that multiplies results without extra overhead:"
    };

    private PostReviewPolicy() {
    }

    public static class CharCountResult {
        public final int count;
        public final int limit;
        public final int remaining;
        public final int overflow;
        public final boolean isEmpty;
        public final boolean isNearLimit;
        public final boolean isOverLimit;
        public final SocialPlatform platform;
        public final String formattedCounter;

        public CharCountResult(int count,
                               int limit,
                               boolean isEmpty,
                               boolean isNearLimit,
                               boolean isOverLimit,
                               SocialPlatform platform,
                               String formattedCounter) {
            this.count = count;
            this.limit = limit;
            this.remaining = limit - count;
            this.overflow = Math.max(0, count - limit);
            this.isEmpty = isEmpty;
            this.isNearLimit = isNearLimit;
            this.isOverLimit = isOverLimit;
            this.platform = platform;
            this.formattedCounter = formattedCounter;
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

    public static class PublishGateResult {
        public final boolean allowed;
        public final String code;
        public final String blockReason;
        public final boolean hasSafetyWarning;
        public final String safetyWarningMessage;

        public PublishGateResult(boolean allowed,
                                 String code,
                                 String blockReason,
                                 boolean hasSafetyWarning,
                                 String safetyWarningMessage) {
            this.allowed = allowed;
            this.code = code;
            this.blockReason = blockReason;
            this.hasSafetyWarning = hasSafetyWarning;
            this.safetyWarningMessage = safetyWarningMessage;
        }
    }

    /**
     * Returns the official character limit for a given social platform.
     */
    public static int getCharacterLimit(SocialPlatform platform) {
        if (platform == SocialPlatform.THREADS) {
            return THREADS_CHAR_LIMIT;
        }
        if (platform == SocialPlatform.LINKEDIN) {
            return LINKEDIN_CHAR_LIMIT;
        }
        return FACEBOOK_CHAR_LIMIT;
    }

    /**
     * Counts Unicode characters accurately (normalizing CRLF to LF and counting
     * Unicode code points so emojis like 🚀 or 🧵 count as 1 character rather than 2 UTF-16 units).
     */
    public static int countCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        return normalized.codePointCount(0, normalized.length());
    }

    /**
     * Evaluates character count, warning/error thresholds, and display label for a platform.
     */
    public static CharCountResult evaluateCharCount(String text, SocialPlatform platform) {
        SocialPlatform safePlatform = platform != null ? platform : SocialPlatform.FACEBOOK;
        int limit = getCharacterLimit(safePlatform);
        int count = countCharacters(text);
        boolean isEmpty = (text == null || text.trim().isEmpty());
        boolean isOverLimit = count > limit;
        int nearThreshold = (int) Math.floor(limit * NEAR_LIMIT_RATIO);
        boolean isNearLimit = !isOverLimit && count >= nearThreshold;

        String channelName = safePlatform.getShortName();
        String formattedCount = String.format(Locale.US, "%,d", count);
        String formattedLimit = String.format(Locale.US, "%,d", limit);

        String label;
        if (isOverLimit) {
            int over = count - limit;
            label = "⚠️ " + formattedCount + " / " + formattedLimit + " ký tự (Vượt " + channelName + " " + over + " ký tự!)";
        } else if (isNearLimit) {
            int left = limit - count;
            label = formattedCount + " / " + formattedLimit + " ký tự (" + channelName + " • còn " + left + ")";
        } else {
            label = formattedCount + " / " + formattedLimit + " ký tự (" + channelName + ")";
        }

        return new CharCountResult(count, limit, isEmpty, isNearLimit, isOverLimit, safePlatform, label);
    }

    /**
     * Checks post text for risky claims, unsourced statistics, or leaked personal info (PII).
     */
    public static SafetyCheckResult checkPostSafety(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new SafetyCheckResult(false, null);
        }

        String lower = content.toLowerCase(Locale.ROOT);
        if (lower.contains("cam kết 100%")
                || lower.contains("làm giàu cấp tốc")
                || lower.contains("chắc chắn kiếm")
                || lower.contains("hoa hồng khủng")) {
            return new SafetyCheckResult(
                    true,
                    "CẢNH BÁO NỘI DUNG: Phát hiện cụm từ cam kết lợi nhuận quá mức có thể vi phạm tiêu chuẩn cộng đồng của Facebook & Threads."
            );
        }

        if (containsPersonalInfo(content)) {
            return new SafetyCheckResult(
                    true,
                    "CẢNH BÁO BẢO MẬT (PII): Bài viết có chứa thông tin cá nhân (SĐT, Email, CCCD/Địa chỉ...). Hãy kiểm tra kỹ trước khi công khai."
            );
        }

        if ((lower.contains("theo thống kê") || lower.contains("số liệu chứng minh") || lower.contains("theo nghiên cứu"))
                && !lower.contains("nguồn") && !lower.contains("trích") && !lower.contains("source")) {
            return new SafetyCheckResult(
                    true,
                    "LƯU Ý: Bài viết có đề cập đến thống kê/nghiên cứu nhưng chưa dẫn nguồn tham chiếu cụ thể."
            );
        }

        return new SafetyCheckResult(false, null);
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
     * Strict Human-in-the-loop Publish Safety Gate for multi-channel Post objects.
     * Guarantees an Agent can NEVER publish a post unless the human explicitly approved it
     * AND all target channels satisfy platform rules (Threads <= 500, Facebook <= 63,206).
     */
    public static PublishGateResult evaluatePublishGate(Post post, boolean explicitHumanApproval) {
        if (post == null || !explicitHumanApproval || !post.isUserApproved()) {
            return new PublishGateResult(
                    false,
                    CODE_MISSING_HUMAN_APPROVAL,
                    "CHỐT CHẶN AN TOÀN: AI Agent tuyệt đối không được tự ý đăng bài khi người dùng chưa bấm xác nhận duyệt!",
                    false,
                    null
            );
        }

        boolean targetFb = post.isTargetFacebook();
        boolean targetThreads = post.isTargetThreads();
        boolean targetLi = post.isTargetLinkedIn();

        if (!targetFb && !targetThreads && !targetLi) {
            return new PublishGateResult(
                    false,
                    CODE_NO_TARGET_CHANNEL,
                    "Vui lòng chọn ít nhất 1 kênh phát hành (Facebook Page hoặc Threads) trước khi đăng.",
                    false,
                    null
            );
        }

        StringBuilder warnings = new StringBuilder();

        if (targetFb) {
            CharCountResult fbCount = evaluateCharCount(post.getContentFacebook(), SocialPlatform.FACEBOOK);
            if (fbCount.isEmpty) {
                return new PublishGateResult(
                        false,
                        CODE_EMPTY_CONTENT,
                        "Nội dung bài viết cho Facebook Page đang trống. Vui lòng kiểm tra trước khi đăng.",
                        false,
                        null
                );
            }
            if (fbCount.isOverLimit) {
                return new PublishGateResult(
                        false,
                        CODE_FACEBOOK_LIMIT_EXCEEDED,
                        "Nội dung Facebook vượt giới hạn " + FACEBOOK_CHAR_LIMIT + " ký tự (hiện có " + fbCount.count + " ký tự).",
                        false,
                        null
                );
            }
            SafetyCheckResult fbSafety = checkPostSafety(post.getContentFacebook());
            if (fbSafety.hasWarning && fbSafety.warningMessage != null) {
                warnings.append("• [Facebook] ").append(fbSafety.warningMessage).append("\n");
            }
        }

        if (targetThreads) {
            CharCountResult threadsCount = evaluateCharCount(post.getContentThreads(), SocialPlatform.THREADS);
            if (threadsCount.isEmpty) {
                return new PublishGateResult(
                        false,
                        CODE_EMPTY_CONTENT,
                        "Nội dung bài viết cho Threads đang trống. Vui lòng kiểm tra trước khi đăng.",
                        false,
                        null
                );
            }
            if (threadsCount.isOverLimit) {
                return new PublishGateResult(
                        false,
                        CODE_THREADS_LIMIT_EXCEEDED,
                        "Nội dung Threads đang dài " + threadsCount.count + " / " + THREADS_CHAR_LIMIT
                                + " ký tự (vượt " + threadsCount.overflow + " ký tự). Vui lòng rút gọn hoặc bấm 'Ngắn hơn' trước khi đăng!",
                        false,
                        null
                );
            }
            SafetyCheckResult threadsSafety = checkPostSafety(post.getContentThreads());
            if (threadsSafety.hasWarning && threadsSafety.warningMessage != null) {
                warnings.append("• [Threads] ").append(threadsSafety.warningMessage).append("\n");
            }
        }

        if (targetLi) {
            CharCountResult liCount = evaluateCharCount(post.getContentLinkedIn(), SocialPlatform.LINKEDIN);
            if (liCount.isEmpty) {
                return new PublishGateResult(
                        false,
                        CODE_EMPTY_CONTENT,
                        "Nội dung bài viết cho LinkedIn đang trống.",
                        false,
                        null
                );
            }
            if (liCount.isOverLimit) {
                return new PublishGateResult(
                        false,
                        CODE_LINKEDIN_LIMIT_EXCEEDED,
                        "Nội dung LinkedIn vượt giới hạn " + LINKEDIN_CHAR_LIMIT + " ký tự.",
                        false,
                        null
                );
            }
        }

        String warningStr = warnings.toString().trim();
        return new PublishGateResult(
                true,
                CODE_APPROVED_READY,
                null,
                !warningStr.isEmpty(),
                warningStr.isEmpty() ? null : warningStr
        );
    }

    /**
     * Strict Human-in-the-loop Publish Safety Gate for a single platform (e.g. Preview dialog or direct API publish).
     */
    public static PublishGateResult evaluateSingleChannelPublishGate(String content,
                                                                     SocialPlatform platform,
                                                                     boolean explicitHumanApproval) {
        if (!explicitHumanApproval) {
            return new PublishGateResult(
                    false,
                    CODE_MISSING_HUMAN_APPROVAL,
                    "CHỐT CHẶN AN TOÀN: AI Agent tuyệt đối không được tự ý đăng bài khi người dùng chưa bấm xác nhận duyệt!",
                    false,
                    null
            );
        }

        SocialPlatform safePlatform = platform != null ? platform : SocialPlatform.FACEBOOK;
        CharCountResult countResult = evaluateCharCount(content, safePlatform);

        if (countResult.isEmpty) {
            return new PublishGateResult(
                    false,
                    CODE_EMPTY_CONTENT,
                    "Nội dung bài viết đang trống, không thể xuất bản!",
                    false,
                    null
            );
        }

        if (countResult.isOverLimit) {
            String code = (safePlatform == SocialPlatform.THREADS)
                    ? CODE_THREADS_LIMIT_EXCEEDED
                    : (safePlatform == SocialPlatform.LINKEDIN ? CODE_LINKEDIN_LIMIT_EXCEEDED : CODE_FACEBOOK_LIMIT_EXCEEDED);
            return new PublishGateResult(
                    false,
                    code,
                    "Nội dung " + safePlatform.getShortName() + " dài " + countResult.count + " / " + countResult.limit
                            + " ký tự (vượt " + countResult.overflow + " ký tự). Hãy chỉnh sửa hoặc dùng nút 'Ngắn hơn' trước khi đăng!",
                    false,
                    null
            );
        }

        SafetyCheckResult safety = checkPostSafety(content);
        return new PublishGateResult(
                true,
                CODE_APPROVED_READY,
                null,
                safety.hasWarning,
                safety.warningMessage
        );
    }

    /**
     * System prompt for real LLM section/post rewrite operations.
     */
    public static String buildRewriteSystemPrompt(SocialPlatform platform) {
        SocialPlatform safePlatform = platform != null ? platform : SocialPlatform.FACEBOOK;
        String platformRule = (safePlatform == SocialPlatform.THREADS)
                ? "Nền tảng đích: Meta Threads. QUY TẮC BẮT BUỘC: Tổng bài viết sau khi viết lại KHÔNG ĐƯỢC VƯỢT QUÁ 500 ký tự (ưu tiên 220–430 ký tự), giọng điệu tự nhiên, sắc bén, dễ khơi gợi thảo luận."
                : "Nền tảng đích: Facebook Page (giới hạn 63,206 ký tự). Chia đoạn thoáng mắt, dễ đọc trên điện thoại, giữ được nhịp điệu thu hút tương tác.";

        return "Bạn là biên tập viên mạng xã hội (Social Media Copywriter) cao cấp.\n" +
                platformRule + "\n" +
                "Quy tắc đầu ra NGHIÊM NGẶT:\n" +
                "1. CHỈ trả về đúng phần nội dung đã viết lại.\n" +
                "2. KHÔNG thêm lời dẫn (như 'Dưới đây là...', 'Bản viết lại:'), KHÔNG giải thích, KHÔNG bọc trong dấu ngoặc kép hoặc markdown code fence (```).\n" +
                "3. Giữ nguyên ngôn ngữ gốc của bài viết (Tiếng Việt hoặc English) và đảm bảo thông tin trung thực, không tự bịa số liệu.";
    }

    /**
     * User prompt for real LLM rewrite of either a selected section or the full post.
     */
    public static String buildRewriteUserPrompt(String fullText,
                                                String selectedSection,
                                                String instructionType,
                                                SocialPlatform platform,
                                                String language) {
        SocialPlatform safePlatform = platform != null ? platform : SocialPlatform.FACEBOOK;
        String langLabel = (language != null && !language.trim().isEmpty()) ? language.trim() : "theo ngôn ngữ gốc của văn bản";
        boolean hasSelection = selectedSection != null && !selectedSection.trim().isEmpty()
                && fullText != null && !selectedSection.trim().equals(fullText.trim());

        String taskInstruction;
        if (REWRITE_SHORTER.equalsIgnoreCase(instructionType)) {
            if (safePlatform == SocialPlatform.THREADS) {
                taskInstruction = "Hãy rút gọn súc tích hơn (giảm 30–50% độ dài), giữ trọn ý chính và câu hỏi tương tác. BẮT BUỘC độ dài tổng bài viết dưới 450 ký tự (tuyệt đối <= 500 ký tự).";
            } else {
                taskInstruction = "Hãy rút gọn súc tích hơn (giảm 30–50% độ dài), loại bỏ câu chữ rườm rà nhưng giữ nguyên thông điệp cốt lõi, các ý giá trị nhất và lời kêu gọi tương tác.";
            }
        } else if (REWRITE_FORMAL.equalsIgnoreCase(instructionType)) {
            taskInstruction = "Hãy chuyển sang giọng văn trang trọng, chuyên nghiệp, chuẩn mực (phong cách chuyên gia / doanh nghiệp), lược bỏ từ ngữ suồng sã và emoji thừa nhưng vẫn mạch lạc và thuyết phục."
                    + (safePlatform == SocialPlatform.THREADS ? " Giữ tổng độ dài <= 480 ký tự." : "");
        } else if (REWRITE_HOOK.equalsIgnoreCase(instructionType)) {
            taskInstruction = "Hãy đổi câu mở đầu (Hook — 1 đến 2 câu/dòng đầu tiên) thật mới mẻ, sắc bén và cuốn hút để giữ chân người đọc ngay lập tức; GIỮ NGUYÊN ý chính của phần thân bài và lời kết phía sau (chỉ chỉnh nhẹ câu nối nếu cần)."
                    + (safePlatform == SocialPlatform.THREADS ? " Đảm bảo tổng độ dài toàn bài <= 480 ký tự." : "");
        } else {
            taskInstruction = "Hãy biên tập lại cho mượt mà, tự nhiên và rõ ràng hơn.";
        }

        if (hasSelection && !REWRITE_HOOK.equalsIgnoreCase(instructionType)) {
            return "Ngôn ngữ: " + langLabel + "\n" +
                    "Kênh: " + safePlatform.getDisplayName() + "\n" +
                    "Yêu cầu biên tập RIÊNG ĐOẠN ĐƯỢC CHỌN: " + taskInstruction + "\n" +
                    "CHỈ trả về đúng đoạn văn đã được viết lại để thay thế trực tiếp vào vị trí đoạn được chọn (không lặp lại phần còn lại của bài).\n\n" +
                    "--- NGỮ CẢNH TOÀN BÀI ---\n" + fullText + "\n\n" +
                    "--- ĐOẠN CẦN VIẾT LẠI ---\n" + selectedSection;
        }

        return "Ngôn ngữ: " + langLabel + "\n" +
                "Kênh: " + safePlatform.getDisplayName() + "\n" +
                "Yêu cầu: " + taskInstruction + "\n\n" +
                "--- NỘI DUNG GỐC ---\n" + (fullText != null ? fullText : "");
    }

    /**
     * Cleans LLM rewrite output from accidental markdown fences, preambles, or surrounding quotes.
     */
    public static String sanitizeLlmRewriteOutput(String rawOutput,
                                                  SocialPlatform platform,
                                                  String instructionType) {
        if (rawOutput == null) {
            return "";
        }
        String text = rawOutput.trim();

        // Strip markdown code fences if model wrapped response
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline > 0) {
                text = text.substring(firstNewline + 1);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
            text = text.trim();
        }

        // Strip common conversational preambles on the first line
        String[] lines = text.split("\n", 2);
        if (lines.length == 2) {
            String firstLineLower = lines[0].trim().toLowerCase(Locale.ROOT);
            if ((firstLineLower.startsWith("dưới đây là")
                    || firstLineLower.startsWith("bản viết lại")
                    || firstLineLower.startsWith("nội dung đã viết lại")
                    || firstLineLower.startsWith("here is")
                    || firstLineLower.startsWith("here's")
                    || firstLineLower.startsWith("rewritten"))
                    && firstLineLower.endsWith(":")) {
                text = lines[1].trim();
            }
        }

        // Strip wrapping quotation marks if whole response is quoted
        if (text.length() > 2 && text.startsWith("\"") && text.endsWith("\"")) {
            text = text.substring(1, text.length() - 1).trim();
        }

        // Ensure Threads SHORTER stays strictly within 500 Unicode characters
        if (platform == SocialPlatform.THREADS && REWRITE_SHORTER.equalsIgnoreCase(instructionType)) {
            text = trimToCodePointLimit(text, THREADS_CHAR_LIMIT);
        }

        return text;
    }

    /**
     * Safely replaces a selected text range inside fullText, or replaces the entire text if no valid selection.
     */
    public static String applySectionReplacement(String fullText, int selStart, int selEnd, String rewrittenSegment) {
        String replacement = rewrittenSegment != null ? rewrittenSegment : "";
        if (fullText == null || fullText.isEmpty()) {
            return replacement;
        }
        int start = Math.min(selStart, selEnd);
        int end = Math.max(selStart, selEnd);
        if (start < 0 || end <= start || end > fullText.length()) {
            return replacement;
        }
        if (start == 0 && end == fullText.length()) {
            return replacement;
        }
        return fullText.substring(0, start) + replacement + fullText.substring(end);
    }

    /**
     * Deterministic smart rewrite used ONLY when the user explicitly enables Demo Mode.
     */
    public static String rewriteLocallyForDemo(String currentText,
                                               String instructionType,
                                               SocialPlatform platform,
                                               String language) {
        if (currentText == null || currentText.trim().isEmpty()) {
            return "";
        }
        String text = currentText.trim();
        boolean isEnglish = "English".equalsIgnoreCase(language) || looksLikeEnglish(text);
        int limit = getCharacterLimit(platform);

        if (REWRITE_SHORTER.equalsIgnoreCase(instructionType)) {
            String[] paragraphs = text.split("\\n\\s*\\n");
            StringBuilder sb = new StringBuilder();
            int targetBudget = (platform == SocialPlatform.THREADS)
                    ? Math.min(340, Math.max(120, (int) (countCharacters(text) * 0.65)))
                    : Math.max(160, (int) (countCharacters(text) * 0.60));

            for (String p : paragraphs) {
                String cleanP = p.trim();
                if (cleanP.isEmpty()) continue;
                if (sb.length() == 0) {
                    sb.append(cleanP);
                } else if (countCharacters(sb.toString()) + countCharacters(cleanP) + 2 <= targetBudget) {
                    sb.append("\n\n").append(cleanP);
                } else {
                    break;
                }
            }
            String condensed = sb.toString().trim();
            if (countCharacters(condensed) >= countCharacters(text) && countCharacters(text) > 40) {
                int shortBudget = Math.max(30, Math.min(targetBudget, (int) (countCharacters(text) * 0.65)));
                condensed = trimToCodePointLimit(text, shortBudget);
            }
            if (platform == SocialPlatform.THREADS) {
                condensed = trimToCodePointLimit(condensed, THREADS_CHAR_LIMIT);
            }
            return condensed;
        }

        if (REWRITE_FORMAL.equalsIgnoreCase(instructionType)) {
            String cleaned = text
                    .replace("🔥", "")
                    .replace("😂", "")
                    .replace("👀", "")
                    .replace("anh em ơi", "quý độc giả")
                    .replace("Anh em", "Quý độc giả")
                    .replace("anh em", "quý độc giả")
                    .replace("cả nhà", "quý độc giả")
                    .replace("Cả nhà", "Quý độc giả")
                    .replace("chém gió", "trao đổi chuyên môn")
                    .replace("nhàn hẳn", "tối ưu đáng kể")
                    .replaceAll("[ \\t]{2,}", " ")
                    .trim();

            String result;
            if (isEnglish) {
                result = cleaned.startsWith("Dear") ? cleaned
                        : "Key Executive Perspective:\n\n" + cleaned + "\n\nWe welcome your professional insights below.";
            } else {
                result = cleaned.startsWith("Kính gửi") ? cleaned
                        : "Kính gửi quý độc giả và đồng nghiệp,\n\n" + cleaned + "\n\nTrân trọng kính mời quý vị cùng chia sẻ góc nhìn chuyên môn.";
            }
            if (platform == SocialPlatform.THREADS) {
                result = trimToCodePointLimit(result, THREADS_CHAR_LIMIT);
            }
            return result;
        }

        if (REWRITE_HOOK.equalsIgnoreCase(instructionType)) {
            int firstNewline = text.indexOf('\n');
            String body = firstNewline >= 0 ? text.substring(firstNewline).trim() : text;
            String[] pool = isEnglish ? DEMO_HOOKS_EN : DEMO_HOOKS_VI;
            String chosenHook = pool[0];
            for (String candidate : pool) {
                if (!text.startsWith(candidate)) {
                    chosenHook = candidate;
                    break;
                }
            }
            String result = body.isEmpty() ? chosenHook : chosenHook + "\n\n" + body;
            if (platform == SocialPlatform.THREADS && countCharacters(result) > limit) {
                result = trimToCodePointLimit(result, THREADS_CHAR_LIMIT);
            }
            return result;
        }

        return text;
    }

    /**
     * Prompt for LLM Visual Agent to craft a brand-new image description distinct from previousPrompt.
     */
    public static String buildImagePromptUserPrompt(String topic,
                                                    String postContent,
                                                    SocialPlatform platform,
                                                    String previousPrompt,
                                                    int variantIndex) {
        SocialPlatform safePlatform = platform != null ? platform : SocialPlatform.FACEBOOK;
        String aspect = (safePlatform == SocialPlatform.THREADS) ? "1:1 square" : "16:9 widescreen";
        String snippet = postContent != null ? postContent.trim() : "";
        if (snippet.length() > 350) {
            snippet = snippet.substring(0, 350) + "...";
        }
        String presetHint = IMAGE_STYLE_PRESETS[Math.abs(variantIndex) % IMAGE_STYLE_PRESETS.length];

        return "Create ONE concise, vivid English image-generation prompt (max 45 words) for a social media illustration.\n" +
                "- Topic: " + (topic != null && !topic.trim().isEmpty() ? topic.trim() : "Social media insight") + "\n" +
                "- Post excerpt: " + snippet + "\n" +
                "- Target channel format: " + safePlatform.getDisplayName() + " (" + aspect + ")\n" +
                "- Preferred new visual direction: " + presetHint + "\n" +
                (previousPrompt != null && !previousPrompt.trim().isEmpty()
                        ? "- IMPORTANT: Make the visual composition and color mood clearly DIFFERENT from this previous prompt: \"" + previousPrompt + "\"\n"
                        : "") +
                "- Rules: No text/watermarks inside the artwork. Output ONLY the English prompt sentence, nothing else.";
    }

    /**
     * Deterministic varied visual prompt used as fallback or in Demo mode.
     */
    public static String buildFallbackImagePrompt(String topic,
                                                  String postContent,
                                                  SocialPlatform platform,
                                                  int variantIndex) {
        SocialPlatform safePlatform = platform != null ? platform : SocialPlatform.FACEBOOK;
        String safeTopic = (topic != null && !topic.trim().isEmpty())
                ? topic.trim()
                : extractShortHeadline(postContent, "AI Social Strategy");
        String aspect = (safePlatform == SocialPlatform.THREADS) ? "1:1 Threads card" : "16:9 Facebook cover";
        String style = IMAGE_STYLE_PRESETS[Math.abs(variantIndex) % IMAGE_STYLE_PRESETS.length];
        return "Concept #" + (Math.abs(variantIndex) + 1) + " (" + aspect + "): " + style + " illustrating \"" + safeTopic + "\"";
    }

    public static String sanitizeImagePromptOutput(String rawOutput, String fallbackPrompt) {
        if (rawOutput == null || rawOutput.trim().isEmpty()) {
            return fallbackPrompt;
        }
        String cleaned = sanitizeLlmRewriteOutput(rawOutput, SocialPlatform.FACEBOOK, "");
        cleaned = cleaned.replace("\n", " ").replaceAll("\\s{2,}", " ").trim();
        if (cleaned.isEmpty()) {
            return fallbackPrompt;
        }
        if (cleaned.length() > 260) {
            cleaned = cleaned.substring(0, 257).trim() + "...";
        }
        return cleaned;
    }

    public static String getInstructionDisplayName(String instructionType) {
        if (REWRITE_SHORTER.equalsIgnoreCase(instructionType)) return "Ngắn hơn";
        if (REWRITE_FORMAL.equalsIgnoreCase(instructionType)) return "Trang trọng hơn";
        if (REWRITE_HOOK.equalsIgnoreCase(instructionType)) return "Đổi Hook mở đầu";
        return "Viết lại";
    }

    public static String trimToCodePointLimit(String text, int maxCodePoints) {
        if (text == null || maxCodePoints <= 0) {
            return "";
        }
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        if (normalized.codePointCount(0, normalized.length()) <= maxCodePoints) {
            return normalized;
        }
        int targetPoints = Math.max(1, maxCodePoints - 3);
        int endIndex = normalized.offsetByCodePoints(0, targetPoints);
        String cut = normalized.substring(0, endIndex).trim();
        int lastSpace = cut.lastIndexOf(' ');
        if (lastSpace > cut.length() * 0.7) {
            cut = cut.substring(0, lastSpace).trim();
        }
        return cut + "...";
    }

    private static String extractShortHeadline(String content, String fallback) {
        if (content == null || content.trim().isEmpty()) {
            return fallback;
        }
        String firstLine = content.trim().split("\n")[0].trim();
        if (firstLine.length() > 60) {
            return firstLine.substring(0, 57).trim() + "...";
        }
        return firstLine.isEmpty() ? fallback : firstLine;
    }

    private static boolean looksLikeEnglish(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        int vietnameseMarkers = 0;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if ("ăâđêôơưàáạảãèéẹẻẽìíịỉĩòóọỏõùúụủũỳýỵỷỹ".indexOf(c) >= 0) {
                vietnameseMarkers++;
            }
        }
        return vietnameseMarkers == 0;
    }
}
