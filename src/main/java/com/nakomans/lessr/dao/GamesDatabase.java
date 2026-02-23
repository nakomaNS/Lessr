package com.nakomans.lessr.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GamesDatabase {

    @Value("${spring.datasource.url}")
    private String url;

    public Connection connectToDatabase() {
        try {
            return DriverManager.getConnection(this.url);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Optional<String> getGameAppId(String rawGameName) {
        String sql = "SELECT appid FROM games WHERE name_clean LIKE ?";
        String gameName = rawGameName.replaceAll("[^a-zA-Z0-9 ]","").trim().toLowerCase();
        String searchPattern = "%" + gameName + "%";

        try (Connection conn = connectToDatabase();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, searchPattern);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if(rs.next()) {
                        return Optional.of(String.valueOf(rs.getInt("appid")));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } return Optional.empty();
        } 


}
