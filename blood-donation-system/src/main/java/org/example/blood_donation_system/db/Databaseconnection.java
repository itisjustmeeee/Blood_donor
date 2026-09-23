package org.example.blood_donation_system.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public class DatabaseConnection {
    private static final String URL = "jdbc:postgresql://localhost:5432/PKS_Database";
    private static final String USER = "postgres";
    private static final String PASSWORD = "Hq93Lpsf5Vc";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
