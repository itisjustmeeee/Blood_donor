package ru.mirea.project.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseManager {
    static final String HOST = "jdbc:postgresql://" +
            setting("BLOOD_DB_HOST", "localhost") + ":" +
            setting("BLOOD_DB_PORT", "5432") + "/";
    static final String DATABASE = setting("BLOOD_DB_NAME", "blood_donor");
    private static final String URL = HOST + DATABASE;
    private static final String USER = setting("BLOOD_DB_USER", "postgres");
    private static final String PASSWORD = setting("BLOOD_DB_PASSWORD", "");

    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    static Connection getServerConnection() throws SQLException {
        return DriverManager.getConnection(HOST + "postgres", USER, PASSWORD);
    }

    private static String setting(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}

