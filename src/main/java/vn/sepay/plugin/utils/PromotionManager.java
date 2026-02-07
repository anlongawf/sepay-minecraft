package vn.sepay.plugin.utils;

import vn.sepay.plugin.SepayPlugin;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.List;

public class PromotionManager {

    private final SepayPlugin plugin;
    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");

    public PromotionManager(SepayPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isPromotionActive() {
        if (!plugin.getConfig().getBoolean("promotion.enabled", false)) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now(ZONE_VN);
        
        // 1. Check Happy Days
        List<String> happyDays = plugin.getConfig().getStringList("promotion.happy_days");
        String currentDay = now.getDayOfWeek().name(); // MONDAY, TUESDAY...
        
        boolean isHappyDay = happyDays.contains(currentDay);

        // 2. Check Happy Hours
        boolean isHappyHour = false;
        List<String> happyHours = plugin.getConfig().getStringList("promotion.happy_hours");
        LocalTime currentTime = now.toLocalTime();
        
        for (String range : happyHours) {
            try {
                String[] parts = range.split("-");
                if (parts.length == 2) {
                    LocalTime start = LocalTime.parse(parts[0].trim());
                    LocalTime end = LocalTime.parse(parts[1].trim());
                    
                    if (!currentTime.isBefore(start) && currentTime.isBefore(end)) {
                        isHappyHour = true;
                        break;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid happy_hours format: " + range);
            }
        }

        return isHappyDay || isHappyHour;
    }

    public double getBonusPercent() {
        if (isPromotionActive()) {
            return plugin.getConfig().getDouble("promotion.bonus_percent", 0.0);
        }
        return 0.0;
    }
}
