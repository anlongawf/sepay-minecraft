package vn.sepay.plugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import vn.sepay.plugin.command.SepayReloadCommand;
import vn.sepay.plugin.config.ConfigManager;
import vn.sepay.plugin.listener.PlayerJoinListener;
import vn.sepay.plugin.webhook.WebhookServer;
import vn.sepay.plugin.scheduler.SchedulerAdapter;
import java.util.logging.Logger;

public class SepayPlugin extends JavaPlugin {

    private static SepayPlugin instance;
    private ConfigManager configManager;
    private WebhookServer webhookServer;
    private vn.sepay.plugin.database.DatabaseManager databaseManager;
    private vn.sepay.plugin.utils.EffectManager effectManager;
    private vn.sepay.plugin.utils.PromotionManager promotionManager;
    private vn.sepay.plugin.utils.GuiManager guiManager;
    
    @Override
    public void onEnable() {
        System.setProperty("java.awt.headless", "true");
        instance = this;
        saveDefaultConfig();
        
        this.configManager = new ConfigManager(this);
        this.databaseManager = new vn.sepay.plugin.database.DatabaseManager(this);
        this.effectManager = new vn.sepay.plugin.utils.EffectManager(this);
        this.promotionManager = new vn.sepay.plugin.utils.PromotionManager(this);
        this.guiManager = new vn.sepay.plugin.utils.GuiManager(this);
        
        // Register Commands
        getCommand("bank").setExecutor(new vn.sepay.plugin.command.BankCommand(this));
        getCommand("sepayreload").setExecutor(new SepayReloadCommand(this));
        
        // Register Event
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(guiManager, this);

        // Start Webhook
        startWebhook();
        
        // Hook PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new vn.sepay.plugin.utils.SepayExpansion(this).register();
            getLogger().info("PlaceholderAPI found. Sepay Expansion registered!");
        }

        // Start Auto-Update Top Donors (Every 5 mins = 6000 ticks)
        vn.sepay.plugin.scheduler.SchedulerAdapter.getScheduler().runAsync(this, () -> {
            // Initial update
            databaseManager.updateTopDonorsCache();
            getLogger().info("Top Donors cached.");
            
            // Schedule periodic updates
            // Since we can't easily schedule repeating async with our interface (simplicity),
            // We can just rely on manual updates or simple loop if permitted, OR better: use Folia/Paper specific repeater.
            // But our IScheduler doesn't have runTimerAsync.
            // Let's implement a simple loop in a separate thread/task or add runTimerAsync.
            // PROPOSAL: Add runTimerAsync to IScheduler?
            // SHORTCUT: Just use Bukkit scheduler for Paper, and correct one for Folia?
            // Actually, for simplicity and Folia compat, best to use Folia's AsyncScheduler.runAtFixedRate
            // OR just a simple thread with sleep? (Not recommended in plugins).
            // Let's add runTimerAsync to IScheduler to be clean.
        });
        
        // Let's assume we updating IScheduler is cleaner.
        // For now, let's just do a simple "Run Task Later" loop for the cache update.
        startCacheUpdateTask();

        // Print Status
        printStartupStatus();
        
