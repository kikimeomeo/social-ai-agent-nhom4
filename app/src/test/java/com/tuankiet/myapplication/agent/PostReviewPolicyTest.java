package com.tuankiet.myapplication.agent;

import com.tuankiet.myapplication.model.Post;
import com.tuankiet.myapplication.model.SocialPlatform;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PostReviewPolicyTest {

    private static String repeatChar(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    // --- 1. Accurate Character Counting (Threads 500 & Facebook 63,206) ---

    @Test
    public void charCounter_enforcesExactThreads500Boundary() {
        PostReviewPolicy.CharCountResult empty = PostReviewPolicy.evaluateCharCount("", SocialPlatform.THREADS);
        assertEquals(0, empty.count);
        assertEquals(500, empty.limit);
        assertTrue(empty.isEmpty);
        assertFalse(empty.isOverLimit);

        String exact500 = repeatChar('a', 500);
        PostReviewPolicy.CharCountResult atLimit = PostReviewPolicy.evaluateCharCount(exact500, SocialPlatform.THREADS);
        assertEquals(500, atLimit.count);
        assertEquals(0, atLimit.remaining);
        assertEquals(0, atLimit.overflow);
        assertFalse(atLimit.isOverLimit);
        assertTrue(atLimit.isNearLimit);

        String over501 = repeatChar('a', 501);
        PostReviewPolicy.CharCountResult over = PostReviewPolicy.evaluateCharCount(over501, SocialPlatform.THREADS);
        assertEquals(501, over.count);
        assertEquals(1, over.overflow);
        assertTrue(over.isOverLimit);
        assertTrue(over.formattedCounter.contains("501 / 500"));
        assertTrue(over.formattedCounter.contains("Vượt Threads 1"));
    }

    @Test
    public void charCounter_enforcesExactFacebook63206Boundary() {
        String exactFb = repeatChar('b', PostReviewPolicy.FACEBOOK_CHAR_LIMIT);
        PostReviewPolicy.CharCountResult atLimit = PostReviewPolicy.evaluateCharCount(exactFb, SocialPlatform.FACEBOOK);
        assertEquals(63206, atLimit.count);
        assertEquals(63206, atLimit.limit);
        assertFalse(atLimit.isOverLimit);
        assertTrue(atLimit.formattedCounter.contains("63,206"));

        String overFb = repeatChar('b', PostReviewPolicy.FACEBOOK_CHAR_LIMIT + 5);
        PostReviewPolicy.CharCountResult over = PostReviewPolicy.evaluateCharCount(overFb, SocialPlatform.FACEBOOK);
        assertTrue(over.isOverLimit);
        assertEquals(5, over.overflow);
        assertTrue(over.formattedCounter.contains("Facebook"));
    }

    @Test
    public void charCounter_countsUnicodeEmojisVietnameseAndCrlfAccurately() {
        // 🚀 and 🧵 are surrogate pairs in UTF-16 (length() == 2 each) but 1 Unicode character each
        String withEmojis = "Xin chào 🚀🧵";
        assertEquals(11, PostReviewPolicy.countCharacters(withEmojis));

        // CRLF should be counted as a single newline character, not 2
        String withCrlf = "Dòng 1\r\nDòng 2";
        assertEquals(13, PostReviewPolicy.countCharacters(withCrlf));
    }

    // --- 2. Human-in-the-loop Safety Gate (Agent MUST NEVER auto-publish) ---

    @Test
    public void safetyGate_strictlyBlocksPublishingWithoutExplicitHumanApproval() {
        Post draft = new Post();
        draft.setTargetFacebook(true);
        draft.setTargetThreads(true);
        draft.setContentFacebook("Bài viết Facebook hợp lệ cho cộng đồng AI.");
        draft.setContentThreads("Bài viết Threads ngắn gọn hợp lệ.");
        draft.setStatus(Post.STATUS_DRAFT);
        // Default post.isUserApproved() MUST be false
        assertFalse(draft.isUserApproved());

        // Even if someone passes explicitHumanApproval=false, gate MUST block
        PostReviewPolicy.PublishGateResult agentAttempt = PostReviewPolicy.evaluatePublishGate(draft, false);
        assertFalse(agentAttempt.allowed);
        assertEquals(PostReviewPolicy.CODE_MISSING_HUMAN_APPROVAL, agentAttempt.code);

        // Even if explicitHumanApproval=true, if post.isUserApproved() is still false, gate MUST block
        PostReviewPolicy.PublishGateResult unapprovedPostAttempt = PostReviewPolicy.evaluatePublishGate(draft, true);
        assertFalse(unapprovedPostAttempt.allowed);
        assertEquals(PostReviewPolicy.CODE_MISSING_HUMAN_APPROVAL, unapprovedPostAttempt.code);

        // Single-channel gate also blocks without explicit human approval
        PostReviewPolicy.PublishGateResult singleAgentAttempt =
                PostReviewPolicy.evaluateSingleChannelPublishGate("Bài viết", SocialPlatform.THREADS, false);
        assertFalse(singleAgentAttempt.allowed);
        assertEquals(PostReviewPolicy.CODE_MISSING_HUMAN_APPROVAL, singleAgentAttempt.code);
    }

    @Test
    public void safetyGate_blocksThreadsOver500EvenWhenUserApproved() {
        Post post = new Post();
        post.setTargetFacebook(true);
        post.setTargetThreads(true);
        post.setContentFacebook("Nội dung Facebook hợp lệ.");
        post.setContentThreads(repeatChar('x', 520));
        post.setUserApproved(true);

        PostReviewPolicy.PublishGateResult result = PostReviewPolicy.evaluatePublishGate(post, true);
        assertFalse(result.allowed);
        assertEquals(PostReviewPolicy.CODE_THREADS_LIMIT_EXCEEDED, result.code);
        assertTrue(result.blockReason.contains("520"));
        assertTrue(result.blockReason.contains("500"));
    }

    @Test
    public void safetyGate_allowsApprovedValidMultiChannelPostAndSurfacesSafetyWarnings() {
        Post post = new Post();
        post.setTargetFacebook(true);
        post.setTargetThreads(true);
        post.setContentFacebook("Theo thống kê năm 2026, AI Agent tăng 300% năng suất làm việc.");
        post.setContentThreads("AI Agent giúp tiết kiệm thời gian mỗi ngày. Bạn nghĩ sao? 👇");
        post.setUserApproved(true);

        PostReviewPolicy.PublishGateResult result = PostReviewPolicy.evaluatePublishGate(post, true);
        assertTrue(result.allowed);
        assertEquals(PostReviewPolicy.CODE_APPROVED_READY, result.code);
        assertTrue("Unsourced statistics should surface a human review warning", result.hasSafetyWarning);
        assertNotNull(result.safetyWarningMessage);
    }

    @Test
    public void safetyChecker_detectsPiiAndRiskyClaims() {
        PostReviewPolicy.SafetyCheckResult piiCheck = PostReviewPolicy.checkPostSafety(
                "Liên hệ ngay số 0912345678 hoặc email admin@company.vn để nhận tài liệu."
        );
        assertTrue(piiCheck.hasWarning);
        assertTrue(piiCheck.warningMessage.contains("PII"));

        PostReviewPolicy.SafetyCheckResult scamCheck = PostReviewPolicy.checkPostSafety(
                "Tham gia ngay khóa học làm giàu cấp tốc cam kết 100% lợi nhuận!"
        );
        assertTrue(scamCheck.hasWarning);
    }

    // --- 3. Real LLM Rewrite Prompting, Output Sanitization & Partial-Section Replacement ---

    @Test
    public void rewritePrompts_includeInstructionPlatformConstraintsAndSelectedSection() {
        String shorterThreadsPrompt = PostReviewPolicy.buildRewriteUserPrompt(
                "Đoạn 1 dài.\n\nĐoạn 2 rất dài cần rút gọn lại.",
                "Đoạn 2 rất dài cần rút gọn lại.",
                PostReviewPolicy.REWRITE_SHORTER,
                SocialPlatform.THREADS,
                "Tiếng Việt"
        );
        assertTrue(shorterThreadsPrompt.contains("500 ký tự"));
        assertTrue(shorterThreadsPrompt.contains("ĐOẠN CẦN VIẾT LẠI"));
        assertTrue(shorterThreadsPrompt.contains("Đoạn 2 rất dài"));

        String formalFbPrompt = PostReviewPolicy.buildRewriteUserPrompt(
                "Anh em ơi tool này ngon lắm!",
                null,
                PostReviewPolicy.REWRITE_FORMAL,
                SocialPlatform.FACEBOOK,
                "Tiếng Việt"
        );
        assertTrue(formalFbPrompt.contains("trang trọng"));
        assertTrue(formalFbPrompt.contains("Facebook Page"));

        String hookPrompt = PostReviewPolicy.buildRewriteUserPrompt(
                "Hôm nay nói về AI.\n\nAI đang thay đổi mọi thứ.",
                null,
                PostReviewPolicy.REWRITE_HOOK,
                SocialPlatform.THREADS,
                "Tiếng Việt"
        );
        assertTrue(hookPrompt.contains("Hook"));
    }

    @Test
    public void sanitizeLlmRewriteOutput_cleansCodeFencesPreambleQuotesAndEnforcesThreadsShorterLimit() {
        String messy = "```text\nDưới đây là bản viết lại:\n\"Nội dung sạch sẽ và súc tích hơn.\"\n```";
        String cleaned = PostReviewPolicy.sanitizeLlmRewriteOutput(
                messy, SocialPlatform.FACEBOOK, PostReviewPolicy.REWRITE_FORMAL
        );
        assertEquals("Nội dung sạch sẽ và súc tích hơn.", cleaned);

        String tooLongThreads = repeatChar('a', 600);
        String capped = PostReviewPolicy.sanitizeLlmRewriteOutput(
                tooLongThreads, SocialPlatform.THREADS, PostReviewPolicy.REWRITE_SHORTER
        );
        assertTrue(PostReviewPolicy.countCharacters(capped) <= PostReviewPolicy.THREADS_CHAR_LIMIT);
    }

    @Test
    public void applySectionReplacement_splicesSelectedParagraphWithoutCorruptingSurroundingText() {
        String original = "Mở đầu hay.\n\nĐoạn giữa hơi dài dòng.\n\nKết bài CTA.";
        int start = original.indexOf("Đoạn giữa");
        int end = start + "Đoạn giữa hơi dài dòng.".length();
        String updated = PostReviewPolicy.applySectionReplacement(original, start, end, "Đoạn giữa súc tích.");
        assertEquals("Mở đầu hay.\n\nĐoạn giữa súc tích.\n\nKết bài CTA.", updated);
    }

    @Test
    public void demoRewrite_handlesShorterFormalAndHookWithinThreadsLimit() {
        String longThreads = repeatChar('a', 260) + "\n\n" + repeatChar('b', 280);
        String shorter = PostReviewPolicy.rewriteLocallyForDemo(
                longThreads, PostReviewPolicy.REWRITE_SHORTER, SocialPlatform.THREADS, "Tiếng Việt"
        );
        assertTrue(PostReviewPolicy.countCharacters(shorter) <= PostReviewPolicy.THREADS_CHAR_LIMIT);
        assertTrue(PostReviewPolicy.countCharacters(shorter) < PostReviewPolicy.countCharacters(longThreads));

        String formal = PostReviewPolicy.rewriteLocallyForDemo(
                "Anh em ơi AI năm nay đỉnh quá 🔥", PostReviewPolicy.REWRITE_FORMAL, SocialPlatform.FACEBOOK, "Tiếng Việt"
        );
        assertTrue(formal.contains("quý độc giả") || formal.contains("Kính gửi"));
        assertFalse(formal.contains("🔥"));

        String hook = PostReviewPolicy.rewriteLocallyForDemo(
                "Mở đầu cũ.\n\nThân bài quan trọng giữ nguyên.", PostReviewPolicy.REWRITE_HOOK, SocialPlatform.FACEBOOK, "Tiếng Việt"
        );
        assertTrue(hook.contains("Thân bài quan trọng giữ nguyên."));
        assertFalse(hook.startsWith("Mở đầu cũ."));
    }

    // --- 4. Image Regeneration Prompt Diversity ---

    @Test
    public void imagePromptGenerator_producesDistinctConceptsAcrossVariantsAndPlatforms() {
        String v0 = PostReviewPolicy.buildFallbackImagePrompt("AI 2026", "Nội dung bài viết", SocialPlatform.FACEBOOK, 0);
        String v1 = PostReviewPolicy.buildFallbackImagePrompt("AI 2026", "Nội dung bài viết", SocialPlatform.FACEBOOK, 1);
        String threadsV1 = PostReviewPolicy.buildFallbackImagePrompt("AI 2026", "Nội dung bài viết", SocialPlatform.THREADS, 1);

        assertNotEquals(v0, v1);
        assertTrue(v0.contains("16:9"));
        assertTrue(threadsV1.contains("1:1"));

        String llmPrompt = PostReviewPolicy.buildImagePromptUserPrompt("AI 2026", "Nội dung", SocialPlatform.THREADS, v0, 1);
        assertTrue(llmPrompt.contains("DIFFERENT"));
        assertTrue(llmPrompt.contains("1:1 square"));
    }
}
