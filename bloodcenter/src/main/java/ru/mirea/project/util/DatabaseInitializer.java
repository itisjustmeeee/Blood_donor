package ru.mirea.project.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

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
        String[] statements = {
                """
                DO $$ BEGIN
                    CREATE TYPE user_role AS ENUM ('donor', 'doctor');
                EXCEPTION WHEN duplicate_object THEN NULL;
                END $$;
                """,
                """
                DO $$ BEGIN
                    CREATE TYPE gender AS ENUM ('male', 'female', 'other');
                EXCEPTION WHEN duplicate_object THEN NULL;
                END $$;
                """,
                """
                DO $$ BEGIN
                    CREATE TYPE admission_status AS ENUM ('pending', 'accepted', 'rejected');
                EXCEPTION WHEN duplicate_object THEN NULL;
                END $$;
                """,
                """
                DO $$ BEGIN
                    CREATE TYPE request_status AS ENUM ('created', 'confirmed', 'completed', 'cancelled');
                EXCEPTION WHEN duplicate_object THEN NULL;
                END $$;
                """,
                """
                DO $$ BEGIN
                    CREATE TYPE donation_type AS ENUM ('whole_blood', 'plasma', 'platelets');
                EXCEPTION WHEN duplicate_object THEN NULL;
                END $$;
                """,
                """
                DO $$ BEGIN
                    CREATE TYPE batch_status AS ENUM ('available', 'reserved', 'used', 'expired', 'disposed');
                EXCEPTION WHEN duplicate_object THEN NULL;
                END $$;
                """,
                """
                DO $$ BEGIN
                    CREATE TYPE donation_result AS ENUM ('successful', 'unsuccessful');
                EXCEPTION WHEN duplicate_object THEN NULL;
                END $$;
                """,
                """
                CREATE TABLE IF NOT EXISTS blood_group (
                    blood_group_id SERIAL PRIMARY KEY,
                    blood_type VARCHAR(2) NOT NULL,
                    rh_factor CHAR(1) NOT NULL,
                    CONSTRAINT chk_blood_type CHECK (blood_type IN ('A', 'B', 'AB', '0')),
                    CONSTRAINT chk_rh_factor CHECK (rh_factor IN ('+', '-')),
                    CONSTRAINT uq_blood_group UNIQUE (blood_type, rh_factor)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS donor (
                    donor_id SERIAL PRIMARY KEY,
                    full_name VARCHAR(150) NOT NULL,
                    age INT NOT NULL,
                    role user_role NOT NULL,
                    gender gender NOT NULL,
                    weight INT NOT NULL,
                    email VARCHAR(100) NOT NULL UNIQUE,
                    password_hash VARCHAR(100) NOT NULL,
                    blood_group_id INT NOT NULL REFERENCES blood_group(blood_group_id),
                    CONSTRAINT chk_donor_weight CHECK (weight >= 50),
                    CONSTRAINT chk_donor_age CHECK (age >= 18)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS donation_request (
                    request_id SERIAL PRIMARY KEY,
                    donor_id INT NOT NULL REFERENCES donor(donor_id),
                    donation_date DATE NOT NULL,
                    request_status request_status NOT NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS medical_examination (
                    examination_id SERIAL PRIMARY KEY,
                    request_id INT NOT NULL UNIQUE REFERENCES donation_request(request_id),
                    examination_date DATE NOT NULL,
                    hemoglobin DECIMAL(5, 2),
                    blood_pressure VARCHAR(20),
                    conclusion VARCHAR(200),
                    admission_status admission_status NOT NULL,
                    CONSTRAINT chk_hemoglobin CHECK (hemoglobin IS NULL OR hemoglobin > 0)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS blood_batch (
                    batch_id SERIAL PRIMARY KEY,
                    blood_group_id INT NOT NULL REFERENCES blood_group(blood_group_id),
                    batch_number VARCHAR(50) NOT NULL UNIQUE,
                    preparation_date DATE NOT NULL,
                    expiration_date DATE NOT NULL,
                    total_volume INT NOT NULL,
                    status batch_status NOT NULL,
                    CONSTRAINT chk_batch_volume CHECK (total_volume > 0),
                    CONSTRAINT chk_batch_dates CHECK (expiration_date > preparation_date)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS donation (
                    donation_id SERIAL PRIMARY KEY,
                    examination_id INT NOT NULL UNIQUE REFERENCES medical_examination(examination_id),
                    batch_id INT NOT NULL REFERENCES blood_batch(batch_id),
                    donation_date DATE NOT NULL,
                    blood_volume INT NOT NULL,
                    donation_type donation_type NOT NULL,
                    result donation_result NOT NULL,
                    CONSTRAINT chk_donation_volume CHECK (blood_volume > 0 AND blood_volume <= 450)
                )
                """
        };
        try (Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
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
