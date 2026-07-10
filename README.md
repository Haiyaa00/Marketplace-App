# 🛒 Marketplace - Student Trading Platform

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Java-orange.svg)](https://www.java.com/)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM-blue.svg)](https://developer.android.com/topic/libraries/architecture/viewmodel)
[![Database](https://img.shields.io/badge/Local%20DB-Room-red.svg)](https://developer.android.com/training/data-storage/room)
[![Backend](https://img.shields.io/badge/Backend-Firebase-yellow.svg)](https://firebase.google.com/)

**Marketplace** là một ứng dụng di động trao đổi, mua bán sản phẩm nội bộ dành riêng cho sinh viên Đại học Kiến trúc Hà Nội (HAU). Dự án được xây dựng trên triết lý **Offline-First**, giúp tối ưu hóa tốc độ tải trang, đảm bảo tính an toàn dữ liệu và đem lại trải nghiệm mượt mà trong môi trường thực tế.

---

## 📸 Giao diện ứng dụng
*(Phần này dành cho các ảnh chụp màn hình thực tế của bạn)*

<table>
  <tr>
    <td><img src="" width="200" alt="Login Screen"/></td>
    <td><img src="" width="200" alt="Home Screen"/></td>
    <td><img src="" width="200" alt="Post Screen"/></td>
    <td><img src="" width="200" alt="Chat Screen"/></td>
  </tr>
  <tr>
    <td align="center">Xác thực sinh viên</td>
    <td align="center">Trang chủ & Auto-Slider</td>
    <td align="center">Quản lý tin đa năng</td>
    <td align="center">Nhắn tin Real-time</td>
  </tr>
</table>

---

## ✨ Tính năng cốt lõi

### 🔐 Hệ thống xác thực & Bảo mật (Authentication)
- **Ràng buộc Domain:** Chỉ cho phép đăng ký/đăng nhập bằng email sinh viên định dạng `@kientruchanoi.edu.vn`.
- **Xác thực Email (Email Verification):** Quy trình Polling tự động kiểm tra trạng thái click link xác thực từ Gmail trước khi kích hoạt tài khoản.
- **Auto-Login:** Lưu phiên đăng nhập mã hóa, tự động bỏ qua màn hình Login khi mở ứng dụng vào các lần tiếp theo.

### 🏠 Trang chủ & Tìm kiếm thông minh (Home & Search)
- **Kiến trúc Offline-First Cache:** Tải dữ liệu tức thời dưới 10ms từ SQLite (Room DB), giúp ứng dụng xem được sản phẩm ngay cả khi mất kết nối mạng.
- **Auto-Banner:** Hệ thống slider quảng cáo tự động chuyển trang sau mỗi 3 giây với hiệu ứng Zoom-out 3D và dấu chấm tròn viên thuốc (Pill-shape) co giãn hiện đại.
- **Tìm kiếm Offline Realtime:** Hỗ trợ gõ chữ tìm kiếm nhanh bằng câu lệnh SQL `LIKE` truy vấn trực tiếp dưới máy khách.
- **Lọc đa chiều:** Hệ thống kết hợp tìm kiếm từ khóa, chọn thẻ lọc nhanh (Material Chips) và sắp xếp theo Giá tăng/giảm.

### 📤 Đăng tin & Quản lý tin bán (Smart Posting)
- **Multi-image Upload:** Hỗ trợ chọn từ Gallery tối đa **10 hình ảnh** (sử dụng Android Photo Picker hiện đại có sẵn checkbox và tự động giới hạn), nén dung lượng ảnh và tải lên Cloudinary API chạy song song đa luồng.
- **Định dạng tiền tệ:** Tự động tách dấu phân cách hàng nghìn (VD: 1.000.000đ) thời gian thực khi người dùng nhập giá.
- **Địa chỉ hành chính Hà Nội:** Đọc và parse dữ liệu dạng cấu trúc JSON cục bộ (Thành phố -> Quận -> Phường -> Số nhà) giúp chuẩn hóa thông tin địa điểm giao dịch mà không cần mạng.
- **Chỉnh sửa & Xóa bài đăng:** Giao diện tách biệt. Cho phép chỉnh sửa thông tin cũ, thêm ảnh mới hoặc giữ ảnh cũ. Có dialog xác minh bảo mật trước khi xóa.

### 💬 Nhắn tin thời gian thực (Real-time Messaging)
- **Gửi nhận Tin nhắn Đa phương tiện:** Chat chữ và chat nhiều ảnh cùng lúc (Tối đa 5 ảnh). Tự động nén và đẩy ảnh thông qua Cloudinary CDN.
- **Nhãn thời gian thông minh:** Tự động tính toán mốc thời gian tương đối ("Hôm nay", "Hôm qua", hoặc Ngày/Tháng) để chia cách các đoạn hội thoại một cách tinh tế.
- **Inbox & Chỉ báo chưa đọc:** Hiển thị danh sách các phòng chat. In đậm chữ và hiện chấm xanh cho tin nhắn chưa đọc. Tự động xóa bôi đậm khi click mở phòng chat.
- **Thông báo Pop-up (Heads-up):** Hệ thống trượt nổi từ trên tai thỏ xuống khi có tin nhắn mới (Zalo Style), kèm theo số Badge đỏ tăng giảm thời gian thực trên thanh Bottom Navigation.

### ❤️ Sản phẩm Yêu thích (Wishlist)
- **Nút tương tác đồng bộ:** Nút Trái tim trên thanh Sticky AppBar và nút chính tự động đổi màu đồng bộ qua LiveData.
- **SQL INNER JOIN:** Sử dụng liên kết bảng kép giữa `favorites` và `products` để hiển thị danh sách sản phẩm yêu thích gọn gàng tại trang Profile cá nhân.

---

## 🏗 Kiến trúc kỹ thuật (Technical Stack)

Dự án áp dụng mô hình **MVVM (Model-View-ViewModel)** chuẩn Google kết hợp với **Repository Pattern**:

- **UI Framework:** Material Design 3, ViewBinding, ViewPager2, Navigation Component, Exposed Dropdown Menu, Choice Chips.
- **Jetpack Components:** ViewModel, LiveData, Room Persistence, Executor Thread Pool.
- **Networking & Backend:**
    - **Firebase Auth:** Quản lý định danh người dùng.
    - **Cloud Firestore:** Cơ sở dữ liệu NoSQL thời gian thực (Real-time DB).
    - **Cloudinary:** Quản lý, lưu trữ và tối ưu hóa tài nguyên hình ảnh.
- **Libraries:**
    - **Glide:** Tải và cache ảnh mượt mà, hỗ trợ load ảnh dạng vòng tròn.
    - **Gson:** Chuyển đổi dữ liệu đối tượng sang chuỗi JSON và ngược lại.
    - **SwipeRefreshLayout:** Cập nhật dữ liệu bằng thao tác vuốt.

---

## 📂 Cấu trúc thư mục tiêu biểu

```text
com.example.marketplace
├── data/
│   ├── local/        # Cấu hình Room DB, Entities, DAOs (Lưu cục bộ)
│   ├── remote/       # FirebaseManager, CloudinaryManager (Kết nối đám mây)
│   └── repository/   # Lớp điều phối dữ liệu (Single Source of Truth)
├── ui/
│   ├── auth/         # Login, Register
│   ├── home/         # Home Dashboard, BannerAdapter
│   ├── post/         # Đăng bài, Quản lý tin, SelectedImageAdapter, EditPostActivity
│   ├── chat/         # Nhắn tin real-time, ChatActivity
│   ├── contact/      # Inbox list, ChatRoomAdapter
│   └── detail/       # Chi tiết sản phẩm, SliderAdapter
├── model/            # Các lớp đối tượng (User, Product, Message, ChatRoom, Category)
└── utils/            # DateUtils, ImageUtils, AddressParser, DataMapper, Resource

🚀 Cài đặt dự án
Clone project:
git clone https://github.com/your-username/Marketplace-HAU.git
Cấu hình Firebase:
Tạo Project trên Firebase Console.
Thêm ứng dụng Android và tải file google-services.json vào thư mục app/.
Bật Authentication (Email/Password) và Firestore.
Tạo index kép trên Firestore cho Collection ChatRooms (Lọc theo participants và sắp xếp theo lastTimestamp).
Cấu hình Cloudinary:
Cập nhật CLOUD_NAME và UPLOAD_PRESET bên trong CloudinaryManager.java.
Build:
Sử dụng Android Studio (JDK 17+, Gradle 8.2+).