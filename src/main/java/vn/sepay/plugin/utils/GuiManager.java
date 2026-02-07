package vn.sepay.plugin.utils;

import vn.sepay.plugin.SepayPlugin;
import vn.sepay.plugin.database.TransactionData;
import vn.sepay.plugin.scheduler.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class GuiManager implements Listener {

    private final SepayPlugin plugin;
    private final String TITLE_MAIN = "§2Ngân Hàng Sepay";
    private final String TITLE_DEPOSIT = "§2Nạp Thẻ Ủng Hộ";
    private final String TITLE_TOP = "§6Bảng Xếp Hạng";
    private final String TITLE_HISTORY = "§bLịch Sử Giao Dịch";

    public GuiManager(SepayPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_MAIN);

        // Decor
        ItemStack bg = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, "§0");
        for (int i=0; i<27; i++) inv.setItem(i, bg);

        // Items
        inv.setItem(11, createGuiItem(Material.DIAMOND, "§a§lNẠP TIỀN", 
            "§7Nạp thẻ ủng hộ Server", "§7Nhận Point và Quà tặng", "", "§e▶ Nhấn để mở"));
            
        inv.setItem(13, createGuiItem(Material.GOLD_BLOCK, "§6§lTOP ĐẠI GIA", 
            "§7Xem bảng xếp hạng", "§7Top 10 người giàu nhất", "", "§e▶ Nhấn để xem"));
            
        inv.setItem(15, createGuiItem(Material.BOOK, "§b§lLỊCH SỬ", 
            "§7Xem lịch sử nạp thẻ", "§7Các giao dịch gần đây", "", "§e▶ Nhấn để xem"));

        player.openInventory(inv);
    }

    public void openDepositMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_DEPOSIT);

        inv.setItem(10, createGuiItem(Material.EMERALD, "§a10,000 VNĐ", "§7Nhận: §e10 Points"));
        inv.setItem(11, createGuiItem(Material.EMERALD, "§a20,000 VNĐ", "§7Nhận: §e20 Points"));
        inv.setItem(12, createGuiItem(Material.EMERALD, "§a50,000 VNĐ", "§7Nhận: §e50 Points"));
        inv.setItem(13, createGuiItem(Material.DIAMOND, "§b100,000 VNĐ", "§7Nhận: §e100 Points"));
        inv.setItem(14, createGuiItem(Material.DIAMOND, "§b200,000 VNĐ", "§7Nhận: §e200 Points"));
        inv.setItem(15, createGuiItem(Material.NETHER_STAR, "§6500,000 VNĐ", "§7Nhận: §e500 Points"));

        // Promotion Info
        if (plugin.getPromotionManager().isPromotionActive()) {
            double bonus = plugin.getPromotionManager().getBonusPercent();
            String title = plugin.getConfig().getString("promotion.messages.gui_title", "&e⚡ ĐANG KHUYẾN MÃI");
            List<String> rawLore = plugin.getConfig().getStringList("promotion.messages.gui_lore");
            
            if (rawLore.isEmpty()) {
                rawLore = Arrays.asList("&7Hệ thống đang tặng thêm &6%bonus%%", "&7cho mọi giao dịch!");
            }
            
            List<String> lore = new ArrayList<>();
            for (String line : rawLore) {
                lore.add(line.replace("%bonus%", String.valueOf((long)bonus)));
            }
            
            // Convert List<String> to String... for createGuiItem helper
            // Helper method accepts String... lore (Varargs)
            // We need to change createGuiItem to accept List or convert.
            // Let's create a temp array.
            inv.setItem(26, createGuiItem(Material.GOLD_INGOT, title, lore.toArray(new String[0])));
        }

        // Other Items
        inv.setItem(22, createGuiItem(Material.NAME_TAG, "§eNạp Số Khác", "§7Nhập số tiền tùy ý bạn muốn"));
        inv.setItem(18, createGuiItem(Material.ARROW, "§cQuay lại"));

        player.openInventory(inv);
    }

    public void openTopMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_TOP);
        
        // Loading...
        inv.setItem(13, createGuiItem(Material.CLOCK, "§7Đang tải..."));
        inv.setItem(18, createGuiItem(Material.ARROW, "§cQuay lại"));
        player.openInventory(inv);

        SchedulerAdapter.getScheduler().runAsync(plugin, () -> {
            // Get cached top donors from DatabaseManager (Assuming fetch is fast or cached)
            // But we already have getCachedTopDonors which is instant.
            List<TransactionData> topList = plugin.getDatabaseManager().getCachedTopDonors();
            
            SchedulerAdapter.getScheduler().runEntity(player, plugin, () -> {
                 // Check if player still viewing same inventory?
                 // Simple hack: check title
                 if (!player.getOpenInventory().getTitle().equals(TITLE_TOP)) return;
                 
                 Inventory current = player.getOpenInventory().getTopInventory();
                 current.setItem(13, null); // Clear loading

                 int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21}; // 10 slots
                 for (int i = 0; i < topList.size() && i < slots.length; i++) {
                     TransactionData t = topList.get(i);
                     current.setItem(slots[i], createGuiItem(Material.PLAYER_HEAD, 
                         "§6TOP " + (i+1) + ": " + t.getPlayerName(),
                         "§7Tổng nạp: §e" + String.format("%,.0f VNĐ", t.getAmount())
                     ));
                 }
            });
        });
    }

    public void openHistoryMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_HISTORY);
        inv.setItem(13, createGuiItem(Material.CLOCK, "§7Đang tải..."));
        inv.setItem(18, createGuiItem(Material.ARROW, "§cQuay lại"));
        player.openInventory(inv);
        
        SchedulerAdapter.getScheduler().runAsync(plugin, () -> {
            List<String> history = plugin.getDatabaseManager().getTransactionHistory(player.getName(), 10);
            
            SchedulerAdapter.getScheduler().runEntity(player, plugin, () -> {
                 if (!player.getOpenInventory().getTitle().equals(TITLE_HISTORY)) return;
                 Inventory current = player.getOpenInventory().getTopInventory();
                 current.setItem(13, null);
                 
                 int slot = 0;
                 for (String line : history) {
                     if (slot >= 18) break; // Limit
                     // line format: [Timestamp] Amount - Status
                     current.setItem(slot, createGuiItem(Material.PAPER, 
                         "§eGiao dịch #" + (slot+1),
                         "§f" + line
                     ));
                     slot++;
                 }
                 
                 if (history.isEmpty()) {
                     current.setItem(13, createGuiItem(Material.BARRIER, "§cChưa có giao dịch nào"));
                 }
            });
        });
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (!title.equals(TITLE_MAIN) && !title.equals(TITLE_DEPOSIT) 
            && !title.equals(TITLE_TOP) && !title.equals(TITLE_HISTORY)) return;
        
        e.setCancelled(true);
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

        Player player = (Player) e.getWhoClicked();
        ItemStack clickedItem = e.getCurrentItem();
        String name = clickedItem.getItemMeta().getDisplayName();

        if (name.equals("§cQuay lại")) {
            openMainMenu(player);
            return;
        }

        // MAIN MENU HANDLING
        if (title.equals(TITLE_MAIN)) {
            if (name.contains("NẠP TIỀN")) openDepositMenu(player);
            else if (name.contains("TOP ĐẠI GIA")) openTopMenu(player);
            else if (name.contains("LỊCH SỬ")) openHistoryMenu(player);
            return;
        }

        // DEPOSIT MENU HANDLING
        if (title.equals(TITLE_DEPOSIT)) {
            if (name.equals("§eNạp Số Khác")) {
                player.closeInventory();
                player.sendMessage("§aVui lòng nhập lệnh: §e/nap <số tiền>"); // Or /bank <amount>
                return;
            }
            
            try {
                String raw = name.replace("§a", "").replace("§b", "").replace("§6", "")
                                 .replace(" VNĐ", "").replace(",", "");
                if (clickedItem.getType() == Material.GOLD_INGOT) return;
                long amount = Long.parseLong(raw);
                player.closeInventory();
                
                // Trigger QR Generation (Legacy BankCommand logic)
                // We will rely on BankCommand to handle arg "amount"
                player.performCommand("bank " + amount);
                
            } catch (NumberFormatException ex) {}
        }
    }
}
