# Kế hoạch Phát triển SepayPlugin (Paper 1.21)

## Mục tiêu
Tạo plugin Minecraft Paper 1.21 tích hợp cổng thanh toán Sepay qua QR Code và Webhook.

## Các tính năng chính
1.  **Command `/nap <amount>`**: 
    - Tạo mã giao dịch duy nhất (VD: `NAP<PlayerName>_<Random>`).
    - Tạo QR Code chứa thông tin chuyển khoản (Tuân thủ VietQR hoặc Link Sepay).
    - Cấp phát `Map` (Bản đồ) cho người chơi hiển thị QR Code này.
2.  **Map Renderer**:
    - Render hình ảnh QR Code lên bản đồ trong game.
3.  **Webhook Server**:
    - Chạy HTTP Server nhẹ (sử dụng `NanoHTTPD` hoặc `Sun HttpServer`) để lắng nghe thông báo từ Sepay.
    - Port cấu hình được `config.yml`.
4.  **Xử lý Giao dịch**:
    - Xác thực dữ liệu từ Webhook.
    - Khớp mã giao dịch với người chơi đang chờ.
    - Cộng tiền (Vault) hoặc chạy lệnh thưởng.
    - Lưu log để chống trùng lặp.

## Cấu trúc Dự án (Maven)
- **Group ID**: `vn.sepay`
- **Artifact ID**: `SepayPlugin`
- **Dependencies**:
    - `paper-api` (1.21-R0.1-SNAPSHOT)
    - `VaultAPI`
    - `zxing` (Tạo QR Code)
    - `NanoHTTPD` (Webhook Server - Shade vào plugin)

## Các bước thực hiện
1.  Khởi tạo `pom.xml` và cấu trúc thư mục.
2.  Viết lớp `ConfigManager` xử lý `config.yml`.
3.  Viết `QRHelper` để tạo ảnh QR từ chuỗi.
4.  Viết `MapManager` để xử lý render Map.
5.  Viết `NapCommand` xử lý logic lệnh.
6.  Viết `WebhookServer` lắng nghe callback.
7.  Tích hợp `Vault` và xử lý phần thưởng.
8.  Kiểm thử giả lập (Unit Test logic tạo mã).

## Ghi chú
- Yêu cầu Java 21 (Paper 1.21).
- Webhook cần Public IP hoặc Tunnel để hoạt động thực tế.
