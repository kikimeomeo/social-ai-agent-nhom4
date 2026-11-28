package com.tuankiet.myapplication.repository;

import android.content.Context;

import com.tuankiet.myapplication.agent.CommentSafetyPolicy;
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
                "Đang kết nối qua Meta Graph API (Page Access Token)"
        );
        dbHelper.insertOrUpdateAccount(fbAccount);

        SocialAccount thAccount = new SocialAccount(
                "acc_threads_1",
                SocialPlatform.THREADS,
                "AI Tech VN",
                "@aitech.vn",
                true,
                8920,
                "T",
                "Đang kết nối qua Meta Threads API (Official v1.0)"
        );
        dbHelper.insertOrUpdateAccount(thAccount);

        // 2. Seed Recent Posts (Facebook Page + Threads)
        Post post1 = createSeedPost(
                "5 Xu Hướng Trí Tuệ Nhân Tạo Năm 2026",
                "Chuyên gia",
                "🚀 5 XU HƯỚNG AI KHÔNG THỂ BỎ QUA TRONG NĂM 2026!\n\n" +
                        "1. Small Language Models (SLM) chạy trực tiếp on-device không cần internet.\n" +
                        "2. Agentic Workflows thay thế chatbot hỏi đáp truyền thống.\n" +
                        "3. Multimodal Reasoning xử lý video và âm thanh real-time.\n" +
                        "4. Tự động hóa quy trình quản lý mạng xã hội cá nhân.\n" +
                        "5. Bảo mật dữ liệu & Privacy-first AI.\n\n" +
                        "👉 Anh em đang ứng dụng xu hướng nào trong công việc? Bình luận bên dưới cùng bàn luận nhé!\n#AI2026 #TechTrends #SocialAI #FutureTech",
                "5 xu hướng AI đang định hình sản phẩm công nghệ 2026 🌐\n\n" +
                        "• SLM chạy trực tiếp on-device giảm độ trễ & chi phí.\n" +
                        "• Multi-Agent phân vai thay thế prompt đơn lẻ.\n" +
                        "• Privacy-first AI giữ dữ liệu trên thiết bị.\n\n" +
                        "Team bạn đã áp dụng hướng nào rồi? Cùng thảo luận nhé! #AI2026 #AgenticAI",
                Post.STATUS_PUBLISHED,
                System.currentTimeMillis() - 86400000L * 2,
                "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe",
                7700, 550, 73, 24
        );
        dbHelper.insertPost(post1);

        Post post2 = createSeedPost(
                "Tại sao Mobile Developer phải học On-Device AI?",
                "Thân thiện",
                "Chào mọi người! 👋 Tại sao năm 2026 là thời điểm vàng để Mobile Dev chuyển mình sang On-Device AI?\n\n" +
                        "💡 Không còn phụ thuộc 100% vào Cloud API.\n" +
                        "💡 Ứng dụng chạy mượt, bảo mật dữ liệu tuyệt đối cho người dùng.\n" +
                        "💡 Tiết kiệm chi phí token cho startup hàng ngàn đô mỗi tháng.\n\n" +
                        "Anh em nào quan tâm để lại 1 like, mình sẽ share source code demo app SocialAI viết bằng Java Native nhé! 📱✨\n#MobileDev #AndroidNative #OnDeviceAI",
                "Mobile Dev nên học On-Device AI ngay từ hôm nay 📱\n\n" +
                        "1. Phản hồi gần như tức thì, không phụ thuộc mạng.\n" +
                        "2. Bảo mật dữ liệu ngay trên máy người dùng.\n" +
                        "3. Tiết kiệm đáng kể chi phí API mỗi tháng.\n\n" +
                        "Bạn đang dùng thư viện nào cho AI trên Android? #AndroidDev #OnDeviceAI",
                Post.STATUS_PUBLISHED,
                System.currentTimeMillis() - 86400000L * 4,
                "https://images.unsplash.com/photo-1555774698-0b77e0d5fac6",
                7300, 557, 56, 18
        );
        dbHelper.insertPost(post2);

        Post post3 = createSeedPost(
                "Kiến trúc Orchestrator - Sub-Agents trong quản lý Social Media",
                "Chuyên gia",
                "🤖 Khám phá mô hình Orchestrator - SubAgents: Bí quyết để một AI Agent có thể quản lý đồng thời Facebook Page và Threads mà không bị trùng lặp nội dung!\n\n" +
                        "Mỗi nền tảng có một 'Sub-Agent' chuyên trách theo ngữ cảnh và văn phong riêng biệt. Đón đọc bài phân tích chi tiết vào 20h tối nay nhé! 🔥",
                "Kiến trúc Orchestrator - Sub-Agent cho đăng bài đa kênh 🏗️\n\n" +
                        "• Master Orchestrator: Tóm tắt brief và định hướng nội dung.\n" +
                        "• Sub-Agents: Tối ưu riêng giọng văn Facebook Page & giới hạn ≤ 500 ký tự của Threads.\n\n" +
                        "Đang ở bản nháp để duyệt trước khi phát hành! #SocialAI #MultiAgent",
                Post.STATUS_DRAFT,
                System.currentTimeMillis() - 3600000L * 5,
                null,
                0, 0, 0, 0
        );
        dbHelper.insertPost(post3);

        // 3. Seed Comments across Facebook & Threads covering all 5 AI categories + F6 safety cases
        String post1Content = post1.getContentFacebook();
        String post2Content = post2.getContentFacebook();

        CommentItem c1 = createAnalyzedSeedComment(
                SocialPlatform.FACEBOOK,
                post1.getTopic(),
                post1Content,
                "Trần Minh Khoa",
                "K",
                "Bài viết rất sâu sắc và thực tế anh ơi! Mong anh ra thêm series phân tích kiến trúc On-Device AI trên Android ạ.",
                "15 phút trước",
                false,
                null
        );
        dbHelper.insertComment(c1);

        CommentItem c2 = createAnalyzedSeedComment(
                SocialPlatform.THREADS,
                post1.getTopic(),
                post1Content,
                "@lehuong.dev",
                "H",
                "Cho em hỏi app có hỗ trợ tự nhập Gemini API Key cá nhân (BYOK) và chế độ miễn phí không ạ?",
                "42 phút trước",
                false,
                null
        );
        dbHelper.insertComment(c2);

        CommentItem c3 = createAnalyzedSeedComment(
                SocialPlatform.FACEBOOK,
                post2.getTopic(),
                post2Content,
                "Phạm Quốc Khánh",
                "K",
                "Bài viết chất lượng lắm, góp ý thêm là bạn nên bổ sung bảng so sánh độ trễ giữa Gemini Flash và SLM chạy local để dễ chọn hơn nhé!",
                "1 giờ trước",
                false,
                null
        );
        dbHelper.insertComment(c3);

        CommentItem c4 = createAnalyzedSeedComment(
                SocialPlatform.THREADS,
                post2.getTopic(),
                post2Content,
                "@anon_reviewer",
                "A",
                "Nội dung này sao giống copy từ bài blog tuần trước vậy? Đề nghị admin giải thích rõ nguồn số liệu, đừng có nói suông!",
                "3 giờ trước",
                false,
                null
        );
        dbHelper.insertComment(c4);

        CommentItem c5 = createAnalyzedSeedComment(
                SocialPlatform.THREADS,
                post1.getTopic(),
                post1Content,
                "@minhnguyen.biz",
                "M",
                "Mình muốn đăng ký tư vấn triển khai cho team, gọi mình qua số 0912345678 hoặc gửi qua email minh.nguyen@gmail.com nhé!",
                "4 giờ trước",
                false,
                null
        );
        dbHelper.insertComment(c5);

        CommentItem c6 = createAnalyzedSeedComment(
                SocialPlatform.FACEBOOK,
                post2.getTopic(),
                post2Content,
                "Nguyễn Hữu Tín",
                "T",
                "Hợp đồng SLA triển khai on-premise cho ngân hàng bên mình có cam kết mức bồi thường pháp lý cụ thể không?",
                "5 giờ trước",
                false,
                null
        );
        dbHelper.insertComment(c6);

        CommentItem c7 = createAnalyzedSeedComment(
                SocialPlatform.THREADS,
                post2.getTopic(),
                post2Content,
                "@baodang.tech",
                "B",
                "Rất đồng tình với quan điểm của tác giả về việc giảm chi phí inference. Đã repost cho đồng nghiệp cùng đọc!",
                "6 giờ trước",
                true,
                "Cảm ơn @baodang.tech đã ủng hộ và chia sẻ bài viết về On-Device AI nhé!"
        );
        dbHelper.insertComment(c7);

        CommentItem c8 = createAnalyzedSeedComment(
                SocialPlatform.FACEBOOK,
                post1.getTopic(),
                post1Content,
                "Spam Bot 24/7",
                "S",
                "Bấm vào link này để nhận ngay khóa học làm giàu cấp tốc không cần học tập: bit.ly/spam-link",
                "1 ngày trước",
                false,
                null
        );
        dbHelper.insertComment(c8);

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
                "Threads Sub-agent",
                "Threads Concise Writer",
                "Convert brief into conversational Threads post under 500 characters",
                "Drafted concise Threads post (312 chars) with open question hook.",
                620,
                0.0024,
                960
        );
        dbHelper.insertLog(log3);
    }

    private Post createSeedPost(String topic,
                                String tone,
                                String contentFacebook,
                                String contentThreads,
                                String status,
                                long createdAt,
                                String imageUrl,
                                int reachCount,
                                int likeCount,
                                int commentCount,
                                int shareCount) {
        Post post = new Post();
        post.setTopic(topic);
        post.setTone(tone);
        post.setLanguage("Tiếng Việt");
        post.setContentFacebook(contentFacebook);
        post.setContentThreads(contentThreads);
        post.setContentLinkedIn("");
        post.setStatus(status);
        post.setTargetFacebook(true);
        post.setTargetThreads(true);
        post.setTargetLinkedIn(false);
        post.setCreatedAt(createdAt);
        post.setImageUrl(imageUrl);
        post.setReachCount(reachCount);
        post.setLikeCount(likeCount);
        post.setCommentCount(commentCount);
        post.setShareCount(shareCount);
        boolean published = Post.STATUS_PUBLISHED.equals(status);
        post.setPublishedFacebook(published);
        post.setPublishedThreads(published);
        return post;
    }

    public List<Post> getPosts() {
        return dbHelper.getAllPosts();
    }

    public void addPost(Post post) {
        dbHelper.insertPost(post);
    }

    public void updatePost(Post post) {
        if (post == null) return;
        if (post.getId() > 0) {
            dbHelper.updatePost(post);
        } else {
            dbHelper.insertPost(post);
        }
    }

    private CommentItem createAnalyzedSeedComment(SocialPlatform platform,
                                                  String postTitle,
                                                  String postContent,
                                                  String authorName,
                                                  String authorInitial,
                                                  String content,
                                                  String timeAgo,
                                                  boolean answered,
                                                  String replyText) {
        CommentItem item = new CommentItem();
        item.setPlatform(platform);
        item.setPostTitle(postTitle);
        item.setPostContent(postContent);
        item.setAuthorName(authorName);
        item.setAuthorInitial(authorInitial);
        item.setContent(content);
        item.setTimeAgo(timeAgo);
        item.setAnswered(answered);
        item.setReplyText(replyText);

        CommentSafetyPolicy.CommentAnalysis analysis = CommentSafetyPolicy.analyzeCommentLocally(
                platform, postTitle, postContent, authorName, content
        );
        CommentSafetyPolicy.applyAnalysisToComment(item, analysis);
        return item;
    }

    public List<CommentItem> getComments() {
        return dbHelper.getAllComments();
    }

    public CommentItem getCommentById(long commentId) {
        return dbHelper.getCommentById(commentId);
    }

    public void replyToComment(long commentId, String reply) {
        dbHelper.updateCommentReply(commentId, reply, false);
    }

    public void replyToComment(long commentId, String reply, boolean autoReplied) {
        dbHelper.updateCommentReply(commentId, reply, autoReplied);
    }

    public void updateCommentAnalysis(CommentItem item) {
        dbHelper.updateCommentAnalysis(item);
    }

    public String findPostContentByTitle(String postTitle) {
        return dbHelper.findPostContentByTitle(postTitle);
    }

    public List<SocialAccount> getAccounts() {
        List<SocialAccount> accounts = dbHelper.getAllAccounts();
        boolean hasThreads = false;
        for (SocialAccount acc : accounts) {
            if (acc.getPlatform() == SocialPlatform.THREADS) {
                hasThreads = true;
                break;
            }
        }
        if (!hasThreads) {
            SocialAccount thAccount = new SocialAccount(
                    "acc_threads_1",
                    SocialPlatform.THREADS,
                    "AI Tech VN",
                    "@aitech.vn",
                    true,
                    8920,
                    "T",
                    "Đang kết nối qua Meta Threads API (Official v1.0)"
            );
            dbHelper.insertOrUpdateAccount(thAccount);
            accounts.add(thAccount);
        }
        return accounts;
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

        data.setThreadsReach(14200);
        data.setThreadsLikes(1120);
        data.setThreadsComments(145);
        data.setThreadsEngagementRate(8.91);

        data.setTopPerformingPost("5 Xu Hướng Trí Tuệ Nhân Tạo Năm 2026 (7,700 Reach, 550 Likes)");
        data.setWeeklyAiSummary("Hiệu suất tuần này rất khả quan! Kênh Threads có tỉ lệ tương tác vượt trội (8.9%) nhờ nội dung ngắn gọn giàu tính thảo luận. Kênh Facebook duy trì lượng Reach ổn định với 18,500 lượt xem.");
        data.setNextWeekAdvice("Đề xuất tuần tới: Đẩy mạnh 3 bài viết ngắn chủ đề AI Agent trên Threads và bổ sung 1 bài Q&A tương tác cao cho Facebook Page vào tối thứ 5 (khung giờ vàng 20:00).");

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

