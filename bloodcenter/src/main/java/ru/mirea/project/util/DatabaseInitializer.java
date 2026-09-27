package ru.mirea.project.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

public final class DatabaseInitializer {
    private DatabaseInitializer() {
    }

    public static void initialize() throws SQLException {
        ensureDatabase();
        try (Connection connection = DatabaseManager.getConnection()) {
            if (!schemaExists(connection)) {
                createSchema(connection);
            }
            migrateDonorAge(connection);
            migrateDonorPhone(connection);
        }
    }

    private static void ensureDatabase() throws SQLException {
        try (Connection connection = DatabaseManager.getServerConnection();
                var check = connection.prepareStatement(
                        "SELECT 1 FROM pg_database WHERE datname=?")) {
            check.setString(1, DatabaseManager.DATABASE);
            try (var rows = check.executeQuery()) {
                if (rows.next()) {
                    return;
                }
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE DATABASE " + DatabaseManager.DATABASE);
            }
        }
    }

    private static boolean schemaExists(Connection connection) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT to_regclass('public.donor') IS NOT NULL");
                var rows = statement.executeQuery()) {
            rows.next();
            return rows.getBoolean(1);
        }
    }

    private static void createSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String sql : splitStatements(readSchemaScript())) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                }
            }
        }
    }

    private static String readSchemaScript() throws SQLException {
        Path[] candidates = {
                Path.of("sql_script", "blood_donor.sql"),
                Path.of("..", "sql_script", "blood_donor.sql")
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                try {
                    return Files.readString(candidate, StandardCharsets.UTF_8);
                } catch (IOException exception) {
                    throw new SQLException("Не удалось прочитать SQL-схему: " + candidate, exception);
                }
            }
        }
        throw new SQLException(
                "Не найден файл sql_script/blood_donor.sql. Запустите приложение из корня проекта или каталога bloodcenter.");
    }

    private static String[] splitStatements(String script) {
        String withoutComments = Pattern.compile("(?s)/\\*.*?\\*/").matcher(script).replaceAll("");
        withoutComments = withoutComments.replaceAll("(?m)--.*$", "");
        return withoutComments.split(";");
    }

    private static void migrateDonorAge(Connection connection) throws SQLException {
        boolean hasAge = hasColumn(connection, "age");
        boolean hasBirthDate = hasColumn(connection, "birth_date");
        try (Statement statement = connection.createStatement()) {
            if (!hasAge) {
                statement.execute("ALTER TABLE donor ADD COLUMN age INT");
                if (hasBirthDate) {
                    statement.execute("""
                            UPDATE donor
                            SET age = EXTRACT(YEAR FROM age(CURRENT_DATE, birth_date))::int
                            WHERE age IS NULL
                            """);
                }
                statement.execute("UPDATE donor SET age=18 WHERE age IS NULL OR age < 18");
                statement.execute("ALTER TABLE donor ALTER COLUMN age SET NOT NULL");
            }
            if (hasBirthDate) {
                statement.execute("ALTER TABLE donor DROP CONSTRAINT IF EXISTS chk_donor_birth_date");
                statement.execute("ALTER TABLE donor DROP COLUMN birth_date");
            }
            statement.execute("""
                    DO $$ BEGIN
                        ALTER TABLE donor ADD CONSTRAINT chk_donor_age CHECK (age >= 18);
                    EXCEPTION WHEN duplicate_object THEN NULL;
                    END $$;
                    """);
        }
    }

    private static void migrateDonorPhone(Connection connection) throws SQLException {
        if (hasColumn(connection, "phone")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE donor DROP COLUMN phone");
            }
        }
    }

    private static boolean hasColumn(Connection connection, String column) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT EXISTS (
                    SELECT 1 FROM information_schema.columns
                    WHERE table_schema='public' AND table_name='donor' AND column_name=?
                )
                """)) {
            statement.setString(1, column);
            try (var rows = statement.executeQuery()) {
                rows.next();
                return rows.getBoolean(1);
            }
        }
    }
}
