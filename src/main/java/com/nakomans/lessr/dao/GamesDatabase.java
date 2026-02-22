package com.nakomans.lessr.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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
}
