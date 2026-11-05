package com.tuankiet.myapplication.repository;

import android.content.Context;

import com.tuankiet.myapplication.data.DatabaseHelper;
import com.tuankiet.myapplication.model.AgentLog;
import com.tuankiet.myapplication.model.AnalyticsData;
import com.tuankiet.myapplication.model.CommentItem;
import com.tuankiet.myapplication.model.Post;
import com.tuankiet.myapplication.model.SocialAccount;
import com.tuankiet.myapplication.model.SocialPlatform;

import java.util.ArrayList;
import java.util.List;

public class MockDataRepository {
    private final DatabaseHelper dbHelper;
    private static MockDataRepository instance;

    public static synchronized MockDataRepository getInstance(Context context) {
        if (instance == null) {
            instance = new MockDataRepository(context);
        }
        return instance;
    }

    private MockDataRepository(Context context) {
        dbHelper = DatabaseHelper.getInstance(context);
        seedInitialDataIfNeeded();
    }

    public void seedInitialDataIfNeeded() {
        if (!dbHelper.isDatabaseEmpty()) {
            return;
        }

        // 1. Seed Social Accounts
        SocialAccount fbAccount = new SocialAccount(
                "acc_fb_1",
                SocialPlatform.FACEBOOK,
                "AI Tech Vietnam",
                "@aitechvn.official",
                true,
                12450,
                "F",
                "Đang kết nối qua Page Access Token"
        );
        dbHelper.insertOrUpdateAccount(fbAccount);

        SocialAccount liAccount = new SocialAccount(
                "acc_li_1",
                SocialPlatform.LINKEDIN,
                "Nguyen Van A (Tech Lead)",
                "@nguyen-van-a-ai",
                true,
                8920,
                "L",
                "Đang kết nối qua LinkedIn Member OAuth"
        );
        dbHelper.insertOrUpdateAccount(liAccount);

        // 2. Seed Recent Posts
        Post post1 = new Post(
                0,
                "5 Xu Hướng Trí Tuệ Nhân Tạo Năm 2026",
                "Chuyên gia",
                "🚀 5 XU HƯỚNG AI KHÔNG THỂ BỎ QUA TRONG NĂM 2026!\n\n" +
                        "1. Small Language Models (SLM) chạy trực tiếp on-device không cần internet.\n" +
                        "2. Agentic Workflows thay thế chatbot hỏi đáp truyền thống.\n" +
                        "3. Multimodal Reasoning xử lý video và âm thanh real-time.\n" +
                        "4. Tự động hóa quy trình quản lý mạng xã hội cá nhân.\n" +
                        "5. Bảo mật dữ liệu & Privacy-first AI.\n\n" +
                        "👉 Anh em đang ứng dụng xu hướng nào trong công việc? Bình luận bên dưới cùng bàn luận nhé!\n#AI2026 #TechTrends #SocialAI #FutureTech",
                "5 Key AI Trends Shaping Tech Ecosystems in 2026 🌐\n\n" +
                        "As artificial intelligence transitions from experimentation to core operational infrastructure, leaders must prioritize these architectural shifts:\n\n" +
                        "• Edge & On-Device Intelligence: Drastic reduction in latency and cloud inference costs.\n" +
                        "• Autonomous Multi-Agent Orchestration: Specialization over monolithic general models.\n" +
                        "• Enterprise Data Sovereignty: Private fine-tuning on domain-specific datasets.\n\n" +
                        "How is your engineering team adapting to these paradigms? Let's connect and discuss.\n\n#ArtificialIntelligence #Leadership #SoftwareEngineering #AgenticAI",
                Post.STATUS_PUBLISHED,
                true,
                true,
                System.currentTimeMillis() - 86400000L * 2,
                "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe",
                7700,
                550,
                73,
                24
        );
        dbHelper.insertPost(post1);

        Post post2 = new Post(
                0,
                "Tại sao Mobile Developer phải học On-Device AI?",
                "Thân thiện",
                "Chào mọi người! 👋 Tại sao năm 2026 là thời điểm vàng để Mobile Dev chuyển mình sang On-Device AI?\n\n" +
                        "💡 Không còn phụ thuộc 100% vào Cloud API.\n" +
                        "💡 Ứng dụng chạy mượt, bảo mật dữ liệu tuyệt đối cho người dùng.\n" +
                        "💡 Tiết kiệm chi phí token cho startup hàng ngàn đô mỗi tháng.\n\n" +
                        "Anh em nào quan tâm để lại 1 like, mình sẽ share source code demo app SocialAI viết bằng Java Native nhé! 📱✨\n#MobileDev #AndroidNative #OnDeviceAI",
                "Why Mobile Developers Must Embrace On-Device Machine Learning Today 📱\n\n" +
                        "The edge compute renaissance is here. By shifting inference workloads directly to the client:\n" +
                        "1. Zero network latency for real-time user experiences.\n" +
                        "2. Absolute client-side data privacy (HIPAA & GDPR friendly).\n" +
                        "3. Predictable cost margins independent of token billing.\n\n" +
                        "The gap between cloud AI and local device execution is closing fast. Continuous learning is our best leverage.\n\n#AndroidDev #MachineLearning #EdgeComputing",
                Post.STATUS_PUBLISHED,
                true,
                true,
                System.currentTimeMillis() - 86400000L * 4,
                "https://images.unsplash.com/photo-1555774698-0b77e0d5fac6",
                7300,
                557,
                56,
                18
        );
        dbHelper.insertPost(post2);

        Post post3 = new Post(
                0,
                "Kiến trúc Orchestrator - Sub-Agents trong quản lý Social Media",
                "Chuyên gia",
                "🤖 Khám phá mô hình Orchestrator - SubAgents: Bí quyết để một AI Agent có thể quản lý đồng thời Facebook Page và LinkedIn mà không bị trùng lặp nội dung!\n\n" +
                        "Mỗi nền tảng có một 'Sub-Agent' chuyên trách theo ngữ cảnh và văn phong riêng biệt. Đón đọc bài phân tích chi tiết vào 20h tối nay nhé! 🔥",
                "Architectural Blueprint: Orchestrator - Sub-Agent Paradigm for Multi-Channel Publishing 🏗️\n\n" +
                        "Monolithic prompting fails when distributing content across distinct platforms with divergent demographics. Our solution:\n" +
                        "- Master Orchestrator: Synthesizes core brief and strategic intent.\n" +
                        "- Specialized Platform Agents: Optimize voice, formatting, and engagement hooks independently.\n" +
                        "Full architecture breakdown in draft review.",
                Post.STATUS_DRAFT,
                true,
                true,
                System.currentTimeMillis() - 3600000L * 5,
                null,
                0,
                0,
                0,
                0
        );
        dbHelper.insertPost(post3);

        // 3. Seed Comments with AI Sentiment Classification & Safety Flag
        CommentItem c1 = new CommentItem(
                0,
                SocialPlatform.FACEBOOK,
                "5 Xu Hướng Trí Tuệ Nhân Tạo Năm 2026",
                "Trần Minh Khoa",
                "K",
                "Bài viết rất sâu sắc và thực tế anh ơi! Mong anh ra thêm series phân tích kiến trúc On-Device AI trên Android ạ.",
                "15 phút trước",
                CommentItem.CATEGORY_PRAISE,
                false,
                null,
                "Dạ cảm ơn Khoa nhiều nhé! Mình đang hoàn thiện phần demo bằng Java Native và sẽ lên sóng bài phân tích chi tiết vào tuần tới nhé.",
                false,
                null
        );
        dbHelper.insertComment(c1);

        CommentItem c2 = new CommentItem(
                0,
                SocialPlatform.LINKEDIN,
                "5 Xu Hướng Trí Tuệ Nhân Tạo Năm 2026",
                "Lê Thị Hương",
                "H",
                "Cho em hỏi với app Android thuần Java thì tích hợp Gemini REST API có bị delay quá không ạ? Em thấy streaming khá mượt.",
                "1 giờ trước",
                CommentItem.CATEGORY_QUESTION,
                false,
                null,
                "Chào Hương, khi dùng HTTP Streaming với OkHttp và cập nhật TextView qua Handler, độ trễ gần như bằng 0 và mang lại trải nghiệm rất trực quan bạn nhé!",
                false,
                null
        );
        dbHelper.insertComment(c2);

        CommentItem c3 = new CommentItem(
                0,
                SocialPlatform.FACEBOOK,
                "Tại sao Mobile Developer phải học On-Device AI?",
                "Người dùng ẩn danh",
                "A",
                "Nội dung này sao giống copy từ bài blog tuần trước vậy? Đề nghị admin giải thích rõ nguồn số liệu, đừng có nói suông!",
                "3 giờ trước",
                CommentItem.CATEGORY_NEGATIVE,
                false,
                null,
                null, // Disabled auto-reply for negative comment
                true,
                "CẢNH BÁO AN TOÀN: Bình luận có sắc thái tiêu cực hoặc khiếu nại bản quyền. Hệ thống đã khóa tính năng tự động trả lời để tránh rủi ro truyền thông. Bạn vui lòng tự nhập phản hồi thủ công."
        );
        dbHelper.insertComment(c3);

        CommentItem c4 = new CommentItem(
                0,
                SocialPlatform.LINKEDIN,
                "Tại sao Mobile Developer phải học On-Device AI?",
                "Đặng Quốc Bảo",
                "B",
                "Rất đồng tình với quan điểm của tác giả về việc giảm chi phí serverless inference. Đã repost cho đồng nghiệp cùng đọc!",
                "5 giờ trước",
                CommentItem.CATEGORY_PRAISE,
                true,
                "Cảm ơn anh Bảo đã ủng hộ và chia sẻ bài viết!",
                "Cảm ơn anh Bảo đã ủng hộ và chia sẻ bài viết!",
                false,
                null
        );
        dbHelper.insertComment(c4);

        CommentItem c5 = new CommentItem(
                0,
                SocialPlatform.FACEBOOK,
                "5 Xu Hướng Trí Tuệ Nhân Tạo Năm 2026",
                "Spam Bot 24/7",
                "S",
                "Bấm vào link này để nhận ngay khóa học làm giàu cấp tốc không cần học tập: bit.ly/spam-link",
                "1 ngày trước",
                CommentItem.CATEGORY_SPAM,
                false,
                null,
                null,
                true,
                "CẢNH BÁO: Phát hiện liên kết spam độc hại. Đề xuất ẩn hoặc xóa bình luận này."
        );
        dbHelper.insertComment(c5);

        // 4. Seed Initial Agent Logs
        AgentLog log1 = new AgentLog(
                0,
                System.currentTimeMillis() - 86400000L * 2,
                "Orchestrator Synthesis",
                "Master Orchestrator",
                "Analyze topic: 5 Xu hướng AI 2026; Tone: Chuyên gia",
                "Generated brief: Core thesis, 5 structured bullets, CTA focused on engineering leaders.",
                820,
                0.0032,
                1240
        );
        dbHelper.insertLog(log1);

        AgentLog log2 = new AgentLog(
                0,
                System.currentTimeMillis() - 86400000L * 2,
                "Facebook Sub-agent",
                "Facebook Copywriter",
                "Convert brief into engaging FB post with hashtags, emojis, community question",
                "Drafted 5-bullet FB post with emoji headers and discussion hook.",
                640,
                0.0025,
                980
        );
        dbHelper.insertLog(log2);

        AgentLog log3 = new AgentLog(
                0,
                System.currentTimeMillis() - 86400000L * 2,
                "LinkedIn Sub-agent",
                "LinkedIn Thought Leader",
                "Convert brief into professional B2B perspective with strategic takeaways",
                "Drafted executive LinkedIn post with bulleted takeaways and career hashtags.",
                750,
                0.0029,
                1120
        );
        dbHelper.insertLog(log3);
    }

