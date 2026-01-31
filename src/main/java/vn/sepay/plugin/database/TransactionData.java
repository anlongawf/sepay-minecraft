package vn.sepay.plugin.database;

public class TransactionData {
    private final String id;
    private final String playerName;
    private final double amount;
    private final String content;
    private final String status;

    public TransactionData(String id, String playerName, double amount, String content, String status) {
        this.id = id;
        this.playerName = playerName;
        this.amount = amount;
        this.content = content;
        this.status = status;
    }

    public String getId() { return id; }
    public String getPlayerName() { return playerName; }
    public double getAmount() { return amount; }
    public String getContent() { return content; }
    public String getStatus() { return status; }
}
