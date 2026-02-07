# Feature Implementation Plan: Promotions, GUI, Holograms

## 1. Promotion System (Khuyến Mãi)
Hệ thống tự động cộng thêm % giá trị nạp trong khung giờ vàng.

### Cấu hình `config.yml`
```yaml
promotion:
  enabled: true
  bonus_percent: 20 # Cộng thêm 20%
  # Khung giờ vàng (HH:mm)
  happy_hours:
    - "18:00-20:00"
    - "08:00-10:00"
  # Ngày vàng (Thứ 2 -> Chủ Nhật)
  happy_days:
    - "SATURDAY"
    - "SUNDAY"
```

### Logic Implementation
- Class `PromotionManager`:
    - `checkPromotion()`: Kiểm tra thời gian thực so với config.
    - `calculateBonus(amount)`: Trả về số tiền thực nhận (Gốc + Bonus).
- Tích hợp vào `WebhookServer`: Khi xử lý nạp tiền, gọi `calculateBonus`.

## 2. GUI Menu (/nap)
Thay thế lệnh chat bằng giao diện Inventory trực quan.

### Design
- Command `/nap` (không tham số) -> Mở GUI `Sepay Nạp Thẻ`.
- Items:
    - Các gói nạp cố định: 10k, 20k, 50k, 100k, 200k, 500k.
    - Item thông tin: Hiển thị tỉ lệ nạp và khuyến mãi hiện tại.
    - Item Custom: Cho phép nhập số tiền tùy ý (AnvilUI hoặc Chat input).

### Logic Implementation
- Class `GuiManager`: Xử lý `InventoryClickEvent`.
- Khi click item -> Gọi `NapCommand.generateQR`.

## 3. Hologram Leaderboard (Bảng Xếp Hạng)
Hiển thị Top Nạp Thẻ bằng Hologram tại Spawn.
**Yêu cầu**: Plugin `DecentHolograms` (Soft Dependency).

### Logic Implementation
- Class `HologramManager`:
    - `updateHologram()`: Lấy dữ liệu từ Database (Async) -> Format text -> Update qua DH API.
    - `startAutoUpdateTask()`: Chạy task 5-10 phút/lần để refresh.
- Placeholders:
    - `%sepay_top_name_1%`, `%sepay_top_amount_1%`... để dùng cho các plugin Hologram khác nếu không dùng DH trực tiếp. (Dùng `PlaceholderAPI`).

## Task Checklist
- [ ] **Promotion System**
    - [ ] Update `config.yml`
    - [ ] Implement `PromotionManager`
    - [ ] Integreate into `WebhookServer`

- [ ] **GUI Manager**
    - [ ] Create `GuiManager` & Listener
    - [ ] Map `/nap` command to open GUI

- [ ] **Hologram & PAPI**
    - [ ] Add `PlaceholderAPI` & `DecentHolograms` dependencies
    - [ ] Implement `SepayExpansion` (PAPI)
    - [ ] Implement `HologramTask`