    public List<Post> getPosts() {
        return dbHelper.getAllPosts();
    }

    public void addPost(Post post) {
        dbHelper.insertPost(post);
    }

    public List<CommentItem> getComments() {
        return dbHelper.getAllComments();
    }

    public void replyToComment(long commentId, String reply) {
        dbHelper.updateCommentReply(commentId, reply);
    }

    public List<SocialAccount> getAccounts() {
        return dbHelper.getAllAccounts();
    }

    public void updateAccountStatus(SocialAccount account) {
        dbHelper.insertOrUpdateAccount(account);
    }

    public AnalyticsData getAnalyticsData() {
        AnalyticsData data = new AnalyticsData();
        List<Post> posts = dbHelper.getAllPosts();
        List<CommentItem> comments = dbHelper.getAllComments();

        data.setTotalPostsCreated(posts.size());

        int pendingCount = 0;
        for (CommentItem c : comments) {
            if (!c.isAnswered() && !CommentItem.CATEGORY_SPAM.equals(c.getCategory())) {
                pendingCount++;
            }
        }
        data.setPendingComments(pendingCount);

        // Calculate reach and interactions
        int totalWeeklyEngagement = 1845;
        data.setWeeklyEngagement(totalWeeklyEngagement);

        data.setFacebookReach(18500);
        data.setFacebookLikes(840);
        data.setFacebookComments(112);
        data.setFacebookEngagementRate(5.15);

        data.setLinkedinReach(14200);
        data.setLinkedinLikes(1120);
        data.setLinkedinComments(145);
        data.setLinkedinEngagementRate(8.91);

        data.setTopPerformingPost("5 Xu Hướng Trí Tuệ Nhân Tạo Năm 2026 (7,700 Reach, 550 Likes)");
        data.setWeeklyAiSummary("Hiệu suất tuần này rất khả quan! Kênh LinkedIn có tỉ lệ tương tác vượt trội (8.9%) nhờ nội dung chuyên sâu kỹ thuật. Kênh Facebook duy trì lượng Reach ổn định với 18,500 lượt xem.");
        data.setNextWeekAdvice("Đề xuất tuần tới: Đẩy mạnh 3 bài viết chuyên môn về AI Agent trên LinkedIn và bổ sung 1 bài Q&A tương tác cao cho Facebook Page vào tối thứ 5 (khung giờ vàng 20:00).");

        List<AnalyticsData.DayMetric> timeline = new ArrayList<>();
        timeline.add(new AnalyticsData.DayMetric("T2", 120, 180));
        timeline.add(new AnalyticsData.DayMetric("T3", 210, 240));
        timeline.add(new AnalyticsData.DayMetric("T4", 320, 390));
        timeline.add(new AnalyticsData.DayMetric("T5", 280, 310));
        timeline.add(new AnalyticsData.DayMetric("T6", 390, 480));
        timeline.add(new AnalyticsData.DayMetric("T7", 520, 610));
        timeline.add(new AnalyticsData.DayMetric("CN", 590, 680));
        data.setWeeklyTimeline(timeline);

        return data;
    }
}

