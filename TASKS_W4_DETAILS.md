# DANH SÁCH ISSUE TUẦN 4 (CHUẨN TÀI KHOẢN GITHUB)
**Dự án:** SocialAI - Mobile AI Agent Xây Dựng Kênh Social Media  
**Nhóm:** 04 | **Phạm vi kênh chốt:** Facebook Page & Threads | **Hạn hoàn thành:** 21/11/2026 (Thứ Bảy)  

### Danh sách 4 tài khoản GitHub thành viên:
1. **Kiệt:** `kikimeomeo` (Owner)
2. **Huy:** `huy-vequy`
3. **Thiện:** `thienshoko`
4. **Hợp:** `ZanHopUS`

> **Lưu ý trước khi mở nhánh mới:** Nhóm hoàn tất review & merge 8 PR của tuần trước (`#13` → `#20`) vào `main`, sau đó `git pull origin main` rồi mới tạo nhánh `feature/<issue_id>-...` cho các Issue dưới đây.

---

## 👤 1. PHẦN CỦA KIỆT (`@kikimeomeo`)

### 📌 TASK 1: REFACTOR & ĐỒNG BỘ GIAO DIỆN TOÀN APP (UI/UX)
* **Title:**
```text
[UI/Refactor] Refactor và đồng bộ giao diện toàn app theo phạm vi 2 kênh Facebook Page & Threads
```
* **Assignee:** `kikimeomeo`
* **Projects:** `SocialAI Project Board`
* **Description:**
```markdown
**Hạn chót:** 21/11/2026

### Mục tiêu
Đồng bộ ngôn ngữ thiết kế Material 3, spacing, màu sắc, icon và luồng điều hướng giữa các tab sau khi merge code của 4 thành viên; loại bỏ phần LinkedIn dư thừa để thống nhất 2 kênh Facebook Page & Threads.

### Tiêu chí nghiệm thu (Checklist)
- [ ] Rà soát và đồng bộ style, màu sắc, typography và component dùng chung trên 4 tab (Home/Channels, Create Post, Inbox, Analytics/Settings).
- [ ] Gỡ bỏ hoặc ẩn các lựa chọn/hardcode liên quan đến LinkedIn, chốt đồng nhất chỉ còn **Facebook Page** và **Threads** ở mọi màn hình.
- [ ] Kiểm tra và sửa các lỗi hiển thị (overlap, cắt chữ, dark/light contrast, padding) trên các kích thước màn hình phổ biến.
```

---

### 📌 TASK 2: HOÀN THIỆN LUỒNG MULTI-AGENT, STREAM & SINH ẢNH (F2)
* **Title:**
```text
[F2 - Core/AI] Hoàn thiện luồng viết bài theo bước, stream chữ, huỷ/retry và sinh ảnh minh họa
```
* **Assignee:** `kikimeomeo`
* **Projects:** `SocialAI Project Board`
* **Description:**
```markdown
**Hạn chót:** 21/11/2026

### Mục tiêu
Hoàn thiện trọn vẹn quy trình sinh nội dung của Multi-Agent cho Facebook Page & Threads kèm trải nghiệm stream chữ, sinh ảnh minh họa và xử lý lỗi độc lập từng kênh.

### Tiêu chí nghiệm thu (Checklist)
- [ ] Hiển thị tiến độ viết bài theo từng bước: *Dàn ý → Bản nháp → Tự rà quy tắc kênh → Mô tả ảnh → Sinh ảnh → Đóng gói*.
- [ ] Hiệu ứng **stream nội dung văn bản thời gian thực** lên giao diện khi AI đang sinh bài.
- [ ] Hỗ trợ **huỷ ngang giữa chừng** và **chạy lại (retry) riêng từng kênh**; lỗi ở Facebook không làm ảnh hưởng kết quả của Threads (và ngược lại).
- [ ] Tích hợp luồng **sinh ảnh minh họa** phù hợp từng kênh, kèm **cơ chế Fallback template** dựng ảnh có sẵn khi mất mạng hoặc lỗi API sinh ảnh.
```

---

### 📌 TASK 3: NHẬT KÝ AGENT LOGS (F2)
* **Title:**
```text
[F2 - Logs] Xây dựng màn hình/Dialog Nhật ký Agent Logs (Prompt, Kết quả, Token, Chi phí)
```
* **Assignee:** `kikimeomeo`
* **Projects:** `SocialAI Project Board`
* **Description:**
```markdown
**Hạn chót:** 21/11/2026

### Mục tiêu
Cho phép người dùng xem lại toàn bộ lịch sử hoạt động của AI Agent để minh bạch prompt, kết quả trả về và chi phí sử dụng.

### Tiêu chí nghiệm thu (Checklist)
- [ ] Lưu lại chi tiết từng bước chạy Agent: Tên Agent/kênh, prompt đã dùng, kết quả AI trả về, trạng thái thành công/lỗi.
- [ ] Ghi nhận số token tiêu thụ (input/output) và chi phí ước tính (`$`) cho mỗi lần chạy.
- [ ] Thiết kế màn hình/Dialog **Agent Logs** hiển thị danh sách nhật ký rõ ràng, cho phép bấm xem chi tiết từng lần gọi.
```

