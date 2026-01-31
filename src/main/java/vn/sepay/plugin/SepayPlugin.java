package vn.sepay.plugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import vn.sepay.plugin.command.NapCommand;
import vn.sepay.plugin.command.SepayReloadCommand;
import vn.sepay.plugin.config.ConfigManager;
import vn.sepay.plugin.listener.PlayerJoinListener;
import vn.sepay.plugin.webhook.WebhookServer;
import java.util.logging.Logger;

public class SepayPlugin extends JavaPlugin {

    private static SepayPlugin instance;
    private ConfigManager configManager;
    private WebhookServer webhookServer;
    private vn.sepay.plugin.database.DatabaseManager databaseManager;
    private vn.sepay.plugin.utils.EffectManager effectManager;
    
    @Override
    public void onEnable() {
        System.setProperty("java.awt.headless", "true");
        instance = this;
        saveDefaultConfig();
        
        this.configManager = new ConfigManager(this);
        this.databaseManager = new vn.sepay.plugin.database.DatabaseManager(this);
        this.effectManager = new vn.sepay.plugin.utils.EffectManager(this);
        
        // Register Commands
        getCommand("nap").setExecutor(new NapCommand(this));
        getCommand("sepayreload").setExecutor(new SepayReloadCommand(this));
        
        // Register Event
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // Start Webhook
        startWebhook();
        
        // Print Status
        printStartupStatus();
        
        getLogger().info("SepayPlugin has been enabled!");
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
        
        // 5. IPN URL Hint
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
    
    public void sendDiscordLog(String user, double amount, String id) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
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
