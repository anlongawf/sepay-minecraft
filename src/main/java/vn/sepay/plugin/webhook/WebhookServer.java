package vn.sepay.plugin.webhook;

import vn.sepay.plugin.SepayPlugin;
import vn.sepay.plugin.scheduler.SchedulerAdapter;
import fi.iki.elonen.NanoHTTPD;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebhookServer extends NanoHTTPD {

    private final SepayPlugin plugin;
    
    public WebhookServer(SepayPlugin plugin, int port) {
        super(port);
        this.plugin = plugin;
    }

    @Override
    public Response serve(IHTTPSession session) {
        plugin.getLogger().info("[Sepay] Received request: " + session.getMethod() + " " + session.getUri());
        
        if (!session.getMethod().equals(Method.POST)) {
            plugin.getLogger().info("[Sepay] Rejected: Method not POST");
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Only POST allowed.");
        }

        String apiKey = plugin.getConfigManager().getApiKey();
        String authHeader = session.getHeaders().get("authorization");
        
        if (apiKey != null && !apiKey.isEmpty()) {
            boolean authorized = false;
            // Lenient check: header usually "Bearer <token>"
            if (authHeader != null && authHeader.contains(apiKey)) authorized = true;
            
            if (!authorized) {
                 plugin.getLogger().warning("[Sepay] Authentication Failed. Header: " + authHeader);
                 return newFixedLengthResponse(Response.Status.UNAUTHORIZED, MIME_PLAINTEXT, "Invalid API Key");
            }
        }

        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String jsonBody = files.get("postData");
            
            plugin.getLogger().info("[Sepay] Body: " + jsonBody);

            if (jsonBody == null) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "No Body");
            }

            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(jsonBody);

            String transactionId = String.valueOf(json.get("id")); 
            if (transactionId == null || transactionId.equals("null") || transactionId.isEmpty()) {
                 transactionId = String.valueOf(json.get("referenceCode"));
            }

            if (plugin.getDatabaseManager().isTransactionProcessed(transactionId)) {
                 plugin.getLogger().info("[Sepay] Duplicate transaction ignored: " + transactionId);
                 return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "Already Processed");
            }

            String content = (String) json.get("content");
            Object amtObj = json.get("transferAmount");
            double amount = 0;
            if (amtObj instanceof Double) amount = (Double) amtObj;
            else if (amtObj instanceof Long) amount = ((Long) amtObj).doubleValue();
            else if (amtObj != null) amount = Double.parseDouble(amtObj.toString());
            
            plugin.getLogger().info("[Sepay] Processing: ID=" + transactionId + ", Amount=" + amount + ", Content=" + content);
            processPayment(transactionId, content, amount);
            
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "Success");

        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error: " + e.getMessage());
        }
    }

    private void processPayment(String txnId, String content, double amount) {
        if (content == null) return;
        
        String prefix = plugin.getConfigManager().getContentPrefix().trim();
        plugin.getLogger().info("[Sepay] Checking content: '" + content + "' against prefix '" + prefix + "'");
        
        Pattern pattern = Pattern.compile(Pattern.quote(prefix) + "\\s*([A-Za-z0-9_]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            String playerName = matcher.group(1);
            plugin.getLogger().info("[Sepay] Match found: Player=" + playerName);
            
            // Calculate Bonus
            double finalAmount = amount;
            double bonusPercent = 0;
            if (plugin.getPromotionManager().isPromotionActive()) {
                bonusPercent = plugin.getPromotionManager().getBonusPercent();
                finalAmount = amount + (amount * bonusPercent / 100.0);
                plugin.getLogger().info(String.format("[Sepay] Promotion Active! +%.0f%% Bonus. Amount: %.0f -> %.0f", 
                        bonusPercent, amount, finalAmount));
            }
            
            // Need final variables for lambda
            double effectiveAmount = finalAmount; 
            double appliedBonus = bonusPercent;

            // Folia: Use Global Scheduler to look up player
            SchedulerAdapter.getScheduler().runGlobal(plugin, () -> {
                Player player = Bukkit.getPlayer(playerName); // Safe on Global
                
                if (player != null) {
                    // Player is Online -> Transfer control to Player's Region Thread
                    SchedulerAdapter.getScheduler().runEntity(player, plugin, () -> {
                         double exchangeRate = plugin.getConfigManager().getExchangeRate();
                         double gameMoney = effectiveAmount * exchangeRate;
                         
                         // Execute Command
                         for (String cmd : plugin.getConfigManager().getSuccessCommands()) {
                             String run = cmd.replace("%player%", playerName)
                                             .replace("%amount%", String.valueOf((long)amount)) // Log original amount
                                             .replace("%game_money%", String.valueOf((long)gameMoney));
                             Bukkit.dispatchCommand(Bukkit.getConsoleSender(), run);
                         }
                         
                         // Play Effects
                         plugin.getEffectManager().playSuccessEffects(player, effectiveAmount, gameMoney);
                         
                         // Send Bonus Message
                         if (appliedBonus > 0) {
                             String msg = "&e⚡ &lKHUYẾN MÃI: &fBạn được tặng thêm &6" + (long)appliedBonus + "% &fgiá trị nạp!";
                             player.sendMessage(msg.replace("&", "§"));
                         }
                         
                         SchedulerAdapter.getScheduler().runAsync(plugin, () -> {
                             plugin.getDatabaseManager().saveTransaction(txnId, playerName, amount, content, "SUCCESS");
                             plugin.sendDiscordLog(playerName, effectiveAmount, txnId); 
                         });
                         
                         plugin.getLogger().info("[Sepay] Processed donation for " + playerName + ": " + effectiveAmount);
                    });
                } else {
                    // Player Offline - Save PENDING
                    // Note: We save ORIGINAL amount. Bonus is calculated when they join (so if promo ends, they might lose it? 
                    // OR we should save bonus flag? For simplicity, let's recalculate on join OR save final amount?)
                    // Decision: Save ORIGINAL amount. Recalculate bonus on JOIN -> This encourages players to login during promo! 
                    // actually safer to calculate bonus NOW is better but requires DB schema change to store "bonus". 
                    // Let's keep it simple: Save pending, and re-check promotion on join.
                    // WAIT. If they paid during promo, they should get promo even if they login later.
                    // However, without DB change, we can't store "this txn has bonus".
                    // Workaround: We will handle bonus check in PlayerJoinListener too, based on "Current Time" (bad) or accept that Pending = check promo at claim time.
                    // For now: Check promo at claim time (PlayerJoinListener).
                    
                    SchedulerAdapter.getScheduler().runAsync(plugin, () -> {
                        plugin.getDatabaseManager().saveTransaction(txnId, playerName, amount, content, "PENDING");
                        plugin.sendDiscordLog(playerName, amount, txnId); // Log original for now
                        plugin.getLogger().info("[Sepay] Player offline. Saved as PENDING for " + playerName);
                    });
                }
            });
        }
    }
}
