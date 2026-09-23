package ru.mirea.project.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
    private static final String URL = "jdbc:postgresql://localhost:5432/blood_donor";
    private static final String USER = "postgres";
    private static final String PASSWORD = "Alice!?Kan";

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
