Dưới đây là biên bản ghi chép chi tiết toàn bộ nội dung buổi họp trong video **`lttbdd.mov`** (thời lượng: 18 phút 04 giây):

---

# BIÊN BẢN CHI TIẾT BUỔI HỌP: ĐỒ ÁN CUỐI KỲ MÔN LTTBDD
- **Chủ trì**: Thầy Hải Phạm (Host)
- **Kênh thông báo & thảo luận**: Kênh Slack `#2024_31_mobile_development`
- **Mục tiêu buổi họp**: Công bố đề tài đồ án cuối kỳ môn Lập trình thiết bị di động (LTTBDD), giải thích chi tiết các yêu cầu tính năng (Epic), quy trình làm việc trên GitHub và quy định báo cáo hàng tuần.

---

## I. THÔNG TIN CHUNG VỀ ĐỀ TÀI
- **Tên đề tài**: **Mobile AI Agent Xây Dựng Kênh Social Media**
- **Định hướng**: Tập trung phát triển trên nền tảng **Mobile (Frontend)**, không cần dựng backend từ đầu mà tận dụng API trực tiếp (kết nối trực tiếp OAuth mạng xã hội, LLM API của bên thứ 3).
- **Ý tưởng cốt lõi**: Hỗ trợ người dùng xây dựng, phát triển và quản lý các kênh mạng xã hội thông qua các Agent AI tự động (từ việc nhận chủ đề, tìm kiếm thông tin, viết bài, sinh ảnh, duyệt bài, đăng bài đến việc theo dõi chỉ số tăng trưởng và tự động tương tác/trả lời comment).

---

## II. CHI TIẾT YÊU CẦU TÍNH NĂNG (9 NHÓM CHỨC NĂNG / EPICS)
> **Quy ước ký hiệu**:
> - **`[B]` Bắt buộc**: Các tính năng cơ bản toàn bộ các nhóm bắt buộc phải hoàn thành.
> - **`[N]` Nâng cao**: Các tính năng mở rộng/tùy chọn (optional) giúp nhóm nâng cao điểm số.

---

### F1. Tích hợp nhiều mạng xã hội
- `[B]` **Kết nối tối thiểu 2 kênh**: Đăng nhập OAuth (hỗ trợ LinkedIn hoặc Facebook Page / Threads).
- `[B]` **Màn hình "Kênh của tôi"**: Hiển thị trạng thái kết nối, tài khoản/Page đang dùng, các chức năng kênh hỗ trợ (đăng, đọc comment, trả lời, mention, số liệu).
- `[B]` **Cơ chế OAuth**: Đăng nhập OAuth qua trình duyệt hệ thống; tự làm mới token (refresh token); thông báo người dùng kết nối lại khi token hết hạn.
- `[B]` **Ngắt kết nối**: Hỗ trợ ngắt kết nối và xoá dữ liệu đã lưu của kênh.

---

### F2. AI Agent viết bài và sinh ảnh
- `[B]` **Kiến trúc Orchestrator - Sub-agents**: Orchestrator nhận topic, lập brief chung và gọi song song các sub-agent (mỗi kênh một sub-agent riêng biệt).
- `[B]` **Luồng viết bài riêng cho từng kênh**: Quy trình gồm: *Dàn ý $\rightarrow$ Bản nháp $\rightarrow$ Tự rà theo quy tắc từng kênh $\rightarrow$ Mô tả ảnh $\rightarrow$ Sinh ảnh $\rightarrow$ Đóng gói kết quả*.
- `[B]` **Luồng sinh ảnh theo kênh**: Sinh ảnh với kích thước, phong cách phù hợp từng kênh; có phương án dự phòng (fallback) dựng ảnh từ template có sẵn khi không có hoặc lỗi API sinh ảnh.
- `[B]` **Trải nghiệm viết**: Hiển thị tiến độ từng bước, stream nội dung văn bản trực tiếp khi AI đang sinh chữ.
- `[B]` **Xử lý ngắt / lỗi**: Cho phép huỷ ngang giữa chừng; một kênh bị lỗi không làm ảnh hưởng các kênh khác; cho phép chạy lại (retry) riêng từng kênh.
- `[B]` **Nhật ký Agent (Agent Logs)**: Xem lại chi tiết từng bước, prompt đã dùng, kết quả trả về, số token tiêu thụ và chi phí ước tính.

---

