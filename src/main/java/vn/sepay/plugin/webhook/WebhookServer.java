package vn.sepay.plugin.webhook;

import vn.sepay.plugin.SepayPlugin;
import fi.iki.elonen.NanoHTTPD;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebhookServer extends NanoHTTPD {

    private final SepayPlugin plugin;
    private final Set<String> processedTransactions = new HashSet<>();
    private final File logFile;

    public WebhookServer(SepayPlugin plugin, int port) {
        super(port);
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "processed_transactions.txt");
        loadProcessedTransactions();
    }

    private void loadProcessedTransactions() {
        if (!logFile.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                processedTransactions.add(line.trim());
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load processed transactions.");
        }
    }

    private void logTransaction(String id) {
        processedTransactions.add(id);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(id);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        plugin.getLogger().info("[Sepay] Received request: " + session.getMethod() + " " + session.getUri());
        
        if (!session.getMethod().equals(Method.POST)) {
            plugin.getLogger().info("[Sepay] Rejected: Method not POST");
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Only POST allowed.");
        }

        String apiKey = plugin.getConfigManager().getApiKey();
        String authHeader = session.getHeaders().get("authorization");
        
        if (apiKey != null && !apiKey.isEmpty()) {
            boolean authorized = false;
            // Lenient check: header usually "Bearer <token>"
            if (authHeader != null && authHeader.contains(apiKey)) authorized = true;
            
            if (!authorized) {
                 plugin.getLogger().warning("[Sepay] Authentication Failed. Header: " + authHeader);
                 return newFixedLengthResponse(Response.Status.UNAUTHORIZED, MIME_PLAINTEXT, "Invalid API Key");
            }
        }

        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String jsonBody = files.get("postData");
            
            plugin.getLogger().info("[Sepay] Body: " + jsonBody);

            if (jsonBody == null) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "No Body");
            }

            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(jsonBody);

            String transactionId = String.valueOf(json.get("id")); 
            if (transactionId == null || transactionId.equals("null") || transactionId.isEmpty()) {
                 transactionId = String.valueOf(json.get("referenceCode"));
            }

            if (processedTransactions.contains(transactionId)) {
                 plugin.getLogger().info("[Sepay] Duplicate transaction ignored: " + transactionId);
                 return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "Already Processed");
            }

            String content = (String) json.get("content");
            Object amtObj = json.get("transferAmount");
            double amount = 0;
            if (amtObj instanceof Double) amount = (Double) amtObj;
            else if (amtObj instanceof Long) amount = ((Long) amtObj).doubleValue();
            else if (amtObj != null) amount = Double.parseDouble(amtObj.toString());
            
            plugin.getLogger().info("[Sepay] Processing: ID=" + transactionId + ", Amount=" + amount + ", Content=" + content);
            processPayment(transactionId, content, amount);
            
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "Success");

        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error: " + e.getMessage());
        }
    }

    private void processPayment(String txnId, String content, double amount) {
        if (content == null) return;
        
        String prefix = plugin.getConfigManager().getContentPrefix().trim();
        plugin.getLogger().info("[Sepay] Checking content: '" + content + "' against prefix '" + prefix + "'");
        
        Pattern pattern = Pattern.compile(Pattern.quote(prefix) + "\\s*([A-Za-z0-9_]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            String playerName = matcher.group(1);
            plugin.getLogger().info("[Sepay] Match found: Player=" + playerName);
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
                // Basic validation: Check if player has played before if we want to be strict
                // if (player.hasPlayedBefore() || player.isOnline()) ...
                // For now, allow any valid username format.
                
                double rate = plugin.getConfigManager().getExchangeRate();
                double gameMoney = amount * rate;
                
                for (String cmd : plugin.getConfigManager().getSuccessCommands()) {
                    String run = cmd.replace("%player%", playerName)
                                    .replace("%amount%", String.valueOf((long)amount))
                                    .replace("%game_money%", String.valueOf((long)gameMoney)); // Cast to long to remove .0
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), run);
                }
                
                logTransaction(txnId);
                plugin.getLogger().info("Processed donation for " + playerName + ": " + amount);
            });
        }
    }
}
