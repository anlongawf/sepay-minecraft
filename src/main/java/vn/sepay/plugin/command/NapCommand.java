package vn.sepay.plugin.command;

import com.google.zxing.WriterException;
import vn.sepay.plugin.SepayPlugin;
import vn.sepay.plugin.config.ConfigManager;
import vn.sepay.plugin.scheduler.SchedulerAdapter;
import vn.sepay.plugin.utils.QRMapRenderer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
         
         if (args.length == 0) {
             plugin.getGuiManager().openGui(player);
             return true;
         }

         if (args.length == 1) {
             if (args[0].equalsIgnoreCase("reload")) {
                 if (player.hasPermission("sepay.admin")) {
                     plugin.reloadConfig();
                     player.sendMessage("§aSepay configuration reloaded!");
                 } else {
                     player.sendMessage("§cYou don't have permission.");
                 }
                 return true;
             }
             
             try {
                 double amount = Double.parseDouble(args[0]);
                 if (amount < 1000) throw new NumberFormatException();

                 ConfigManager cfg = plugin.getConfigManager();
                 String prefix = cfg.getContentPrefix();
                 String content = prefix + player.getName();

                 // Send Bank Info
                 String bank = plugin.getConfigManager().getBankCode();
                 String acc = plugin.getConfigManager().getAccountNumber();
                 String name = plugin.getConfigManager().getAccountName();
                 
                 player.sendMessage("§8==================================");
                 player.sendMessage("§6§lNGÂN HÀNG: §e" + bank);
                 player.sendMessage("§6§lSỐ TÀI KHOẢN: §b" + acc);
                 player.sendMessage("§6§lCHỦ TÀI KHOẢN: §f" + name);
                 
                 // Promotion Message
                 if (plugin.getPromotionManager().isPromotionActive()) {
                     double bonus = plugin.getPromotionManager().getBonusPercent();
                     player.sendMessage("");
                     player.sendMessage("§e⚡ §lKHUYẾN MÃI ĐANG DIỄN RA!");
                     player.sendMessage("§fNạp ngay để nhận thêm §6+" + (long)bonus + "% §fgiá trị.");
                 }
                 
                 player.sendMessage("§8----------------------------------");
                 player.sendMessage("§aNội dung chuyển khoản: §c" + content);
                 player.sendMessage("§7(Nhập đúng nội dung để được cộng tiền tự động)");
                 player.sendMessage("§8==================================");

                 generateQR(player, amount, content);
             } catch (NumberFormatException e) {
                 sendHelp(player);
             }
             return true;
         }
         
         String sub = args[0].toLowerCase();
         if (sub.equals("top")) {
             showTop(player);
             return true;
         }
         
         if (sub.equals("history")) {
             String target = (args.length > 1) ? args[1] : player.getName();
             if (!player.hasPermission("sepay.admin") && !target.equalsIgnoreCase(player.getName())) {
                  player.sendMessage("§cBạn chỉ có thể xem lịch sử của chính mình.");
                  return true;
             }
             showHistory(player, target);
             return true;
         }

         sendHelp(player);
         return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§e/nap <số tiền> §f- Tạo mã QR thanh toán");
        player.sendMessage("§e/nap top §f- Xem top donate");
        player.sendMessage("§e/nap history §f- Xem lịch sử nạp");
    }

    private void showTop(Player player) {
        SchedulerAdapter.getScheduler().runAsync(plugin, () -> {
            List<String> top = plugin.getDatabaseManager().getTopDonors(10);
            SchedulerAdapter.getScheduler().runEntity(player, plugin, () -> {
                player.sendMessage("§e§l--- TOP DONATE ---");
                for (String line : top) {
                    player.sendMessage(line);
                }
            });
        });
    }
    
    private void showHistory(Player player, String target) {
        SchedulerAdapter.getScheduler().runAsync(plugin, () -> {
            List<String> history = plugin.getDatabaseManager().getTransactionHistory(target, 10);
            SchedulerAdapter.getScheduler().runEntity(player, plugin, () -> {
                player.sendMessage("§e§l--- LỊCH SỬ NẠP: " + target + " ---");
                if (history.isEmpty()) {
                    player.sendMessage("§cChưa có giao dịch nào.");
                } else {
                    for (String line : history) {
                        player.sendMessage(line);
                    }
                }
            });
        });
    }

    public void generateQR(Player player, double amount, String content) {
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(plugin.getConfigManager().getMessage("inventory_full"));
            return;
        }

        long amountLong = (long) amount;
        ConfigManager cfg = plugin.getConfigManager();
        
        player.sendMessage(cfg.getMessage("generating_qr"));

        // Async URL Generation
        SchedulerAdapter.getScheduler().runAsync(plugin, () -> {
            try {
                String encodedContent = URLEncoder.encode(content, StandardCharsets.UTF_8.toString());
                String bank = cfg.getBankCode();
                String acc = cfg.getAccountNumber();
                String url = String.format("https://qr.sepay.vn/img?bank=%s&acc=%s&template=compact&amount=%d&des=%s",
                        bank, acc, amountLong, encodedContent);

                QRMapRenderer renderer = new QRMapRenderer(url);

                SchedulerAdapter.getScheduler().runEntity(player, plugin, () -> {
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
