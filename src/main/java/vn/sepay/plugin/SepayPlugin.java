package vn.sepay.plugin;

import org.bukkit.plugin.java.JavaPlugin;
import vn.sepay.plugin.command.NapCommand;
import vn.sepay.plugin.command.SepayReloadCommand;
import vn.sepay.plugin.config.ConfigManager;
import vn.sepay.plugin.webhook.WebhookServer;
import java.util.logging.Logger;

public class SepayPlugin extends JavaPlugin {

    private static SepayPlugin instance;
    private ConfigManager configManager;
    private WebhookServer webhookServer;
    
    @Override
    public void onEnable() {
        System.setProperty("java.awt.headless", "true");
        instance = this;
        saveDefaultConfig();
        
        this.configManager = new ConfigManager(this);
        
        // Register Commands
        getCommand("nap").setExecutor(new NapCommand(this));
        getCommand("sepayreload").setExecutor(new SepayReloadCommand(this));
        
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
        getLogger().info("SepayPlugin has been disabled!");
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
