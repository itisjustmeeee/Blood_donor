package ru.mirea.project.repository;

import ru.mirea.project.util.DatabaseManager;
import ru.mirea.project.model.Donor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DonorRepository {
    private static final String SELECT = """
            SELECT d.donor_id, d.full_name, d.age, d.role::text, d.gender::text,
                   d.weight, d.email, d.password_hash, d.blood_group_id,
                   bg.blood_type || bg.rh_factor
            FROM donor d JOIN blood_group bg ON bg.blood_group_id = d.blood_group_id
            """;

    public void ensureRules() throws SQLException {
        String sql = """
                DO $$
                BEGIN
                    IF NOT EXISTS (
                        SELECT 1 FROM pg_constraint WHERE conname = 'chk_donor_weight_min'
                    ) THEN
                        ALTER TABLE donor
                        ADD CONSTRAINT chk_donor_weight_min CHECK (weight >= 50);
                    END IF;
                END $$;
                """;
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }

    public Donor findByEmail(String email) throws SQLException {
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(SELECT + " WHERE lower(d.email)=lower(?)")) {
            statement.setString(1, email);
            try (var rows = statement.executeQuery()) {
                return rows.next() ? map(rows) : null;
            }
        }
    }

    public Donor findById(int id) throws SQLException {
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(SELECT + " WHERE d.donor_id=?")) {
            statement.setInt(1, id);
            try (var rows = statement.executeQuery()) {
                return rows.next() ? map(rows) : null;
            }
        }
    }

    public List<Donor> findAll() throws SQLException {
        List<Donor> result = new ArrayList<>();
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(SELECT + " ORDER BY d.donor_id");
             var rows = statement.executeQuery()) {
            while (rows.next()) result.add(map(rows));
        }
        return result;
    }

    public Donor create(String name, int age, String role, String gender,
                        int weight, String email, String passwordHash,
                        int bloodGroupId) throws SQLException {
        String sql = """
                INSERT INTO donor(full_name,age,role,gender,weight,email,password_hash,blood_group_id)
                VALUES (?, ?, ?::user_role, ?::gender, ?, ?, ?, ?) RETURNING donor_id
                """;
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setInt(2, age);
            statement.setString(3, role);
            statement.setString(4, gender);
            statement.setInt(5, weight);
            statement.setString(6, email);
            statement.setString(7, passwordHash);
            statement.setInt(8, bloodGroupId);
            try (var rows = statement.executeQuery()) {
                rows.next();
                return findByEmail(email);
            }
        }
    }

    public Donor updateProfile(int donorId, int age, String gender, int weight) throws SQLException {
        String sql = """
                UPDATE donor SET age=?, gender=?::gender, weight=?
                WHERE donor_id=?
                """;
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, age);
            statement.setString(2, gender);
            statement.setInt(3, weight);
            statement.setInt(4, donorId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Профиль пользователя не найден.");
            }
        }
        return findById(donorId);
    }

    private Donor map(java.sql.ResultSet rows) throws SQLException {
        return new Donor(rows.getInt(1), rows.getString(2), rows.getInt(3),
                rows.getString(4), rows.getString(5), rows.getInt(6), rows.getString(7),
                rows.getString(8), rows.getInt(9), rows.getString(10));
    }
}
