package vn.sepay.plugin.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import vn.sepay.plugin.SepayPlugin;
import vn.sepay.plugin.config.ConfigManager;
import vn.sepay.plugin.utils.QRMapRenderer;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class NapCommand implements CommandExecutor {

    private final SepayPlugin plugin;

    public NapCommand(SepayPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("only_player"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage("§cUsage: /nap <amount>");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
            if (amount < 1000) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getConfigManager().getMessage("invalid_amount"));
            return true;
        }
        
        // Remove decimals for QR url usually to look clean
        long amountLong = (long) amount;

        // Verify inventory space
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(plugin.getConfigManager().getMessage("inventory_full"));
            return true;
        }

        ConfigManager cfg = plugin.getConfigManager();
        String prefix = cfg.getContentPrefix();
        String content = prefix + player.getName(); // Format: NAP PlayerName
        
        player.sendMessage(cfg.getMessage("generating_qr"));

        // Async URL Generation and Image Loading
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String encodedContent = URLEncoder.encode(content, StandardCharsets.UTF_8.toString());
                String bank = cfg.getBankCode();
                String acc = cfg.getAccountNumber();
                // Sepay URL
                String url = String.format("https://qr.sepay.vn/img?bank=%s&acc=%s&template=compact&amount=%d&des=%s",
                        bank, acc, amountLong, encodedContent);

                // Pre-load renderer
                QRMapRenderer renderer = new QRMapRenderer(url);

                // Sync back to give item
                Bukkit.getScheduler().runTask(plugin, () -> {
                    ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
                    MapMeta meta = (MapMeta) mapItem.getItemMeta();
                    MapView view = Bukkit.createMap(player.getWorld());
                    
                    view.getRenderers().clear();
                    view.addRenderer(renderer);
                    
                    meta.setMapView(view);
                    meta.setDisplayName("§aQR Nạp: " + amountLong + " VNĐ");
                    mapItem.setItemMeta(meta);
                    
                    player.getInventory().addItem(mapItem);
                    player.sendMessage(cfg.getMessage("map_given"));
                });

            } catch (Exception e) {
                e.printStackTrace();
                player.sendMessage("§cError creating QR Code.");
            }
        });

        return true;
    }
}
