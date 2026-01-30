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
        
        getLogger().info("SepayPlugin has been enabled!");
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
