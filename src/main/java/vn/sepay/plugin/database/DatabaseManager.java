package vn.sepay.plugin.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import vn.sepay.plugin.SepayPlugin;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private final SepayPlugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(SepayPlugin plugin) {
        this.plugin = plugin;
        connect();
        initTables();
    }

    private void connect() {
        String type = plugin.getConfig().getString("database.type", "sqlite");
        HikariConfig config = new HikariConfig();

        if (type.equalsIgnoreCase("mysql")) {
            String host = plugin.getConfig().getString("database.host");
            int port = plugin.getConfig().getInt("database.port");
            String database = plugin.getConfig().getString("database.database");
            String username = plugin.getConfig().getString("database.username");
            String password = plugin.getConfig().getString("database.password");

            String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        } else {
            File file = new File(plugin.getDataFolder(), "database.db");
            config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
        }

        config.setPoolName("SepayPool");
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(5000);

        dataSource = new HikariDataSource(config);
        plugin.getLogger().info("Database connected (" + type + ")");
    }

    private void initTables() {
        String transactionTable = "CREATE TABLE IF NOT EXISTS sepay_transactions (" +
                "id VARCHAR(50) PRIMARY KEY," +
                "player_name VARCHAR(16) NOT NULL," +
                "amount DOUBLE NOT NULL," +
                "content TEXT," +
                "status VARCHAR(20) NOT NULL," + // SUCCESS, PENDING
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";
        
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(transactionTable);
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Check if transaction exists
    public boolean isTransactionProcessed(String id) {
        String sql = "SELECT id FROM sepay_transactions WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Save transaction
    public void saveTransaction(String id, String player, double amount, String content, String status) {
        String sql = "INSERT INTO sepay_transactions (id, player_name, amount, content, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, player);
            ps.setDouble(3, amount);
            ps.setString(4, content);
            ps.setString(5, status);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Update status
    public void updateStatus(String id, String status) {
        String sql = "UPDATE sepay_transactions SET status = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public List<TransactionData> getPendingTransactions(String playerName) {
        List<TransactionData> list = new ArrayList<>();
        String sql = "SELECT * FROM sepay_transactions WHERE status = 'PENDING' AND player_name = ?";
        
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TransactionData(
                        rs.getString("id"),
                        rs.getString("player_name"),
                        rs.getDouble("amount"),
                        rs.getString("content"),
                        rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<String> getTopDonors(int limit) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT player_name, SUM(amount) as total FROM sepay_transactions WHERE status = 'SUCCESS' GROUP BY player_name ORDER BY total DESC LIMIT ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    list.add(String.format("#%d. %s - %,.0f VNĐ", 
                        rank++, rs.getString("player_name"), rs.getDouble("total")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public List<String> getTransactionHistory(String playerName, int limit) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT * FROM sepay_transactions WHERE player_name = ? ORDER BY created_at DESC LIMIT ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerName);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(String.format("[%s] %,.0f VNĐ - %s", 
                        rs.getString("created_at"), rs.getDouble("amount"), rs.getString("status")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private final List<TransactionData> cachedTopDonors = new ArrayList<>();

    public List<TransactionData> getCachedTopDonors() {
        return cachedTopDonors;
    }

    public void updateTopDonorsCache() {
        String sql = "SELECT player_name, SUM(amount) as total FROM sepay_transactions WHERE status = 'SUCCESS' GROUP BY player_name ORDER BY total DESC LIMIT 10";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                List<TransactionData> newList = new ArrayList<>();
                while (rs.next()) {
                    // Reusing TransactionData to store name and total amount
                    // ID = null, content = null, status = SUCCESS
                    newList.add(new TransactionData(
                        null,
                        rs.getString("player_name"),
                        rs.getDouble("total"),
                        null,
                        "SUCCESS"
                    ));
                }
                synchronized (cachedTopDonors) {
                    cachedTopDonors.clear();
                    cachedTopDonors.addAll(newList);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
