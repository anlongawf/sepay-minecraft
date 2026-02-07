package vn.sepay.plugin.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import vn.sepay.plugin.SepayPlugin;
import vn.sepay.plugin.database.TransactionData;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import java.util.List;

public class SepayExpansion extends PlaceholderExpansion {

    private final SepayPlugin plugin;

    public SepayExpansion(SepayPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "sepay";
    }

    @Override
    public String getAuthor() {
        return "Sepay";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // Keep expansion registered on reload
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        // %sepay_top_name_1%
        if (params.startsWith("top_name_")) {
            try {
                int index = Integer.parseInt(params.replace("top_name_", "")) - 1;
                return getTopName(index);
            } catch (NumberFormatException e) {
                return "Invalid Index";
            }
        }

        // %sepay_top_amount_1%
        if (params.startsWith("top_amount_")) {
            try {
                int index = Integer.parseInt(params.replace("top_amount_", "")) - 1;
                return getTopAmount(index);
            } catch (NumberFormatException e) {
                return "0";
            }
        }

        return null;
    }

    private String getTopName(int index) {
        List<TransactionData> top = plugin.getDatabaseManager().getCachedTopDonors();
        if (index < 0 || index >= top.size()) return "---";
        return top.get(index).getPlayer();
    }

    private String getTopAmount(int index) {
        List<TransactionData> top = plugin.getDatabaseManager().getCachedTopDonors();
        if (index < 0 || index >= top.size()) return "0";
        return String.format("%,.0f", top.get(index).getAmount());
    }
}
