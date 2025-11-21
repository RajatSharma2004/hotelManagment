package com.hotel.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseService {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/hotel_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Rajat@6548";

    // static connection provider
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    public static void initDatabase() {
        // For MySQL, create tables manually in Workbench (or I can add SQL-runner)
        System.out.println("Using external MySQL. Ensure database 'hotel_db' and tables exist.");
    }

    public static boolean authenticateAdmin(String username, String password) {
        String query = "SELECT password FROM admin WHERE username = ?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String pw = rs.getString("password");
                    return pw != null && pw.equals(password);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}