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
            // Support console for reload/top/history maybe? For now restrict basic usage.
            sender.sendMessage(plugin.getConfigManager().getMessage("only_player"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        
        switch (sub) {
            case "top":
                showTop(player);
                break;
            case "history":
                String target = (args.length > 1) ? args[1] : player.getName();
                if (!player.hasPermission("sepay.admin") && !target.equalsIgnoreCase(player.getName())) {
                     player.sendMessage("§cBạn chỉ có thể xem lịch sử của chính mình.");
                     return true;
                }
                showHistory(player, target);
                break;
            default:
                // Handle as amount
                 try {
                     double amount = Double.parseDouble(args[0]);
                     if (amount < 1000) throw new NumberFormatException();
                     generateQR(player, amount);
                 } catch (NumberFormatException e) {
                     sendHelp(player);
                 }
        }
        return true;
    }
    
    private void sendHelp(Player p) {
        p.sendMessage("§e===== SEPAY COMMANDS =====");
        p.sendMessage("§a/nap <số tiền> §7- Tạo mã QR nạp tiền.");
        p.sendMessage("§a/nap top §7- Xem BXH nạp thẻ.");
        p.sendMessage("§a/nap history [player] §7- Xem lịch sử giao dịch.");
    }
    
    private void showTop(Player p) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            p.sendMessage("§eĐang tải dữ liệu...");
            java.util.List<String> top = plugin.getDatabaseManager().getTopDonors(10);
            p.sendMessage("§6🏆 BẢNG XẾP HẠNG NẠP THẺ 🏆");
            if (top.isEmpty()) {
                p.sendMessage("§7Chưa có dữ liệu.");
            } else {
                for (String line : top) {
                    p.sendMessage("§e" + line);
                }
            }
        });
    }
    
    private void showHistory(Player p, String target) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
             p.sendMessage("§eĐang tải lịch sử của " + target + "...");
             java.util.List<String> history = plugin.getDatabaseManager().getTransactionHistory(target, 10);
             p.sendMessage("§6📜 LỊCH SỬ GIAO DỊCH: " + target);
             if (history.isEmpty()) {
                 p.sendMessage("§7Không tìm thấy giao dịch nào.");
             } else {
                 for (String line : history) {
                     p.sendMessage("§f" + line);
                 }
             }
        });
    }

    private void generateQR(Player player, double amount) {
        long amountLong = (long) amount;

        // Verify inventory space
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(plugin.getConfigManager().getMessage("inventory_full"));
            return;
        }

        ConfigManager cfg = plugin.getConfigManager();
        String prefix = cfg.getContentPrefix();
        String content = prefix + player.getName(); 
        
        player.sendMessage(cfg.getMessage("generating_qr"));

        // Async URL Generation
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String encodedContent = URLEncoder.encode(content, StandardCharsets.UTF_8.toString());
                String bank = cfg.getBankCode();
                String acc = cfg.getAccountNumber();
                String url = String.format("https://qr.sepay.vn/img?bank=%s&acc=%s&template=compact&amount=%d&des=%s",
                        bank, acc, amountLong, encodedContent);

                QRMapRenderer renderer = new QRMapRenderer(url);

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
    }
}
