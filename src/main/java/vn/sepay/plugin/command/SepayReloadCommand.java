package vn.sepay.plugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import vn.sepay.plugin.SepayPlugin;

public class SepayReloadCommand implements CommandExecutor {
    private final SepayPlugin plugin;

    public SepayReloadCommand(SepayPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.getConfigManager().reload();
        plugin.startWebhook(); // Restart webhook in case port changed
        sender.sendMessage(plugin.getConfigManager().getMessage("prefix") + "§aReloaded config!");
        return true;
    }
}
