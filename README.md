SepayPlugin - Nạp tiền tự động qua QR Code

Tích hợp cổng thanh toán Sepay.vn vào máy chủ Minecraft (Paper 1.21).
Hỗ trợ tạo QR Code ngân hàng tự động và xử lý giao dịch qua Webhook.

Tính năng
- Command /nap <số tiền>: Tạo bản đồ chứa QR Code chuyển khoản chính xác.
- Tự động cộng tiền: Xử lý callback từ Sepay Webhook và chạy lệnh nạp (Vault/Console).
- Chống trùng lặp: Log lại các mã giao dịch đã xử lý (SQLite/MySQL).
- Offline Support: Lưu giao dịch khi người chơi offline, trả thưởng khi online.
- Discord Integration: Gửi log nạp thẻ về kênh Discord Admin.
- Hiệu ứng: Title và Pháo hoa khi nạp thành công.

Yêu cầu
- Java 21+
- Paper 1.21
- Cổng (Port) mở cho Webhook (Mặc định: 30079)

Hướng dẫn cài đặt nhanh
1. Tải Plugin và bỏ vào thư mục plugins.
2. Khởi động server và mở file config.yml.
3. Trên Sepay.vn, tạo Webhook mới:
   - URL: http://<IP>:30079
   - Kiểu chứng thực: API Key
   - API Key: (Lấy từ config.yml dán qua)
4. Reload plugin bằng lệnh /sepayreload.

Donate & Support
Nếu cần hỗ trợ cài đặt hoặc báo lỗi, hãy tham gia Discord:
👉 [Discord Support](https://discord.gg/k99aC2mYJj)

Nếu plugin hữu ích, bạn có thể mời mình một ly cà phê nhé!
- Ngân hàng: MB Bank
- Số tài khoản: 0903982264
- Chủ tài khoản: PHAN QUOC AN

Tài trợ Hosting
Plugin được phát triển và vận hành mượt mà trên hạ tầng của CloudCheap Asia.
👉 [https://cloudcheap.asia/](https://cloudcheap.asia/)
