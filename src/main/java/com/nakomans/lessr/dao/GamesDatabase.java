package com.nakomans.lessr.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.AbstractMap;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GamesDatabase {

    @Value("${spring.datasource.url}")
    private String url;

    private final List<String> allGamesCache = new ArrayList<>();

    @PostConstruct
    public void loadGamesCache() {
        String sql = "SELECT name_clean FROM games";
        try (Connection conn = connectToDatabase();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                allGamesCache.add(rs.getString(1));
            }
        } catch (SQLException e) {
        }
    }

    public Connection connectToDatabase() {
        try {
            return DriverManager.getConnection(this.url);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Optional<String> getGameAppId(String rawGameName) {
        String sql = "SELECT appid FROM games WHERE name_clean = ?";
        String gameName = rawGameName.replaceAll("[^a-zA-Z0-9 ]","").trim().toLowerCase();

        try (Connection conn = connectToDatabase();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
            pstmt.setString(1, gameName);

            try (ResultSet rs = pstmt.executeQuery()) {
                if(rs.next()) {
                    return Optional.of(String.valueOf(rs.getInt("appid")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } 
        return Optional.empty();
    }

    public List<String> suggestSimilarGames(String rawGameName) {
        String gameName = rawGameName.replaceAll("[^a-zA-Z0-9 ]", "").trim().toLowerCase();
        
        if (allGamesCache.isEmpty()) {
            return new ArrayList<>();
        }

        return allGamesCache.stream()
            .map(game -> {
                int score;
                if (game.equals(gameName)) {
                    score = 0;
                } else if (game.startsWith(gameName)) {
                    score = 1;
                } else if (game.contains(gameName)) {
                    score = 2;
                } else {
                    score = calculateLevenshtein(gameName, game) + 3;
                }
                return new AbstractMap.SimpleEntry<>(game, score);
            })
            .filter(entry -> entry.getValue() <= gameName.length() + 3)
            .sorted(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .distinct() 
            .limit(15)
            .collect(Collectors.toList());
    }

    private int calculateLevenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[a.length()][b.length()];
    }
}