package com.tuankiet.myapplication.agent;

import com.tuankiet.myapplication.model.CommentItem;
import com.tuankiet.myapplication.model.SocialPlatform;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CommentSafetyPolicyTest {

    @Test
    public void localAnalysis_classifiesAllFiveCategories() {
        CommentSafetyPolicy.CommentAnalysis praise = CommentSafetyPolicy.analyzeCommentLocally(
                SocialPlatform.FACEBOOK,
                "5 Xu Hướng AI 2026",
                "Nội dung bài viết về AI",
                "Trần Minh Khoa",
                "Bài viết rất sâu sắc và hữu ích, cảm ơn tác giả nhiều!"
        );
        assertEquals(CommentItem.CATEGORY_PRAISE, praise.category);
        assertFalse(praise.hasPersonalInfo);
        assertTrue(praise.confidence >= CommentSafetyPolicy.MIN_AUTO_REPLY_CONFIDENCE);
        assertNotNull(praise.suggestedReply);
        assertTrue(praise.suggestedReply.contains("5 Xu Hướng AI 2026"));

        CommentSafetyPolicy.CommentAnalysis faqQuestion = CommentSafetyPolicy.analyzeCommentLocally(
                SocialPlatform.THREADS,
                "5 Xu Hướng AI 2026",
                "Ứng dụng hỗ trợ BYOK",
                "@lehuong",
                "Cho em hỏi app có hỗ trợ nhập Gemini API Key miễn phí không ạ?"
        );
        assertEquals(CommentItem.CATEGORY_QUESTION, faqQuestion.category);
        assertTrue(faqQuestion.isFaq);
        assertTrue(faqQuestion.confidence >= CommentSafetyPolicy.MIN_AUTO_REPLY_CONFIDENCE);
        assertNotNull(faqQuestion.suggestedReply);

        CommentSafetyPolicy.CommentAnalysis suggestion = CommentSafetyPolicy.analyzeCommentLocally(
                SocialPlatform.FACEBOOK,
                "On-Device AI",
                "So sánh hiệu năng",
                "Phạm Quốc Khánh",
                "Góp ý thêm là bạn nên bổ sung bảng so sánh độ trễ giữa các model nhé!"
        );
        assertEquals(CommentItem.CATEGORY_SUGGESTION, suggestion.category);
        assertNotNull(suggestion.suggestedReply);
        assertNotNull(suggestion.autoReplyBlockedReason);

        CommentSafetyPolicy.CommentAnalysis negative = CommentSafetyPolicy.analyzeCommentLocally(
                SocialPlatform.THREADS,
                "On-Device AI",
                "Nội dung bài viết",
                "@critic",
                "Nội dung này nhìn giống copy và nói suông, rất tệ!"
        );
        assertEquals(CommentItem.CATEGORY_NEGATIVE, negative.category);
        assertTrue(negative.negativeFlagged);
        assertNotNull(negative.riskWarning);

        CommentSafetyPolicy.CommentAnalysis spam = CommentSafetyPolicy.analyzeCommentLocally(
                SocialPlatform.FACEBOOK,
                "5 Xu Hướng AI 2026",
                "",
                "Bot 24/7",
                "Bấm vào link bit.ly/spam-link để nhận khóa học làm giàu cấp tốc miễn phí 100%!"
        );
        assertEquals(CommentItem.CATEGORY_SPAM, spam.category);
        assertTrue(spam.negativeFlagged);
        assertNull(spam.suggestedReply);
    }

    @Test
    public void containsPersonalInfo_detectsVietnamesePiiWithoutFalsePositives() {
        assertTrue(CommentSafetyPolicy.containsPersonalInfo("Gọi mình số 0912345678 nhé"));
        assertTrue(CommentSafetyPolicy.containsPersonalInfo("SĐT của em là +84 987 654 321"));
        assertTrue(CommentSafetyPolicy.containsPersonalInfo("Gửi tài liệu qua minh.nguyen@gmail.com giúp mình"));
        assertTrue(CommentSafetyPolicy.containsPersonalInfo("CCCD của mình là 079203012345"));
        assertTrue(CommentSafetyPolicy.containsPersonalInfo("Chuyển khoản vào số tài khoản ngân hàng mình"));
        assertTrue(CommentSafetyPolicy.containsPersonalInfo("Giao hàng tới số 123 đường Nguyễn Huệ nhé"));

        assertFalse(CommentSafetyPolicy.containsPersonalInfo("Bài viết rất hay, cảm ơn team năm 2026!"));
        assertFalse(CommentSafetyPolicy.containsPersonalInfo("App hỗ trợ tối đa 500 ký tự trên Threads phải không?"));
    }

    @Test
    public void controlledAutoReply_allowsOnlySafePraiseAndFaq() {
        CommentItem safePraise = new CommentItem();
        safePraise.setCategory(CommentItem.CATEGORY_PRAISE);
        safePraise.setContent("Bài viết tuyệt vời quá!");
        safePraise.setAiConfidence(0.95);
        CommentSafetyPolicy.AutoReplyDecision praiseDecision = CommentSafetyPolicy.evaluateAutoReply(safePraise);
        assertTrue(praiseDecision.allowed);
        assertEquals(CommentSafetyPolicy.CODE_ALLOWED_PRAISE, praiseDecision.reasonCode);

        CommentItem safeFaq = new CommentItem();
        safeFaq.setCategory(CommentItem.CATEGORY_QUESTION);
        safeFaq.setFaq(true);
        safeFaq.setContent("App có dùng thử miễn phí không?");
        safeFaq.setAiConfidence(0.90);
        CommentSafetyPolicy.AutoReplyDecision faqDecision = CommentSafetyPolicy.evaluateAutoReply(safeFaq);
        assertTrue(faqDecision.allowed);
        assertEquals(CommentSafetyPolicy.CODE_ALLOWED_FAQ, faqDecision.reasonCode);
    }

    @Test
    public void controlledAutoReply_strictlyBlocksNegativeSpamPiiUncertainSuggestionAndNonFaq() {
        CommentItem negative = new CommentItem();
        negative.setCategory(CommentItem.CATEGORY_NEGATIVE);
        negative.setContent("Quá tệ!");
        negative.setAiConfidence(0.95);
        assertFalse(CommentSafetyPolicy.evaluateAutoReply(negative).allowed);
        assertEquals(CommentSafetyPolicy.CODE_BLOCKED_NEGATIVE, CommentSafetyPolicy.evaluateAutoReply(negative).reasonCode);

        CommentItem spam = new CommentItem();
        spam.setCategory(CommentItem.CATEGORY_SPAM);
        spam.setContent("bit.ly/spam");
        spam.setAiConfidence(0.95);
        assertFalse(CommentSafetyPolicy.evaluateAutoReply(spam).allowed);
        assertEquals(CommentSafetyPolicy.CODE_BLOCKED_SPAM, CommentSafetyPolicy.evaluateAutoReply(spam).reasonCode);

        // Even if category is PRAISE, PII MUST strictly block auto-reply!
        CommentItem praiseWithPii = new CommentItem();
        praiseWithPii.setCategory(CommentItem.CATEGORY_PRAISE);
        praiseWithPii.setContent("Bài hay quá, gọi mình số 0912345678 hợp tác nhé!");
        praiseWithPii.setAiConfidence(0.95);
        assertFalse(CommentSafetyPolicy.evaluateAutoReply(praiseWithPii).allowed);
        assertEquals(CommentSafetyPolicy.CODE_BLOCKED_PII, CommentSafetyPolicy.evaluateAutoReply(praiseWithPii).reasonCode);

        // Even if category is PRAISE, low AI confidence (< 0.75) MUST strictly block auto-reply!
        CommentItem uncertainPraise = new CommentItem();
        uncertainPraise.setCategory(CommentItem.CATEGORY_PRAISE);
        uncertainPraise.setContent("Ừm cũng được...");
        uncertainPraise.setAiConfidence(0.60);
        assertFalse(CommentSafetyPolicy.evaluateAutoReply(uncertainPraise).allowed);
        assertEquals(CommentSafetyPolicy.CODE_BLOCKED_LOW_CONFIDENCE, CommentSafetyPolicy.evaluateAutoReply(uncertainPraise).reasonCode);

        // Suggestion (Góp ý) MUST NOT auto-reply
        CommentItem suggestion = new CommentItem();
        suggestion.setCategory(CommentItem.CATEGORY_SUGGESTION);
        suggestion.setContent("Nên bổ sung thêm biểu đồ minh họa.");
        suggestion.setAiConfidence(0.92);
        assertFalse(CommentSafetyPolicy.evaluateAutoReply(suggestion).allowed);
        assertEquals(CommentSafetyPolicy.CODE_BLOCKED_SUGGESTION, CommentSafetyPolicy.evaluateAutoReply(suggestion).reasonCode);

        // Non-FAQ question MUST NOT auto-reply
        CommentItem complexQuestion = new CommentItem();
        complexQuestion.setCategory(CommentItem.CATEGORY_QUESTION);
        complexQuestion.setFaq(false);
        complexQuestion.setContent("Chính sách bồi thường SLA ngân hàng thế nào?");
        complexQuestion.setAiConfidence(0.85);
        assertFalse(CommentSafetyPolicy.evaluateAutoReply(complexQuestion).allowed);
        assertEquals(CommentSafetyPolicy.CODE_BLOCKED_NON_FAQ, CommentSafetyPolicy.evaluateAutoReply(complexQuestion).reasonCode);
    }

    @Test
    public void parseLlmAnalysisJson_handlesMarkdownFenceAndEnforcesPiiGuardrail() {
        String llmJson = "```json\n" +
                "{\n" +
                "  \"category\": \"PRAISE\",\n" +
                "  \"is_faq\": false,\n" +
                "  \"has_personal_info\": false,\n" +
                "  \"confidence\": 0.96,\n" +
                "  \"risk_warning\": null,\n" +
                "  \"suggested_reply\": \"Cảm ơn bạn đã ủng hộ!\"\n" +
                "}\n" +
                "```";

        CommentSafetyPolicy.CommentAnalysis analysis = CommentSafetyPolicy.parseLlmAnalysisJson(
                llmJson,
                SocialPlatform.THREADS,
                "AI 2026",
                "Chi tiết bài viết",
                "@user",
                "Bài hay lắm, gửi báo giá qua test.user@gmail.com cho mình nhé!"
        );

        assertTrue("Deterministic PII guardrail must override LLM false negative", analysis.hasPersonalInfo);
        assertTrue(analysis.negativeFlagged);
        assertNotNull(analysis.autoReplyBlockedReason);

        CommentItem probe = new CommentItem();
        probe.setContent("Bài hay lắm, gửi báo giá qua test.user@gmail.com cho mình nhé!");
        CommentSafetyPolicy.applyAnalysisToComment(probe, analysis);
        assertFalse(CommentSafetyPolicy.evaluateAutoReply(probe).allowed);
    }

    @Test
    public void parseLlmAnalysisJson_fallsBackCleanlyOnMalformedResponse() {
        CommentSafetyPolicy.CommentAnalysis analysis = CommentSafetyPolicy.parseLlmAnalysisJson(
                "not a valid json response",
                SocialPlatform.FACEBOOK,
                "5 Xu Hướng AI",
                "Nội dung",
                "Lan",
                "Góp ý thêm là nên thêm ví dụ cụ thể hơn!"
        );
        assertEquals(CommentItem.CATEGORY_SUGGESTION, analysis.category);
        assertNotNull(analysis.suggestedReply);
    }
}
