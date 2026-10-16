package ru.mirea.project.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DatabaseManager {
    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/blood_donor";
    private static final String DEFAULT_USER = "postgres";
    private static final String DEFAULT_PASSWORD = "postgres";
    private static final Pattern URL_PATTERN = Pattern.compile(
            "jdbc:postgresql://([^/:]+)(?::(\\d+))?/(.+?)(?:\\?.*)?$");

    private static final Properties SETTINGS = loadProperties();
    static final String HOST = hostFromUrl(lookup("db.url", DEFAULT_URL));
    static final String DATABASE = databaseFromUrl(lookup("db.url", DEFAULT_URL));
    private static final String URL = lookup("db.url", DEFAULT_URL);
    private static final String USER = lookup("db.user", DEFAULT_USER);
    private static final String PASSWORD = lookup("db.password", DEFAULT_PASSWORD);

    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    static Connection getServerConnection() throws SQLException {
        return DriverManager.getConnection(HOST + "postgres", USER, PASSWORD);
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream stream = DatabaseManager.class.getClassLoader()
                .getResourceAsStream("database.properties")) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось прочитать database.properties", exception);
        }
        return properties;
    }

    private static String lookup(String key, String defaultValue) {
        String envValue = System.getenv(key.replace('.', '_').toUpperCase().replace('-', '_'));
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        if (SETTINGS.containsKey(key)) {
            String value = SETTINGS.getProperty(key).trim();
            if (!value.isBlank()) {
                return value;
            }
        }
        return defaultValue;
    }

    private static String hostFromUrl(String url) {
        Matcher matcher = URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            return "jdbc:postgresql://localhost:5432/";
        }
        String host = matcher.group(1);
        String port = matcher.group(2) == null ? "5432" : matcher.group(2);
        return "jdbc:postgresql://" + host + ":" + port + "/";
    }

    private static String databaseFromUrl(String url) {
        Matcher matcher = URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            return "blood_donor";
        }
        return matcher.group(3);
    }
}
