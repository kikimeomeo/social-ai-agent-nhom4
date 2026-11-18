TUẦN 3

Repo: https://github.com/kikimeomeo/social-ai-agent-nhom4
Project board: https://github.com/users/kikimeomeo/projects/1

Current tasks:
• [#4][In Review] Hoàn thiện giao diện Nhập Topic, chọn Tone, Ngôn ngữ và Lập Brief bài viết (F3) - PR #17 <@kikimeomeo>
• [#5][In Review] Xây dựng kiến trúc Multi-Agent Orchestrator kết nối Google Gemini API (F2) - PR #18 <@kikimeomeo>
• [#6][In Review] Xây dựng màn hình Kênh của tôi và quản lý trạng thái kết nối tài khoản (F1) - PR #19 <@huy-vequy>
• [#7][In Review] Tích hợp Meta Threads API cho luồng xuất bản bài viết và lưu permalink (F5) - PR #20 <@huy-vequy>
• [#8][In Review] Thiết kế Dialog Preview theo kênh và công cụ AI viết lại từng đoạn (F4) - PR #13 <@thienshoko>
• [#9][In Review] Xây dựng giao diện Hộp thư Inbox gom bình luận đa kênh và bộ lọc nền tảng (F6) - PR #14 <@thienshoko>
• [#10][In Review] Xây dựng màn hình Cài đặt API Key LLM (BYOK) và lưu trữ mã hóa bảo mật (F8) - PR #15 <@ZanHopUS>
• [#11][In Review] Xây dựng màn hình Thống kê Analytics, thẻ KPI tổng quan và AI insights (F7) - PR #16 <@ZanHopUS>
• Thực hiện review chéo các Pull Request (#13 - #20) theo quy trình nhóm <Cả nhóm>

Next plan:
• Xử lý xong các góp ý review (PR #15), bổ sung liên kết Issue/đủ 2 Approvals, merge toàn bộ PR #13 - #20 vào main và chốt phạm vi 2 kênh tích hợp là Facebook Page & Threads <Cả nhóm>
• Refactor & đồng bộ giao diện (UI/UX) toàn ứng dụng, hoàn thiện luồng viết bài theo bước, stream chữ thời gian thực, xử lý huỷ/retry từng kênh, sinh ảnh minh họa (kèm fallback template) và nhật ký Agent Logs (prompt, token, chi phí) cho Facebook & Threads (F2) <@kikimeomeo>
• Hoàn thiện OAuth/refresh token và kết nối đủ 2 kênh Facebook Page & Threads (F1), triển khai Offline Queue bằng WorkManager, chống đăng trùng lặp (idempotency) và xử lý lỗi/rate limit (F5) <@huy-vequy>
• Kết nối AI cho 3 nút viết lại đoạn & nút sinh lại ảnh (F4), bổ sung lọc theo bài viết, triển khai AI phân loại cảm xúc comment, gợi ý trả lời & tự động phản hồi có kiểm soát kết hợp CommentSyncWorker (F6) <@thienshoko>
• Mở rộng >= 2 nhà cung cấp LLM, cấu hình model riêng cho từng tác vụ và thống kê token/chi phí theo ngày/tháng (F8), hoàn thiện SQLite + biểu đồ tăng trưởng CustomAnalyticsChartView + so sánh giữa các kênh, chế độ Offline và đa ngôn ngữ VI/EN (F7, F9) <@ZanHopUS>

Blockers:
• Không có blocker kỹ thuật lớn, nhóm đang hoàn tất review chéo toàn bộ 8 PR (#13 - #20) và xử lý các góp ý review để merge đồng bộ vào nhánh main.