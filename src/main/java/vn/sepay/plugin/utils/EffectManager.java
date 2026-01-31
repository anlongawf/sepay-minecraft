package vn.sepay.plugin.utils;

import vn.sepay.plugin.SepayPlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EffectManager {

    private final SepayPlugin plugin;

    public EffectManager(SepayPlugin plugin) {
        this.plugin = plugin;
    }

    public void playSuccessEffects(Player player, double amount, double gameMoney) {
        // Title
        if (plugin.getConfig().getBoolean("effects.title.enabled")) {
            String title = format(plugin.getConfig().getString("effects.title.title"), amount, gameMoney);
            String subtitle = format(plugin.getConfig().getString("effects.title.subtitle"), amount, gameMoney);
            int fadeIn = plugin.getConfig().getInt("effects.title.fade_in");
            int stay = plugin.getConfig().getInt("effects.title.stay");
            int fadeOut = plugin.getConfig().getInt("effects.title.fade_out");
            
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        }

        // Firework
        if (plugin.getConfig().getBoolean("effects.firework.enabled")) {
            spawnFirework(player.getLocation());
        }
    }

    private void spawnFirework(Location loc) {
        try {
            Firework fw = loc.getWorld().spawn(loc, Firework.class);
            FireworkMeta meta = fw.getFireworkMeta();
            
            String typeStr = plugin.getConfig().getString("effects.firework.type", "BALL_LARGE");
            FireworkEffect.Type type = FireworkEffect.Type.valueOf(typeStr);
            
            List<Color> colors = new ArrayList<>();
            for (String c : plugin.getConfig().getStringList("effects.firework.colors")) {
               colors.add(getColor(c));
            }
            if (colors.isEmpty()) colors.add(Color.RED);

            int power = plugin.getConfig().getInt("effects.firework.power", 1);
            
            FireworkEffect effect = FireworkEffect.builder()
                    .flicker(true)
                    .trail(true)
                    .with(type)
                    .withColor(colors)
                    .build();

            meta.addEffect(effect);
            meta.setPower(power);
            fw.setFireworkMeta(meta);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String format(String s, double amount, double gameMoney) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s
                .replace("%amount%", String.format("%,.0f", amount))
                .replace("%game_money%", String.format("%,.0f", gameMoney)));
    }

    private Color getColor(String name) {
        try {
            return (Color) Color.class.getField(name.toUpperCase()).get(null);
        } catch (Exception e) {
            return Color.RED;
        }
    }
}
