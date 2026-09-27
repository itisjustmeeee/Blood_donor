package ru.mirea.project.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseManager {
    static final String HOST = "jdbc:postgresql://localhost:5432/";
    static final String DATABASE = "blood_donor";
    private static final String URL = "jdbc:postgresql://localhost:5432/blood_donor";
    private static final String USER = "postgres";
    private static final String PASSWORD = "1";

    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    static Connection getServerConnection() throws SQLException {
        return DriverManager.getConnection(HOST + "postgres", USER, PASSWORD);
    }
}


