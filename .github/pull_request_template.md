## 📌 Mô tả thay đổi (Description)
<!-- Tóm tắt ngắn gọn mục tiêu của PR, các tính năng hoặc sửa lỗi vừa thực hiện -->

- **Mục tiêu chính:** 
- **Các thành phần ảnh hưởng:** 

---

## 🔗 Liên kết Issue (Linked Issue)
<!-- Bắt buộc liên kết Issue để GitHub tự động đóng ticket khi merge -->
Closes #

---

## 📂 Loại thay đổi (Type of Change)
<!-- Đánh dấu [x] vào các ô phù hợp -->
- [ ] 🚀 **Feature**: Tính năng mới
- [ ] 🐛 **Bugfix**: Sửa lỗi
- [ ] 🎨 **UI/UX**: Cập nhật giao diện / Tài nguyên giao diện
- [ ] 🛠️ **Refactor**: Tối ưu mã nguồn, không đổi chức năng
- [ ] 📝 **Documentation**: Cập nhật tài liệu, README

---

## 📸 Minh chứng kiểm thử / Demo (Screenshots / Evidence)
<!-- Nếu có thay đổi giao diện UI hoặc luồng hoạt động, hãy chèn ảnh chụp màn hình / GIF / Log tại đây -->
*(Dán ảnh màn hình hoặc video demo nếu có)*

---

## ✅ Checklist tự kiểm tra của tác giả (Self-Checklist)
<!-- Đảm bảo bạn đã kiểm tra các mục này TRƯỚC KHI request review -->
- [ ] Code đã chạy thử nghiệm thực tế trên máy ảo / máy thật và không bị crash.
- [ ] Tuân thủ kiến trúc phân tầng của dự án (`agent/`, `data/`, `model/`, `repository/`, `ui/`, `util/`, `worker/`).
- [ ] Đã kiểm tra các trường hợp ngoại lệ (chia cho 0, mất mạng, dữ liệu rỗng).
- [ ] Đặt tên biến, hàm theo đúng chuẩn camelCase và có ý nghĩa rõ ràng.
- [ ] Tuyệt đối **không** hardcode API key, mật khẩu hay token nhạy cảm trong mã nguồn.

---

## 👥 Checklist dành cho Reviewer (Ít nhất 2 Approvals)
<!-- Dành cho các thành viên review PR -->
- [ ] Đã vào tab **Files changed** và để lại ít nhất **1 comment trực tiếp vào dòng code** (Line-level comment).
- [ ] Kiểm tra code đáp ứng đầy đủ yêu cầu của Issue được giao.
- [ ] Xác nhận không có conflict với nhánh `main`.
