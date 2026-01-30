# SepayPlugin - Nạp thẻ tự động qua QR Code

Tích hợp cổng thanh toán Sepay.vn vào máy chủ Minecraft (Paper 1.21).
Hỗ trợ tạo QR Code ngân hàng tự động và xử lý giao dịch qua Webhook.

## Tính năng
- **Command /nap <số tiền>**: Tạo bản đồ chứa QR Code chuyển khoản chính xác.
- **Tự động cộng tiền**: Xử lý callback từ Sepay Webhook và chạy lệnh nạp (Vault/Console).
- **Chống trùng lặp**: Log lại các mã giao dịch đã xử lý.
- **Config linh hoạt**: Tùy chỉnh ngân hàng, nội dung nạp, tỉ lệ quy đổi.

## Yêu cầu
- Java 21+
- Maven (để build source code)
- Paper 1.21
- Vault (Optional - nếu dùng lệnh eco give)
- Cổng (Port) mở cho Webhook (Mặc định 25580)

## Hướng dẫn cài đặt
Xem chi tiết tại [INSTALL.md](INSTALL.md).