### F3. Nhập topic
- `[B]` **Nhập bằng văn bản kèm brief**: Bao gồm mục tiêu bài viết, đối tượng đọc giả, giọng văn (tone), ngôn ngữ (VI/EN), kênh đích sẽ đăng.
- `[N]` Nhập topic bằng **giọng nói (Voice)**.
- `[N]` Nhập bằng **URL**: Agent tự động đọc bài viết gốc trên mạng và viết bài chia sẻ có trích nguồn.
- `[N]` Nhập bằng **hình ảnh** (ảnh chụp slide, whiteboard...).
- `[N]` **Share extension**: Chia sẻ link trực tiếp từ ứng dụng khác (trình duyệt, app đọc báo) sang app để tự tạo bài.
- `[N]` **Gợi ý topic**: Tự động gợi ý đề tài từ các bài viết hiệu quả nhất hoặc từ nguồn xu hướng (trends).

---

### F4. Duyệt và chỉnh sửa bài viết
- `[B]` **Kiểm soát đăng bài**: Agent không được tự ý đăng bài khi người dùng chưa duyệt.
- `[B]` **Xem trước (Preview) & Chỉnh sửa**: Xem trước giao diện thực tế theo từng kênh; cho phép người dùng sửa tay nội dung; tự động đếm ký tự theo giới hạn của từng kênh.
- `[B]` **Viết lại từng phần**: Cho phép yêu cầu AI viết lại từng đoạn ngắn hơn, trang trọng hơn, đổi hook mở đầu; yêu cầu sinh lại ảnh.
- `[N]` Lưu lịch sử các phiên bản bản nháp.
- `[N]` Sinh 3 phương án câu mở đầu (hook) để người dùng chọn.
- `[N]` Tự động sinh `alt text` cho hình ảnh (hỗ trợ SEO & accessibility).
- `[N]` Cảnh báo trước khi đăng: phát hiện số liệu chưa có nguồn dẫn, thông tin cá nhân hoặc từ ngữ nhạy cảm.

---

### F5. Đăng mạng xã hội
- `[B]` **Đăng đồng thời**: Đăng một lúc lên nhiều kênh; quản lý trạng thái từng kênh (*nháp $\rightarrow$ chờ $\rightarrow$ đang đăng $\rightarrow$ đã đăng / lỗi / đã bàn giao*).
- `[B]` **Hàng đợi ngoại tuyến (Offline Queue)**: Mất mạng vẫn bấm đăng được, hệ thống tự động đẩy bài lên khi có kết nối trở lại.
- `[B]` **Chống trùng lặp**: Tránh đăng trùng bài khi người dùng bấm nút 2 lần hoặc khi retry.
- `[B]` **Xử lý lỗi**: Tự động thử lại khi lỗi mạng / rate limit; hiển thị rõ lý do nếu lỗi quyền hạn (permissions) hoặc nội dung vi phạm.
- `[B]` **Lưu vết**: Lưu URL bài viết đã đăng thành công, mở được bài viết gốc trên nền tảng.

---

### F6. Đọc comment và trả lời
- `[B]` **Hộp thư comment hợp nhất**: Gom comment từ mọi kênh về một nơi; có bộ lọc theo kênh, theo bài viết, theo trạng thái.
- `[B]` **Đồng bộ**: Hỗ trợ kéo để làm mới (pull-to-refresh) kết hợp đồng bộ nền (background sync).
- `[B]` **Phân loại bằng AI**: Tự động phân loại comment thành: câu hỏi, khen ngợi, góp ý, tiêu cực, spam.
- `[B]` **Gợi ý trả lời**: AI soạn sẵn câu trả lời theo ngữ cảnh bài đăng và giọng văn chuẩn; người dùng có thể chỉnh sửa trước khi gửi.
- `[B]` **Tự động trả lời có kiểm soát**: Chỉ bật tự động trả lời cho nhóm comment "khen ngợi" và "câu hỏi thường gặp"; tuyệt đối **không** tự trả lời comment tiêu cực, comment chứa thông tin cá nhân hoặc khi AI chưa chắc chắn.
- `[N]` **Push Notification**: Bắn thông báo khi có comment mới, nhấn vào mở thẳng vào đúng comment đó. Lựa chọn 1 trong 2 giải pháp:
  - *(a)* Background App Refresh + Local Notification
  - *(b)* Sử dụng server side để nghe và bắn notification đến mobile app.

---

