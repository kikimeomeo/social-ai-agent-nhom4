# 📜 QUY CHUẨN LÀM VIỆC & ĐÓNG GÓP MÃ NGUỒN (CONTRIBUTING GUIDE)
> **Dự án:** SocialAI - Mobile AI Agent Xây Dựng Kênh Social Media  
> **Nhóm thực hiện:** Nhóm 04 (LTTBDD - GV: Thầy Hải Phạm)  
> **Mục tiêu:** Đảm bảo toàn bộ thành viên tuân thủ đúng quy trình làm việc chuẩn công nghiệp, minh bạch đóng góp cá nhân và đáp ứng 100% tiêu chí chấm điểm của giảng viên.

---

## ⚠️ NGUYÊN TẮC BẤT DI BẤT DỊCH (ĐỌC KỸ TRƯỚC KHI CODE)
1. **CẤM COMMIT / PUSH TRỰC TIẾP LÊN NHÁNH `main`:**
   * Mọi dòng code đưa vào dự án bắt buộc phải đi qua quy trình: **Tạo Issue $\rightarrow$ Nhánh riêng $\rightarrow$ Tạo Pull Request $\rightarrow$ Đủ 2 Review Approve $\rightarrow$ Merge**.
   * Nhánh `main` đã được bật Ruleset bảo vệ: Chặn push trực tiếp, chặn force push, yêu cầu tối thiểu 2 approvals.
2. **ĐÁNH GIÁ ĐIỂM SỐ CÁ NHÂN TÁCH BIỆT:**
   * Giảng viên chấm điểm quá trình dựa trên: **Số lượng Issue được assign**, **Số Commit thực tế**, **Chất lượng Pull Request** và **Lượt Code Review thực chất**.
   * Không có hình thức "gánh team" hay hưởng điểm chung. Thành viên làm sai quy trình hoặc không có đóng góp trên Git tự chịu trách nhiệm điểm số trước giảng viên.

---

## 🌳 1. QUY CHUẨN GITHUB PROJECTS & PHÂN NHÁNH (BRANCHING)
### Quản lý Ticket trên GitHub Projects:
* Mọi tính năng (F1 $\rightarrow$ F9) phải có **Issue** cụ thể trên GitHub Project và bắt buộc **Assign** cho thành viên chịu trách nhiệm.
* Cập nhật trạng thái ticket liên tục theo đúng vòng đời thực tế: `Todo / Backlog` $\rightarrow$ `In Progress` $\rightarrow$ `In Review` $\rightarrow$ `Done`.

### Tạo nhánh làm việc:
Trước khi làm bất kỳ task nào, kéo code mới nhất từ `main` về và tạo nhánh mới:
```bash
git checkout main
git pull origin main
git checkout -b <tên_nhánh>
```

### Quy ước đặt tên nhánh:
* Cú pháp tiền tố:
  * `feature/<issue_id>-<tên-ngắn-gọn>`: Nhánh làm tính năng mới
  * `fix/<issue_id>-<mô-tả-lỗi>`: Nhánh sửa lỗi
  * `docs/<mô-tả-tài-liệu>`: Nhánh viết tài liệu, README, quy chế
  * `refactor/<tên-task>`: Nhánh tái cấu trúc mã nguồn
* Ví dụ hợp lệ:
  * `feature/4-create-post-ui`
  * `feature/16-analytics-kpi-cards`
  * `fix/11-threads-rate-limit`
  * `docs/guidelines-and-pr-template`

---

## 💬 2. QUY CHUẨN COMMIT MESSAGE
Thông điệp commit phải rõ ràng, ngắn gọn và sử dụng định dạng Conventional Commits:
* `feat(<scope>): <mô tả>`: Thêm tính năng mới (Ví dụ: `feat(analytics): add overview kpi cards and chart container`)
* `fix(<scope>): <mô tả>`: Sửa lỗi (Ví dụ: `fix(auth): handle token expiration on threads api`)
* `ui(<scope>): <mô tả>`: Cập nhật giao diện XML / Style Material 3
* `docs(<scope>): <mô tả>`: Cập nhật tài liệu, README, báo cáo tuần
* `refactor(<scope>): <mô tả>`: Tối ưu cấu trúc code, không đổi tính năng

---

## 🚀 3. QUY TRÌNH MỞ PULL REQUEST (PR)
Khi hoàn thành nhánh feature, đẩy lên GitHub và mở Pull Request trỏ về `main`.

### Yêu cầu bắt buộc khi mở PR:
1. **Tiêu đề PR:** Phải theo chuẩn: `feat/fix(<scope>): tóm tắt ngắn gọn #<issue_id>` *(Ví dụ: `feat(analytics): add kpi cards and AI insights #16`)*.
2. **Nội dung PR:** Phải điền đầy đủ theo form mẫu tự động (PR Template):
   * Tóm tắt mục tiêu và danh sách file thay đổi.
   * Liên kết đóng Issue tương ứng: Bắt buộc có dòng `Closes #<id>` (ví dụ `Closes #11`).
   * Đính kèm hình ảnh / GIF demo nếu PR liên quan đến thay đổi giao diện UI.
3. **Request Reviewers:** Tag ít nhất 2 thành viên khác trong nhóm vào mục Reviewers.

---

## 🔍 4. QUY CHUẨN CODE REVIEW (TIÊU CHÍ CHẤM ĐIỂM CỦA THẦY)
> Giảng viên kiểm tra rất kỹ lịch sử Review trên GitHub. Review hời hợt sẽ bị trừ điểm quá trình.

### Quy tắc Review:
1. **Đủ 2 Lượt Approve mới được Merge:** PR chưa đủ 2 Approve tuyệt đối không được merge vào `main`.
2. **Cấm Approve suông / Cấm comment "LGTM":**
   * Người review bắt buộc phải bấm vào tab **Files changed**, đọc từng dòng code và **để lại ít nhất 1 comment nhận xét trực tiếp vào dòng code (Line-level comment)**.
   * Nội dung nhận xét: Góp ý về kiến trúc, kiểm tra null/chia cho 0, chuẩn đặt tên, xử lý ngoại lệ, bảo mật API key, hoặc xác nhận logic đúng.
3. **Ứng dụng AI Review:**
   * Khuyến khích dùng AI để quét mã nguồn hỗ trợ phát hiện lỗi tiềm ẩn.
   * Tuy nhiên, người review phải đọc hiểu, chọn lọc các điểm hợp lý và tự tay để lại nhận xét khách quan, văn phong tự nhiên.

---

## 📢 5. BÁO CÁO HÀNG TUẦN TRÊN SLACK
* **Kênh Slack:** `2024_31_md_nhom4` (đã invite Thầy Hải).
* **Thời hạn:** Trước **23h59 Thứ 7 hàng tuần**.
* **Định dạng báo cáo bắt buộc:**
```markdown
TUẦN <ID>

Current tasks:
• Task 1 <@tên_thành_viên>
• Task 2 <@tên_thành_viên>

Next plan:
• Task 1 <@tên_thành_viên>
• Task 2 <@tên_thành_viên>

Blockers:
• Không có / Mô tả khó khăn kỹ thuật nếu có
```
