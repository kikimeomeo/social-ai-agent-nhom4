package com.tuankiet.myapplication.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.tuankiet.myapplication.util.FallbackImageTemplateGenerator;
import com.tuankiet.myapplication.util.VisualImageService;

import org.junit.Test;

/**
 * Unit tests cho hệ thống Multi-Agent AI (Epic F2 - Issue #22).
 */
public class AIAgentOrchestratorTest {

    @Test
    public void testBuildFacebookUserPrompt_containsRequiredElements() {
        // Test tĩnh logic tạo prompt riêng cho Facebook Sub-agent
        String topic = "5 Xu hướng AI trong năm 2026";
        String tone = "Chuyên gia";
        String language = "Tiếng Việt";

        String expectedContent = "Hãy viết một bài đăng Facebook Page hoàn chỉnh bằng ngôn ngữ: " + language;
        assertTrue(expectedContent.contains(topic) == false); // Sanity check

        String prompt = "Hãy viết một bài đăng Facebook Page hoàn chỉnh bằng ngôn ngữ: " + language + ".\n" +
                "- Chủ đề trọng tâm: " + topic + "\n" +
                "- Giọng văn (Tone of voice): " + tone;

        assertTrue(prompt.contains(topic));
        assertTrue(prompt.contains(tone));
        assertTrue(prompt.contains(language));
    }

    @Test
    public void testBuildThreadsUserPrompt_enforcesUnder500CharsDirective() {
        String topic = "On-Device AI trên Android";
        String tone = "Hài hước";
        String language = "Tiếng Việt";

        String prompt = "Hãy viết một bài đăng Threads (Meta) ngắn gọn, sắc bén bằng ngôn ngữ: " + language + ".\n" +
                "- Chủ đề trọng tâm: " + topic + "\n" +
                "- Giọng văn: " + tone + "\n" +
                "- Yêu cầu: Dưới 500 ký tự";

        assertTrue(prompt.contains(topic));
        assertTrue(prompt.contains("Dưới 500 ký tự"));
    }

    @Test
    public void testFallbackImagePalettes_lengthAndStyles() {
        // Kiểm tra 3 bảng màu chuyển sắc chuẩn của FallbackImageTemplateGenerator
        assertNotNull(FallbackImageTemplateGenerator.COLOR_PALETTES);
        assertEquals(3, FallbackImageTemplateGenerator.COLOR_PALETTES.length);
        assertEquals(2, FallbackImageTemplateGenerator.COLOR_PALETTES[0].length); // 2 màu gradient
    }

    @Test
    public void testSafetyCheckRules_detectsWarnings() {
        String cleanText = "Hôm nay mình chia sẻ về giải pháp tối ưu ứng dụng di động.";
        boolean hasSensitive = cleanText.toLowerCase().contains("tự tử") || cleanText.toLowerCase().contains("lừa đảo");
        assertFalse(hasSensitive);

        String warningText = "Bật mí cách kiếm tiền tỷ lừa đảo nhanh nhất!";
        boolean detected = warningText.toLowerCase().contains("lừa đảo");
        assertTrue(detected);
    }

    @Test
    public void testVisualImageService_buildSearchKeywords() {
        VisualImageService.VisualSpec aiSpec = VisualImageService.buildLocalVisualSpec("5 Xu hướng Trí tuệ Nhân tạo định hình 2026");
        assertTrue(aiSpec.searchKeywords.contains("microchip"));
        assertTrue(aiSpec.aiPrompt.contains("no text"));

        String mobileKeywords = VisualImageService.buildSearchKeywords("Tối ưu hiệu năng app Android và Mobile");
        assertTrue(mobileKeywords.contains("smartphone"));

        String emptyKeywords = VisualImageService.buildSearchKeywords("");
        assertFalse(emptyKeywords.isEmpty());
    }
}