### F7. Theo dõi tăng trưởng
- `[B]` **Số liệu chi tiết từng bài**: Thống kê lượt like/thích, comment, share/chia sẻ, lượt hiển thị (nếu có), tỉ lệ tương tác.
- `[B]` **Biểu đồ tăng trưởng**: Lưu trữ số liệu định kỳ để vẽ biểu đồ tăng trưởng theo thời gian.
- `[B]` **Dashboard tổng quan**: Thống kê lượng follower theo thời gian, tương tác theo tuần, top bài viết hiệu quả nhất, bảng so sánh giữa các kênh.
- `[N]` Phân tích chuyên sâu theo chủ đề, định dạng bài viết và khung giờ đăng tối ưu.
- `[N]` Báo cáo tuần do AI viết: tóm tắt tăng/giảm chỉ số gì, nguyên nhân tại sao, tuần tới nên làm gì.
- `[N]` Home screen Widget tiện ích ngoài màn hình chính.
- `[N]` Đặt mục tiêu tăng trưởng và theo dõi tiến độ.

---

### F8. Quản lý API Key LLM (BYOK - Bring Your Own Key)
- `[B]` **Đa nhà cung cấp**: Hỗ trợ $\ge 2$ nhà cung cấp LLM (OpenAI, Anthropic, Google Gemini, OpenRouter...).
- `[B]` **Bảo mật**: Kiểm tra tính hợp lệ của key ngay khi nhập; lưu an toàn trong Secure Storage thiết bị; tuyệt đối không gửi key lên server trung gian; hiển thị dạng che (`••••`).
- `[B]` **Cấu hình Model linh hoạt**: Cho phép chọn model riêng cho từng tác vụ (model viết bài, model phân loại comment, model sinh ảnh).
- `[B]` **Giám sát chi phí**: Theo dõi số lượng token tiêu thụ và chi phí ước tính theo ngày/tháng.
- `[N]` Thiết lập hạn mức ngân sách ngày/tháng (chạm trần thì dừng Agent).
- `[N]` Tự chuyển sang nhà cung cấp phụ khi nhà cung cấp chính gặp sự cố (Failover).
- `[N]` Chế độ Demo dùng thử không cần nhập key.

---

### F9. Cài đặt chung & Kiến trúc
- `[B]` Hỗ trợ đa ngôn ngữ giao diện: Tiếng Việt / Tiếng Anh (VI/EN).
- `[B]` Hỗ trợ chế độ Offline: cho phép xem lại các bản nháp, comment và các số liệu đã lưu trong bộ nhớ máy.

---

## III. QUY TRÌNH QUẢN LÝ DỰ ÁN VÀ QUY CHUẨN LÀM VIỆC GIT (RẤT QUAN TRỌNG)

> [!IMPORTANT]
> **TIÊU CHÍ ĐÁNH GIÁ ĐIỂM SỐ CỦA GIẢNG VIÊN**:
> Thầy Hải nhấn mạnh sẽ **chấm điểm quá trình làm việc cực kỳ kỹ lưỡng** thông qua hoạt động thực tế trên GitHub (GitHub Project, Git Repo, Branch, Commit, Pull Request, Review).
> - **Tuyệt đối không có chuyện 1 - 2 bạn làm gánh team rồi cả nhóm cùng hưởng điểm chung**. Điểm của mỗi sinh viên sẽ tương xứng với các ticket được assign, code commit và các lượt review thực chất trên GitHub.

### 1. Quản lý công việc với GitHub Projects
- **Tạo Ticket cho từng tính năng**: Toàn bộ các tính năng từ F1 đến F9 (cả Bắt buộc và Nâng cao) khi thực hiện phải được mô tả thành các **Ticket / Issue** cụ thể trên **GitHub Project**.
- **Assignee**: Mỗi ticket **bắt buộc phải được assign cho thành viên chịu trách nhiệm**.
- **Cập nhật Status liên tục**: Phải cập nhật trạng thái của ticket theo đúng vòng đời thực tế:
  $$\text{Todo / Backlog} \longrightarrow \text{In Progress} \longrightarrow \text{In Review} \longrightarrow \text{Done}$$

### 2. Quy chuẩn phân nhánh Git (Branching Model)
- **Cấm commit trực tiếp lên nhánh chính (`main`)**.
- **Feature Branch**: Khi bắt tay implement bất kỳ ticket nào, thành viên phải checkout một nhánh riêng từ `main`:
  - Quy ước đặt tên nhánh: `feature/<ma_issue_hoac_ten_task>` *(ví dụ mẫu thầy chiếu: `feature/16-project-bootstrap`, `Feat/4-omnichannel-integration`)*.
