# SepayPlugin - Nạp thẻ tự động qua QR Code

Tích hợp cổng thanh toán Sepay.vn vào máy chủ Minecraft (Paper 1.21).
Hỗ trợ tạo QR Code ngân hàng tự động và xử lý giao dịch qua Webhook.

## Tính năng
- **Command /nap <số tiền>**: Tạo bản đồ chứa QR Code chuyển khoản chính xác.
- **Tự động cộng tiền**: Xử lý callback từ Sepay Webhook và chạy lệnh nạp (Vault/Console).
- **Chống trùng lặp**: Log lại các mã giao dịch đã xử lý (SQLite/MySQL).
- **Offline Support**: Lưu giao dịch khi người chơi offline, trả thưởng khi online.
- **Discord Integration**: Gửi log nạp thẻ về kênh Discord Admin.
- **Config linh hoạt**: Tùy chỉnh ngân hàng, nội dung nạp, tỉ lệ quy đổi.

## Yêu cầu
- Java 21+
- Maven (để build source code)
- Paper 1.21
- Vault (Optional - nếu dùng lệnh eco give)
- Cổng (Port) mở cho Webhook

## Hướng dẫn cài đặt
Xem chi tiết tại [INSTALL.md](INSTALL.md).

---

## ☕ Donate & Support
Nếu plugin hữu ích, bạn có thể mời mình một ly cà phê nhé!
- **Ngân hàng**: MB Bank
- **Số tài khoản**: `0903982264`
- **Chủ tài khoản**: PHAN QUOC AN

## 💎 Tài trợ Hosting
Plugin được phát triển và vận hành mượt mà trên hạ tầng của **CloudCheap Asia**.
Nếu bạn cần tìm Hosting Minecraft / VPS High Performance giá rẻ, hãy ghé thăm:
👉 **[https://cloudcheap.asia/](https://cloudcheap.asia/)**
