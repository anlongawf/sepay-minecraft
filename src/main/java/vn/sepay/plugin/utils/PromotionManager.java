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

        String endDateStr = plugin.getConfig().getString("promotion.end_date", "");
        
        // If empty -> Forever active (as long as enabled=true)
        if (endDateStr == null || endDateStr.isEmpty()) {
            return true;
        }

        try {
            LocalDateTime now = LocalDateTime.now(ZONE_VN);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            LocalDateTime endDate = LocalDateTime.parse(endDateStr, formatter);
            
            return now.isBefore(endDate);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid promotion.end_date format! Use 'dd/MM/yyyy HH:mm'. Error: " + e.getMessage());
            return false;
        }
    }

    public double getBonusPercent() {
        if (isPromotionActive()) {
            return plugin.getConfig().getDouble("promotion.bonus_percent", 0.0);
        }
        return 0.0;
    }
}