- Tất cả commit trong nhánh feature phải liên quan trực tiếp đến issue đang xử lý.

### 3. Quy chuẩn tạo Pull Request (PR)
- Sau khi hoàn thành feature branch, tạo Pull Request trỏ về `main`.
- **Mô tả PR (PR Description) chi tiết theo Implementation Plan**:
  - Không được mở PR sơ sài chỉ có vài dòng hoặc để trống.
  - Phải trình bày rõ:
    - **Mục tiêu của PR**: Tóm tắt nội dung và kiến trúc áp dụng.
    - **Danh sách các Task cụ thể**: Mô tả chi tiết từng task con đã thực hiện (Kế hoạch thực hiện, file nào bị ảnh hưởng...).
    - **Link liên kết Ticket/Issue**: Đính kèm issue tương ứng (ví dụ: `Closes #16` hoặc `Development: Phase 1 Project Bootstrap`).
    - **Minh chứng (Demo link nếu có)**: Video/ảnh chụp kết quả kiểm thử.

### 4. Quy định nghiêm ngặt về Code Review & Merge PR
- **Tối thiểu 2 Reviewer**: Mỗi Pull Request **bắt buộc phải được request và có ít nhất 2 thành viên còn lại trong nhóm review và Approve (`✓ Approved`)**.
- **Chất lượng Review**:
  - Không review hời hợt "LGTM" hay approve tự động. Phải kiểm tra đối chiếu checklist (cấu trúc code, naming convention, xử lý luồng, bảo mật...).
  - Các thành viên phải comment nhận xét chi tiết vào code/PR (về kiến trúc, DI, xử lý exception, security token...).
- **Hỗ trợ bằng AI Review**: Có thể tích hợp công cụ AI (như GitHub Copilot, Copilot Balanced...) để rà quét và gợi ý review trước, sau đó các thành viên người thật vào xác nhận, comment bổ sung và approve.
- **Quyền Merge**: Chỉ khi **đủ tối thiểu 2 lượt Approve từ các thành viên khác** thì mới được phép merge PR vào nhánh `main`.

---

## IV. QUY ĐỊNH BÁO CÁO HÀNG TUẦN QUA SLACK

### 1. Kênh Slack của nhóm
- Mỗi nhóm chủ động tạo 1 Slack channel riêng trên workspace của lớp theo đúng cú pháp định dạng:
  $$\mathbf{2024\_31\_md\_<ma\_nhom>}$$
  *(Ví dụ: `2024_31_md_nhom1`, `2024_31_md_nhom2`)*
- Phải invite giảng viên (**Thầy Hải**) và đầy đủ tất cả các thành viên trong nhóm vào channel này.

### 2. Lịch nộp báo cáo
- Định kỳ vào **cuối ngày Thứ 7 hàng tuần**, nhóm phải tổng hợp và đăng báo cáo tiến độ tuần lên channel Slack của nhóm.

### 3. Template báo cáo chuẩn (bắt buộc dùng đúng mẫu)
```markdown
TUẦN <ID>

Current tasks:
• Task 1 <assignee>
• Task 2 <assignee>
• Task 3 <assignee>

Next plan:
• Task 1 <assignee>
• Task 2 <assignee>
• Task 3 <assignee>

Blockers:
• Blocker 1
• Blocker 2
```

---

## V. KẾ HOẠCH & LỊCH TRÌNH ĐÁNH GIÁ (TIMELINE)
- **Thời lượng học & triển khai đồ án**: Kéo dài khoảng **6 – 7 tuần** trong suốt môn học (vừa học lý thuyết vừa thực hiện đề tài tuần tự theo tuần).
- **Giai đoạn nước rút hoàn thiện**: Sau khi kết thúc môn học trên lớp, các nhóm có thêm khoảng **2 tuần** để chuẩn bị, polish sản phẩm và viết tài liệu.
- **Đánh giá cuối kỳ**:
  - Buổi **vấn đáp trực tiếp** với thầy Hải Phạm.
  - Nhóm sẽ demo chạy thực tế ứng dụng và trình bày toàn bộ kết quả, nhật ký Git/GitHub Project của cả hành trình.
