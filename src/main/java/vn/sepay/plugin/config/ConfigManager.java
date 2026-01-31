package vn.sepay.plugin.config;

import org.bukkit.configuration.file.FileConfiguration;
import vn.sepay.plugin.SepayPlugin;

import java.util.List;

public class ConfigManager {
    private final SepayPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(SepayPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public String getAccountNumber() {
        return config.getString("sepay.account_number", "");
    }

    public String getBankCode() {
        return config.getString("sepay.bank_code", "MB");
    }
    
    public String getAccountName() {
        return config.getString("sepay.account_name", "SEPAY");
    }

    public String getApiKey() {
        return config.getString("sepay.api_key", "");
    }

    public int getWebhookPort() {
        return config.getInt("sepay.webhook_port", 25580);
    }
    
    public String getContentPrefix() {
        return config.getString("transaction.content_prefix", "NAP ");
    }
    
    public double getExchangeRate() {
        return config.getDouble("transaction.exchange_rate", 1.0);
    }
    
    public List<String> getSuccessCommands() {
        return config.getStringList("actions.on_success");
    }
    
    public String getMessage(String key) {
        String msg = config.getString("messages." + key, "");
        return msg.replace("&", "§");
    }
}