---

## 👤 2. PHẦN CỦA HUY (`@huy-vequy`)

### 📌 TASK 4: HOÀN THIỆN OAUTH, REFRESH TOKEN & KẾT NỐI ĐỦ 2 KÊNH (F1)
* **Title:**
```text
[F1 - Auth] Hoàn thiện OAuth/Refresh Token và kết nối đủ 2 kênh Facebook Page & Threads
```
* **Assignee:** `huy-vequy`
* **Projects:** `SocialAI Project Board`
* **Description:**
```markdown
**Hạn chót:** 21/11/2026

### Mục tiêu
Đảm bảo ứng dụng kết nối được thực tế cả 2 mạng xã hội **Facebook Page** và **Threads** thông qua OAuth và quản lý vòng đời token an toàn.

### Tiêu chí nghiệm thu (Checklist)
- [ ] Hoàn thiện luồng đăng nhập OAuth qua trình duyệt hệ thống cho **Facebook Page** và **Threads**.
- [ ] Lưu trữ Token an toàn trên thiết bị; hỗ trợ **tự làm mới token (refresh token)** khi sắp hết hạn.
- [ ] Hiển thị thông báo nhắc nhở người dùng kết nối lại khi token hết hạn hoặc bị thu hồi quyền.
- [ ] Đảm bảo nút "Ngắt kết nối" xoá sạch token và dữ liệu phiên của kênh tương ứng.
```

---

### 📌 TASK 5: OFFLINE QUEUE, CHỐNG TRÙNG LẶP & XỬ LÝ LỖI ĐĂNG BÀI (F5)
* **Title:**
```text
[F5 - Queue/API] Triển khai Offline Queue (WorkManager), chống trùng lặp và xử lý lỗi đăng bài
```
* **Assignee:** `huy-vequy`
* **Projects:** `SocialAI Project Board`
* **Description:**
```markdown
**Hạn chót:** 21/11/2026

### Mục tiêu
Hoàn thiện luồng xuất bản đồng thời lên Facebook Page & Threads, đảm bảo hoạt động tin cậy ngay cả khi mất mạng hoặc gặp lỗi API.

### Tiêu chí nghiệm thu (Checklist)
- [ ] Hỗ trợ đăng đồng thời lên Facebook Page & Threads; quản lý đầy đủ trạng thái từng kênh (*Nháp → Chờ → Đang đăng → Đã đăng / Lỗi / Đã bàn giao*).
- [ ] Triển khai **Offline Queue** bằng `WorkManager`: mất mạng vẫn bấm đăng được, tự động đẩy bài khi có mạng trở lại.
- [ ] Cơ chế **chống đăng trùng lặp (Idempotency)**: không đăng 2 bài giống nhau khi bấm nút nhiều lần hoặc khi retry.
- [ ] Xử lý lỗi mạng / rate limit (tự động thử lại) và hiển thị rõ nguyên nhân nếu lỗi quyền hạn hoặc vi phạm nội dung.
- [ ] Lưu URL bài viết đã đăng thành công và bấm mở được bài gốc trên Facebook / Threads.
```

---

## 👤 3. PHẦN CỦA THIỆN (`@thienshoko`)

### 📌 TASK 6: KẾT NỐI AI VIẾT LẠI TỪNG ĐOẠN & SINH LẠI ẢNH (F4)
* **Title:**
```text
[F4 - AI] Kết nối AI cho công cụ viết lại từng đoạn và nút yêu cầu sinh lại ảnh minh họa
```
* **Assignee:** `thienshoko`
* **Projects:** `SocialAI Project Board`
* **Description:**
```markdown
**Hạn chót:** 21/11/2026

### Mục tiêu
Hoàn thiện luồng Human-in-the-loop tại màn hình duyệt bài: kết nối AI thật cho các công cụ chỉnh sửa nội dung và ảnh trước khi người dùng bấm đăng.

### Tiêu chí nghiệm thu (Checklist)
- [ ] Đảm bảo chốt chặn an toàn: Agent **tuyệt đối không tự ý đăng bài** khi người dùng chưa bấm duyệt.
- [ ] Kết nối API LLM thật cho 3 nút viết lại: **"Ngắn hơn"**, **"Trang trọng hơn"**, **"Đổi Hook mở đầu"** và cập nhật trực tiếp lên khung sửa bài.
- [ ] Bổ sung nút **"Sinh lại ảnh"** trong màn hình Preview để gọi lại luồng tạo ảnh minh họa mới cho bài viết.
- [ ] Kiểm tra bộ đếm ký tự chuẩn xác theo giới hạn của Threads (500 ký tự) và Facebook.
```

---

