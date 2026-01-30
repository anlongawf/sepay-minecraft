package vn.sepay.plugin.utils;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import org.bukkit.map.MinecraftFont;

public class QRMapRenderer extends MapRenderer {

    private BufferedImage image;
    private boolean rendered = false;

    public QRMapRenderer(String url) {
        try {
            this.image = ImageIO.read(new URL(url));
            // Resize to 128x128 if needed, but MapCanvas handles drawing logic.
            // Ideally we resize here to save performance during render()
            this.image = resize(this.image, 128, 128);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public QRMapRenderer(BufferedImage image) {
        this.image = resize(image, 128, 128);
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (rendered) return; // Only render once to save performance
        
        if (image != null) {
            canvas.drawImage(0, 0, image);
            rendered = true;
        } else {
            canvas.drawText(10, 10, MinecraftFont.Font, "Error/Loading QR...");
        }
    }

    private BufferedImage resize(BufferedImage original, int width, int height) {
        java.awt.Image tmp = original.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return resized;
    }
}
