package vn.sepay.plugin.listener;

import vn.sepay.plugin.SepayPlugin;
import vn.sepay.plugin.database.TransactionData;
import vn.sepay.plugin.scheduler.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class PlayerJoinListener implements Listener {

    private final SepayPlugin plugin;

    public PlayerJoinListener(SepayPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        
        // Run Async to check DB
        SchedulerAdapter.getScheduler().runAsync(plugin, () -> {
            List<TransactionData> pending = plugin.getDatabaseManager().getPendingTransactions(playerName);
            
            if (pending.isEmpty()) return;

            // Run Sync to give rewards
            SchedulerAdapter.getScheduler().runEntity(player, plugin, () -> {
                if (!player.isOnline()) return;

                for (TransactionData txn : pending) {
                    double amount = txn.getAmount();
                    double exchangeRate = plugin.getConfigManager().getExchangeRate();
                    
                    // CHECK PROMOTION FOR PENDING
                    // Logic: If promotion is active NOW, they get bonus.
                    // Ideally we should check if promotion was active THEN, but we didn't save timestamp/status.
                    // So we treat it as "Claim time promotion".
                    double bonusPercent = 0;
                    if (plugin.getPromotionManager().isPromotionActive()) {
                        bonusPercent = plugin.getPromotionManager().getBonusPercent();
                    }
                    
                    double finalAmount = amount + (amount * bonusPercent / 100.0);
                    double gameMoney = finalAmount * exchangeRate;

                    for (String cmd : plugin.getConfigManager().getSuccessCommands()) {
                        String run = cmd.replace("%player%", playerName)
                                        .replace("%amount%", String.valueOf((long)amount))
                                        .replace("%game_money%", String.valueOf((long)gameMoney));
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), run);
                    }
                    
                    // Play Effects
                    plugin.getEffectManager().playSuccessEffects(player, finalAmount, gameMoney);
                    
                    // Promotion Msg
                    if (bonusPercent > 0) {
                         String msg = plugin.getConfig().getString("promotion.messages.bonus_received", 
                                 "&e⚡ &lKHUYẾN MÃI: &fBạn được tặng thêm &6%bonus%% &fgiá trị nạp!");
                         player.sendMessage(msg.replace("%bonus%", String.valueOf((long)bonusPercent)).replace("&", "§"));
                    }
                    
                    // Mark as SUCCESS (Async update)
                    SchedulerAdapter.getScheduler().runAsync(plugin, () -> {
                        plugin.getDatabaseManager().updateStatus(txn.getId(), "SUCCESS");
                    });
                    
                    plugin.getLogger().info("[Sepay] Processed pending donation for " + playerName + ": " + finalAmount);
                }
            });
        });
    }
}
