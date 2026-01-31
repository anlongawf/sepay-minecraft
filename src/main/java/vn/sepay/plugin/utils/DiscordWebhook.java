package vn.sepay.plugin.utils;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhook {

    private final String url;

    public DiscordWebhook(String url) {
        this.url = url;
    }

    public void send(String content) {
        if (url == null || url.isEmpty()) return;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(content.getBytes(StandardCharsets.UTF_8));
            }

            conn.getInputStream().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Helper to build simple Embed JSON
    public static String buildEmbedJson(String title, String description, int color, String footer) {
        return "{"
                + "\"embeds\": [{"
                + "\"title\": \"" + escape(title) + "\","
                + "\"description\": \"" + escape(description) + "\","
                + "\"color\": " + color + ","
                + "\"footer\": { \"text\": \"" + escape(footer) + "\" }"
                + "}]"
                + "}";
    }
    
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", "\\n");
    }
}
