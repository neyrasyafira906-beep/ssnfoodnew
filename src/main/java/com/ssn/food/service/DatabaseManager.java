package com.ssn.food.service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.ssn.food.model.*;

public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/ssn_foodapp?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASS = "";
    
    private static DatabaseManager instance;
    private Connection conn;

    public static DatabaseManager get() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    private DatabaseManager() {
        try {
            // Check if driver exists
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("✅ DATABASE SUCCESS: Connected to ssn_foodapp!");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ DB ERROR: MySQL Driver NOT FOUND!");
            System.err.println("   Make sure 'lib/mysql-connector-j-9.7.0.jar' exists.");
        } catch (SQLException e) {
            System.err.println("❌ DB ERROR: Connection Failed!");
            System.err.println("   URL: " + URL);
            System.err.println("   Reason: " + e.getMessage());
            System.err.println("   [HINT] 1. Pastikan XAMPP (MySQL) sudah di-START.");
            System.err.println("   [HINT] 2. Pastikan database 'ssn_foodapp' sudah dibuat di phpMyAdmin.");
            System.err.println("   [HINT] 3. Cek apakah port MySQL di XAMPP itu 3306 atau 3307.");
        } catch (Exception e) {
            System.err.println("❌ DB ERROR: Unexpected issue: " + e.getMessage());
        }
    }

    public boolean isConnected() { return conn != null; }

    // --- SELLERS ---
    public List<Seller> loadSellers() {
        List<Seller> list = new ArrayList<>();
        if (!isConnected()) return list;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM sellers")) {
            while (rs.next()) {
                Seller s = new Seller(rs.getString("name"), rs.getString("phone"), 
                                    rs.getString("address"), rs.getDouble("lat"), rs.getDouble("lng"));
                s.setId(rs.getString("id"));
                s.setVideoUrl(rs.getString("video_url"));
                s.setRating(rs.getDouble("rating"));
                list.add(s);
            }
        } catch (SQLException e) { e.printStackTrace(); }

        for (Seller s : list) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT AVG(rating) as avg_r, COUNT(*) as cnt_r FROM reviews WHERE seller_id=?")) {
                ps.setString(1, s.getId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    double avg = rs.getDouble("avg_r");
                    int count = rs.getInt("cnt_r");
                    if (count > 0) {
                        s.setRating(avg);
                        s.setRatingCount(count);
                    }
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }
        return list;
    }

    public void saveSeller(Seller s) {
        if (!isConnected()) return;
        String sql = "REPLACE INTO sellers (id, name, phone, address, lat, lng, video_url, rating) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getId());
            ps.setString(2, s.getName());
            ps.setString(3, s.getPhone());
            ps.setString(4, s.getAddress());
            ps.setDouble(5, s.getLat());
            ps.setDouble(6, s.getLng());
            ps.setString(7, s.getVideoUrl());
            ps.setDouble(8, s.getRating());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- MENU ITEMS ---
    public List<FoodItem> loadMenu(String sellerId) {
        List<FoodItem> list = new ArrayList<>();
        if (!isConnected()) return list;
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM menu_items WHERE seller_id=?")) {
            ps.setString(1, sellerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                FoodItem item = new FoodItem(rs.getString("id"), rs.getString("name"), rs.getLong("price"), rs.getInt("stock"), rs.getString("emoji"));
                list.add(item);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void saveMenuItem(String sellerId, FoodItem item) {
        if (!isConnected()) return;
        String sql = "REPLACE INTO menu_items (id, seller_id, name, price, stock, emoji) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getId());
            ps.setString(2, sellerId);
            ps.setString(3, item.getName());
            ps.setLong(4, item.getPrice());
            ps.setInt(5, item.getStock());
            ps.setString(6, item.getEmoji());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- CHATS ---
    public List<ChatMsg> loadChats(String contextId) {
        List<ChatMsg> list = new ArrayList<>();
        if (!isConnected()) return list;
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM chat_messages WHERE context_id=? ORDER BY sent_at ASC")) {
            ps.setString(1, contextId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ChatMsg.From from = ChatMsg.From.valueOf(rs.getString("sender_role"));
                ChatMsg m = new ChatMsg(from, rs.getString("message_text"));
                m.sellerId = contextId;
                list.add(m);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public void saveChat(String contextId, ChatMsg m) {
        if (!isConnected()) return;
        String sql = "INSERT INTO chat_messages (context_id, sender_role, message_text) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, contextId);
            ps.setString(2, m.from.name());
            ps.setString(3, m.text);
            ps.executeUpdate();
            System.out.println("💾 Chat Saved to DB: " + m.text);
        } catch (SQLException e) { 
            System.err.println("❌ Failed to save chat: " + e.getMessage());
        }
    }

    // --- ORDERS ---
    public void saveOrder(Order o) {
        if (!isConnected()) return;
        String sql = "INSERT INTO orders (id, buyer_name, buyer_phone, buyer_address, seller_id, total_price, payment_method, status, timestamp) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, o.getId());
            ps.setString(2, o.getBuyerName());
            ps.setString(3, o.getBuyerPhone());
            ps.setString(4, o.getBuyerAddress());
            ps.setString(5, o.getSellerId());
            ps.setLong(6, o.getTotal());
            ps.setString(7, o.getPayment().name());
            ps.setString(8, o.getStatus().name());
            ps.setString(9, o.getTimestamp());
            ps.executeUpdate();
            System.out.println("📦 Order Saved to DB: " + o.getId());
        } catch (SQLException e) { 
            System.err.println("❌ Failed to save order: " + e.getMessage());
        }
    }

    // --- REVIEWS ---
    public List<Review> loadReviews(String sellerId) {
        List<Review> list = new ArrayList<>();
        if (!isConnected()) return list;
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM reviews WHERE seller_id=? ORDER BY timestamp DESC")) {
            ps.setString(1, sellerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Review r = new Review(rs.getString("seller_id"), rs.getString("buyer_name"), rs.getInt("rating"), rs.getString("comment"));
                r.setId(rs.getString("id"));
                list.add(r);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void saveReview(Review r) {
        if (!isConnected()) return;
        String sql = "INSERT INTO reviews (id, seller_id, buyer_name, rating, comment, timestamp) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getId());
            ps.setString(2, r.getSellerId());
            ps.setString(3, r.getBuyerName());
            ps.setInt(4, r.getRating());
            ps.setString(5, r.getComment());
            ps.setString(6, r.getTimestamp());
            ps.executeUpdate();
            System.out.println("⭐ Review Saved to DB: " + r.getId());
        } catch (SQLException e) { 
            System.err.println("❌ Failed to save review: " + e.getMessage());
        }
    }
}