        getLogger().info("SepayPlugin has been enabled!");
    }
    
    private void startCacheUpdateTask() {
        vn.sepay.plugin.scheduler.SchedulerAdapter.getScheduler().runAsync(this, () -> {
            try {
                databaseManager.updateTopDonorsCache();
            } catch (Exception e) {
                getLogger().warning("Failed to update Top Donors cache: " + e.getMessage());
            }
            
            // Re-schedule in 5 mins (6000 ticks)
            // Note: runTaskLater runs on MAIN thread usually (unless specific async impl). 
            // Our IScheduler.runTaskLater implementation:
            // Paper: Bukkit.getScheduler().runTaskLater (Sync)
            // Folia: GlobalRegionScheduler.runDelayed (Sync)
            // So we need to chain: Sync Timer -> Run Async Update -> Sync Timer ...
            
            vn.sepay.plugin.scheduler.SchedulerAdapter.getScheduler().runTaskLater(this, this::startCacheUpdateTask, 6000L);
        });
    }

    private void printStartupStatus() {
        getLogger().info("==========================================");
        getLogger().info("       SEPAY PLUGIN - STATUS CHECK        ");
        getLogger().info("==========================================");
        
        // 1. Check Bank Info
        String bank = configManager.getBankCode();
        String acc = configManager.getAccountNumber();
        getLogger().info(String.format("1. Bank Info: %s - %s %s", 
                bank, acc, (bank.isEmpty() || acc.isEmpty()) ? "❌ (Missing)" : "✅"));

        // 2. Check API Key
        String key = configManager.getApiKey();
        String maskedKey = (key == null || key.length() < 5) ? "Invalid" : 
                           key.substring(0, 4) + "****" + key.substring(Math.max(0, key.length() - 4));
        boolean hasKey = key != null && !key.isEmpty();
        getLogger().info(String.format("2. API Key: %s %s", 
                maskedKey, hasKey ? "✅" : "❌ (CRITICAL: Webhook unsecured!)"));

        // 3. Webhook Port
        boolean webhookRunning = (webhookServer != null && webhookServer.isAlive());
        int port = configManager.getWebhookPort();
        getLogger().info(String.format("3. Webhook: Port %d %s", 
                port, webhookRunning ? "✅ (Listening)" : "❌ (Failed to start)"));

        // 4. Content Prefix
        getLogger().info("4. Prefix: " + configManager.getContentPrefix());
        
        // 5. Promotion
        boolean promo = promotionManager.isPromotionActive();
        getLogger().info(String.format("5. Promotion: %s (Bonus: %.0f%%)", 
                promo ? "ACTIVE ✅" : "Inactive", promotionManager.getBonusPercent()));

        // 6. IPN URL Hint
        getLogger().info("------------------------------------------");
        getLogger().info("Please configure IPN on Sepay.vn as:");
        getLogger().info("URL: http://<YOUR_SERVER_IP>:" + port);
        getLogger().info("==========================================");
    }

    @Override
    public void onDisable() {
        if (webhookServer != null) {
            webhookServer.stop();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("SepayPlugin has been disabled!");
    }
    
    public vn.sepay.plugin.database.DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public vn.sepay.plugin.utils.EffectManager getEffectManager() {
        return effectManager;
    }

    public vn.sepay.plugin.utils.PromotionManager getPromotionManager() {
        return promotionManager;
    }

    public vn.sepay.plugin.utils.GuiManager getGuiManager() {
        return guiManager;
    }
    
    public void sendDiscordLog(String user, double amount, String id) {
        SchedulerAdapter.getScheduler().runAsync(this, () -> {
            String url = getConfig().getString("discord.webhook_url");
            if (url == null || url.isEmpty()) return;
            
            String title = getConfig().getString("discord.title", "Donation Received");
            String footer = getConfig().getString("discord.footer", "Sepay");
            String colorHex = getConfig().getString("discord.color", "#00FF00");
            int color = 65280; // Default Green
            try { color = Integer.decode(colorHex); } catch (Exception e) {}
            
            String desc = "**User:** " + user + "\n" +
                          "**Amount:** " + String.format("%,.0f", amount) + " VNĐ\n" +
                          "**ID:** " + id;
                          
            String json = vn.sepay.plugin.utils.DiscordWebhook.buildEmbedJson(title, desc, color, footer);
            new vn.sepay.plugin.utils.DiscordWebhook(url).send(json);
        });
    }

    public void startWebhook() {
        if (webhookServer != null) {
            webhookServer.stop();
        }
        try {
            int port = configManager.getWebhookPort();
            webhookServer = new WebhookServer(this, port);
            webhookServer.start();
            getLogger().info("Webhook server started on port " + port);
        } catch (Exception e) {
            getLogger().severe("Could not start Webhook Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static SepayPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
