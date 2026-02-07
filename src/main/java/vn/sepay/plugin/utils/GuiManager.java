package vn.sepay.plugin.utils;

import vn.sepay.plugin.SepayPlugin;
import vn.sepay.plugin.command.NapCommand;
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

public class GuiManager implements Listener {

    private final SepayPlugin plugin;
    private final String GUI_TITLE = "§2Nạp Thẻ Ủng Hộ Server";

    public GuiManager(SepayPlugin plugin) {
        this.plugin = plugin;
    }

    public void openGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);

        // Fixed Amounts
        inv.setItem(10, createGuiItem(Material.EMERALD, "§a10,000 VNĐ", "§7Nhận: §e10 Points"));
        inv.setItem(11, createGuiItem(Material.EMERALD, "§a20,000 VNĐ", "§7Nhận: §e20 Points"));
        inv.setItem(12, createGuiItem(Material.EMERALD, "§a50,000 VNĐ", "§7Nhận: §e50 Points"));
        inv.setItem(13, createGuiItem(Material.DIAMOND, "§b100,000 VNĐ", "§7Nhận: §e100 Points"));
        inv.setItem(14, createGuiItem(Material.DIAMOND, "§b200,000 VNĐ", "§7Nhận: §e200 Points"));
        inv.setItem(15, createGuiItem(Material.NETHER_STAR, "§6500,000 VNĐ", "§7Nhận: §e500 Points"));

        // Promotion Info
        if (plugin.getPromotionManager().isPromotionActive()) {
            double bonus = plugin.getPromotionManager().getBonusPercent();
            inv.setItem(26, createGuiItem(Material.GOLD_INGOT, "§e⚡ ĐANG KHUYẾN MÃI", 
                "§7Hệ thống đang tặng thêm §6" + (long)bonus + "%",
                "§7cho mọi giao dịch!"));
        }

        // Custom Amount
        inv.setItem(22, createGuiItem(Material.NAME_TAG, "§eNạp Số Khác", "§7Nhập số tiền tùy ý bạn muốn"));

        player.openInventory(inv);
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
        if (!e.getView().getTitle().equals(GUI_TITLE)) return;
        
        e.setCancelled(true);
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

        Player player = (Player) e.getWhoClicked();
        ItemStack clickedItem = e.getCurrentItem();
        String name = clickedItem.getItemMeta().getDisplayName();

        if (name.equals("§eNạp Số Khác")) {
            player.closeInventory();
            player.sendMessage("§aVui lòng nhập lệnh: §e/nap <số tiền>");
            return;
        }

        // Parse amount from Name
        try {
            // Strip colors and " VNĐ"
            String raw = name.replace("§a", "").replace("§b", "").replace("§6", "")
                             .replace(" VNĐ", "").replace(",", "");
            
            // Check if it's a number (Handling Promotion Item case)
            if (clickedItem.getType() == Material.GOLD_INGOT) return;

            long amount = Long.parseLong(raw);
            player.closeInventory();
            
            // Call NapCommand logic directly
            // We need to access NapCommand instance or extract logic.
            // Better: Dispatch command to reuse logic
            player.chat("/nap " + amount);
            
        } catch (NumberFormatException ex) {
            // Ignore valid clicks on non-amount items
        }
    }
}
