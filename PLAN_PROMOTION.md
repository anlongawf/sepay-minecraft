# Implementation Plan: Promotion System

## 1. Goal
Implement a time-based promotion system that gives bonus rewards during specific hours and days.
**Key Requirements:**
- Timezone: `Asia/Ho_Chi_Minh` (GMT+7) hardcoded.
- Happy Days: Simple list (SATURDAY, SUNDAY).
- Happy Hours: List of time ranges (18:00-20:00).

## 2. Configuration Changes
Update `config.yml` to include:

```yaml
promotion:
  enabled: true
  bonus_percent: 20
  server_timezone: "Asia/Ho_Chi_Minh" # Default
  happy_hours:
    - "18:00-21:00" # 6PM to 9PM
  happy_days:
    - "SATURDAY"
    - "SUNDAY"
messages:
  promotion_active: "&e⚡ Khuyến mãi &6+%percent%% &eđang diễn ra!"
```

## 3. Code Changes

### 3.1. `PromotionManager` Class
Create `vn.sepay.plugin.utils.PromotionManager`:
- **Methods**:
    - `boolean isActive()`: Checks current time against config.
    - `double calculateBonus(double amount)`: Returns user's bonus amount.
    - `double getTotalAmount(double amount)`: Returns total (base + bonus).
    - `String getBonusMessage()`: Returns formatted message.

### 3.2. `WebhookServer` Integration
Modify `processPayment`:
- Before calculating `gameMoney`, check `promotionManager.isActive()`.
- If active:
    - `gameMoney = amount * exchangeRate * (1 + bonusPercent/100)`
    - Log "Promotion Applied: +X%"
    - Send special message/broadcast about bonus.

### 3.3. `NapCommand` Updates
- When running `/nap`, if promotion is active -> Send message "&e⚡ Đang có khuyến mãi 20%!".

## 4. Verification Plan
### Manual Test
1. Set `happy_hours` to current time in `config.yml`.
2. Reload plugin.
3. Run `/nap` -> Should see promotion message.
4. Simulate Webhook (Postman/Curl) -> Check logs/in-game money -> Should include bonus.