### 📌 TASK 7: AI PHÂN LOẠI COMMENT, TỰ ĐỘNG TRẢ LỜI CÓ KIỂM SOÁT & SYNC NỀN (F6)
* **Title:**
```text
[F6 - AI/Worker] AI phân loại comment, tự động trả lời có kiểm soát và đồng bộ nền CommentSyncWorker
```
* **Assignee:** `thienshoko`
* **Projects:** `SocialAI Project Board`
* **Description:**
```markdown
**Hạn chót:** 21/11/2026

### Mục tiêu
Hoàn thiện Hộp thư tương tác (Inbox) cho Facebook & Threads với khả năng đồng bộ nền, phân loại bình luận bằng AI và phản hồi an toàn.

### Tiêu chí nghiệm thu (Checklist)
- [ ] Bổ sung đầy đủ bộ lọc Inbox theo: **Kênh (Facebook / Threads)**, **Bài viết** và **Trạng thái (Chưa trả lời / Đã trả lời)**.
- [ ] Triển khai kéo để làm mới (Pull-to-refresh) kết hợp đồng bộ ngầm định kỳ bằng `CommentSyncWorker`.
- [ ] Dùng AI tự động phân loại bình luận thành 5 nhóm: *Câu hỏi, Khen ngợi, Góp ý, Tiêu cực, Spam*.
- [ ] AI soạn sẵn câu trả lời gợi ý theo ngữ cảnh bài đăng để người dùng chỉnh sửa trước khi gửi qua API thật.
- [ ] Chế độ **tự động trả lời có kiểm soát**: Chỉ tự động trả lời *Khen ngợi* và *Câu hỏi thường gặp*; **tuyệt đối KHÔNG** tự trả lời comment *Tiêu cực*, chứa thông tin cá nhân hoặc AI chưa chắc chắn.
```

---

## 👤 4. PHẦN CỦA HỢP (`@ZanHopUS`)

### 📌 TASK 8: ĐA NHÀ CUNG CẤP LLM, CHỌN MODEL THEO TÁC VỤ & GIÁM SÁT CHI PHÍ (F8)
* **Title:**
```text
[F8 - BYOK] Hỗ trợ >= 2 nhà cung cấp LLM, cấu hình Model theo tác vụ và thống kê chi phí ngày/tháng
```
* **Assignee:** `ZanHopUS`
* **Projects:** `SocialAI Project Board`
* **Description:**
```markdown
**Hạn chót:** 21/11/2026

### Mục tiêu
Hoàn thiện hệ thống quản lý API Key (BYOK) hỗ trợ đa nhà cung cấp, cho phép chọn model linh hoạt theo từng tác vụ và giám sát chi phí tiêu thụ.

### Tiêu chí nghiệm thu (Checklist)
- [ ] Hỗ trợ **$\ge 2$ nhà cung cấp LLM**: Google Gemini và OpenRouter (hoặc OpenAI), kiểm tra hợp lệ key cho từng provider và lưu trong `EncryptedSharedPreferences`.
- [ ] Cho phép **cấu hình Model riêng cho từng tác vụ**: Model viết bài, Model phân loại/trả lời comment, Model sinh ảnh (hoặc mô tả ảnh).
- [ ] Lưu và hiển thị thống kê **số lượng token tiêu thụ + chi phí ước tính (`$`)** tích lũy theo ngày và theo tháng.
- [ ] Đảm bảo API Key luôn hiển thị dạng che (`••••`) và không bao giờ gửi qua server trung gian.
```

---

### 📌 TASK 9: BIỂU ĐỒ TĂNG TRƯỞNG SQLITE, CHẾ ĐỘ OFFLINE & ĐA NGÔN NGỮ VI/EN (F7, F9)
* **Title:**
```text
[F7/F9 - Data/UI] Biểu đồ tăng trưởng SQLite (CustomAnalyticsChartView), so sánh kênh, Offline Mode & VI/EN
```
* **Assignee:** `ZanHopUS`
* **Projects:** `SocialAI Project Board`
* **Description:**
```markdown
**Hạn chót:** 21/11/2026

### Mục tiêu
Hoàn thiện màn hình Thống kê Analytics dựa trên dữ liệu lưu định kỳ trong SQLite, đồng thời hoàn tất chế độ Offline và đa ngôn ngữ toàn app.

### Tiêu chí nghiệm thu (Checklist)
- [ ] Lưu trữ số liệu tương tác (Like, Comment, Share, Reach, Follower) định kỳ vào **SQLite**.
- [ ] Vẽ biểu đồ tăng trưởng theo thời gian bằng `CustomAnalyticsChartView` (hiển thị **lượng follower theo thời gian** và **tương tác theo tuần**).
- [ ] Hoàn thiện **bảng so sánh hiệu quả giữa 2 kênh (Facebook Page vs Threads)** và danh sách Top bài viết hiệu quả nhất.
- [ ] Hoàn thiện **Chế độ Offline (F9)**: Khi mất mạng vẫn mở app xem lại được bản nháp, lịch sử bình luận và số liệu thống kê đã lưu trong máy.
- [ ] Hoàn thiện **đa ngôn ngữ giao diện Tiếng Việt / Tiếng Anh (VI/EN)** chuyển đổi mượt mà trong phần Cài đặt.
```
