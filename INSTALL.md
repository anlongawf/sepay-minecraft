# Hướng dẫn Cài đặt SepayPlugin

## Bước 1: Cài đặt Plugin
1. Tải file `SepayPlugin-1.0-SNAPSHOT.jar`.
2. Bỏ vào thư mục `/plugins` của server.
3. Khởi động lại server để tạo file config.

## Bước 2: Cấu hình `config.yml`
Mở file `plugins/SepayPlugin/config.yml`:

```yaml
sepay:
  account_number: "SO_TAI_KHOAN"   # Số tài khoản nhận tiền
  bank_code: "MB"                  # Mã ngân hàng (MB, VCB, BIDV,...)
  api_key: "API_KEY_SEPAY"         # Lấy tại quản trị Sepay (nếu có)
  webhook_port: 25580              # Port chạy Webhook Server
```

## Bước 3: Mở Port (Quan trọng)
Bạn cần mở port `25580` (hoặc port bạn cấu hình) trên VPS/Modem để Sepay có thể gọi Webhook.
- Nếu dùng VPS Linux: `ufw allow 25580`
- Nếu chạy localhost: Cần dùng **Ngrok** hoặc **Cloudflare Tunnel** để public port.

## Bước 4: Cấu hình trên Sepay.vn
1. Đăng nhập https://my.sepay.vn
2. Vào mục **Cấu hình tích hợp** -> **Webhook**.
3. Thêm Webhook mới:
   - **URL**: `http://IP-CUA-BAN:25580` (hoặc domain nều có)
   - **Sự kiện**: Transaction Created (Giao dịch mới)
4. Lưu lại và Test thử (Nút "Gửi test").
   - Nếu Server Console hiện "Processed donation...", cài đặt thành công.

## Sử dụng
- Vào game gõ: `/nap 20000`
- Cầm bản đồ vừa nhận được trên tay -> Sẽ hiện QR Code.
- Quét QR bằng ứng dụng ngân hàng và thanh toán.
- Sau khoảng 5-10s, tiền sẽ được cộng.
