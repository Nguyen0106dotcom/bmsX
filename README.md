# Thiên Cơ Số — Ứng Dụng Dự Đoán & Phân Tích Xổ Số Miền Bắc (Android)

Ứng dụng Android viết bằng **Kotlin & Jetpack Compose (Material 3, MVVM)**, được chuyển đổi hoàn chỉnh từ hệ thống phân tích thống kê *Thiên Cơ Số (v6.2.0)* dành cho **Xổ số Kiến thiết Miền Bắc (XSMB)** và các dòng **Vietlott** (Mega 6/45, Power 6/55, Lotto 5/35, Max 3D / 3D Pro).

## Tính năng chính
- **Dự đoán đa thuật toán (`Dự Đoán`)**: Tổng hợp điểm trên nền `100.00` từ 5 thuật toán bản địa (*Điểm Lịch Sử Tối Ưu V13*, *Bản Tối Ưu*, *Điểm Lô Gan & Mốc Thời Gian*, *Phạt Vị Trí Giải*, *Phạt Tần Suất 30 Ngày*) cùng các gói công thức JSON Bundle. Hiển thị Top 3 số vàng, đối chiếu KQXS thực tế, kiểm chứng hiệu suất tổng hợp theo khoảng ngày và xem phân rã điểm từng số.
- **Thuật toán, Tối ưu & AI Studio (`Thuật Toán`)**: Bật/tắt và gán trọng số cho từng thuật toán, tinh chỉnh tham số thủ công, chạy đánh giá đa chu kỳ 30 ngày, tối ưu tham số tự động (coordinate ascent) và tạo công thức JSON Bundle mẫu.
- **Tra cứu KQXS & Phân tích sâu (`KQXS`)**: Bảng kết quả 27 giải XSMB truyền thống, thống kê nhanh trong ngày (*lô nhiều nháy*, *lô rơi*, *top gan lì*, *đầu–đuôi 0–9*) và tra cứu chuyên sâu lịch sử xuất hiện từng bộ số theo năm/tháng.
- **Giả lập Chơi Thử (`Chơi Thử`)**: Thử nghiệm chiến lược *Đề*, *Lô*, *Xiên 2/3/4* theo thứ hạng dự đoán trên dữ liệu lịch sử với quản lý vốn và nhật ký lãi/lỗ chi tiết.
- **Đồng bộ & Lưu trữ (`Cài Đặt`)**: Dữ liệu lịch sử đóng gói sẵn trong `app/src/main/assets/data/`, hỗ trợ đồng bộ cập nhật trực tuyến qua OkHttp và lưu cấu hình bằng Jetpack DataStore.

## Lưu ý
- Tính năng sinh thuật toán Gemini AI hiện chạy ở chế độ xem trước cục bộ (`PlaceholderAiRepository`) vì Firebase chưa được kích hoạt.
- Ứng dụng chỉ phục vụ mục đích nghiên cứu thống kê và phân tích dữ liệu lịch sử.
