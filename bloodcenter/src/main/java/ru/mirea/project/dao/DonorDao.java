package ru.mirea.project.dao;

import ru.mirea.project.db.DatabaseConnection;
import ru.mirea.project.model.Donor;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DonorDao {
    private static final String SELECT = """
            SELECT d.donor_id, d.full_name, d.birth_date, d.role::text, d.gender::text,
                   d.weight, d.email, d.phone, d.password_hash, d.blood_group_id,
                   bg.blood_type || bg.rh_factor
            FROM donor d JOIN blood_group bg ON bg.blood_group_id = d.blood_group_id
            """;

    public Donor findByEmail(String email) throws SQLException {
        try (var connection = DatabaseConnection.getConnection();
             var statement = connection.prepareStatement(SELECT + " WHERE lower(d.email)=lower(?)")) {
            statement.setString(1, email);
            try (var rows = statement.executeQuery()) {
                return rows.next() ? map(rows) : null;
            }
        }
    }

    public Donor findByPhone(String phone) throws SQLException {
        try (var connection = DatabaseConnection.getConnection();
             var statement = connection.prepareStatement(SELECT + " WHERE d.phone=?")) {
            statement.setString(1, phone);
            try (var rows = statement.executeQuery()) {
                return rows.next() ? map(rows) : null;
            }
        }
    }

    public List<Donor> findAll() throws SQLException {
        List<Donor> result = new ArrayList<>();
        try (var connection = DatabaseConnection.getConnection();
             var statement = connection.prepareStatement(SELECT + " ORDER BY d.donor_id");
             var rows = statement.executeQuery()) {
            while (rows.next()) result.add(map(rows));
        }
        return result;
    }

    public Donor create(String name, LocalDate birthDate, String role, String gender,
                        int weight, String email, String phone, String passwordHash,
                        int bloodGroupId) throws SQLException {
        String sql = """
                INSERT INTO donor(full_name,birth_date,role,gender,weight,email,phone,password_hash,blood_group_id)
                VALUES (?, ?, ?::user_role, ?::gender, ?, ?, ?, ?, ?) RETURNING donor_id
                """;
        try (var connection = DatabaseConnection.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setDate(2, Date.valueOf(birthDate));
            statement.setString(3, role);
            statement.setString(4, gender);
            statement.setInt(5, weight);
            statement.setString(6, email);
            statement.setString(7, phone);
            statement.setString(8, passwordHash);
            statement.setInt(9, bloodGroupId);
            try (var rows = statement.executeQuery()) {
                rows.next();
                return findByEmail(email);
            }
        }
    }

    private Donor map(java.sql.ResultSet rows) throws SQLException {
        return new Donor(rows.getInt(1), rows.getString(2), rows.getDate(3).toLocalDate(),
                rows.getString(4), rows.getString(5), rows.getInt(6), rows.getString(7),
                rows.getString(8), rows.getString(9), rows.getInt(10), rows.getString(11));
    }
}
