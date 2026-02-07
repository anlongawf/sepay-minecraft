package vn.sepay.plugin.command;

import vn.sepay.plugin.SepayPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BankCommand implements CommandExecutor {

    private final SepayPlugin plugin;

    public BankCommand(SepayPlugin plugin) {
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
            plugin.getGuiManager().openMainMenu(player);
            return true;
        }
        
        // Handle /bank <amount>
        try {
             double amount = Double.parseDouble(args[0]);
             if (amount < 1000) throw new NumberFormatException();

             vn.sepay.plugin.config.ConfigManager cfg = plugin.getConfigManager();
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
             player.sendMessage("§cVui lòng nhập số tiền hợp lệ (Ví dụ: /bank 20000)");
             plugin.getGuiManager().openMainMenu(player);
        }

        return true;
    }

    public void generateQR(Player player, double amount, String content) {
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(plugin.getConfigManager().getMessage("inventory_full"));
            return;
        }

        long amountLong = (long) amount;
        vn.sepay.plugin.config.ConfigManager cfg = plugin.getConfigManager();
        
        player.sendMessage(cfg.getMessage("generating_qr"));

        // Async URL Generation
        vn.sepay.plugin.scheduler.SchedulerAdapter.getScheduler().runAsync(plugin, () -> {
            try {
                String encodedContent = java.net.URLEncoder.encode(content, java.nio.charset.StandardCharsets.UTF_8.toString());
                String bank = cfg.getBankCode();
                String acc = cfg.getAccountNumber();
                String url = String.format("https://qr.sepay.vn/img?bank=%s&acc=%s&template=compact&amount=%d&des=%s",
                        bank, acc, amountLong, encodedContent);

                vn.sepay.plugin.utils.QRMapRenderer renderer = new vn.sepay.plugin.utils.QRMapRenderer(url);

                vn.sepay.plugin.scheduler.SchedulerAdapter.getScheduler().runEntity(player, plugin, () -> {
                    org.bukkit.inventory.ItemStack mapItem = new org.bukkit.inventory.ItemStack(org.bukkit.Material.FILLED_MAP);
                    org.bukkit.inventory.meta.MapMeta meta = (org.bukkit.inventory.meta.MapMeta) mapItem.getItemMeta();
                    org.bukkit.map.MapView view = org.bukkit.Bukkit.createMap(player.getWorld());
                    
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
