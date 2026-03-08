package com.nakomans.lessr.dao;

import com.nakomans.lessr.dto.WishlistItem;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class WishlistDatabase {

    private static final String URL = "jdbc:sqlite:src/data/user_data.db";

    @PostConstruct
    public void init() {
        String sql = """
            CREATE TABLE IF NOT EXISTS wishlist (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id TEXT NOT NULL,
                game_name TEXT NOT NULL
            );
        """;
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("✅ Banco da Wishlist (Modo Rápido) pronto!");
        } catch (SQLException e) {
            System.out.println("❌ Deu ruim: " + e.getMessage());
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public boolean addGame(String userId, String gameName) {
        String countSql = "SELECT COUNT(*) FROM wishlist WHERE user_id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(countSql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getInt(1) >= 5) return false;
        } catch (SQLException e) { return false; }

        String checkSql = "SELECT id FROM wishlist WHERE user_id = ? AND LOWER(game_name) = LOWER(?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, gameName);
            if (pstmt.executeQuery().next()) return false;
        } catch (SQLException e) { return false; }

        String insertSql = "INSERT INTO wishlist(user_id, game_name) VALUES(?,?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, gameName);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) { return false; }
    }

    public List<WishlistItem> getUserWishlist(String userId) {
        List<WishlistItem> list = new ArrayList<>();
        String sql = "SELECT game_name FROM wishlist WHERE user_id = ?";
        
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new WishlistItem(rs.getString("game_name")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean removeGame(String userId, String gameName) {
        String sql = "DELETE FROM wishlist WHERE user_id = ? AND LOWER(game_name) = LOWER(?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, gameName);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) { return false; }
    }
}