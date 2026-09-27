package ru.mirea.project.repository;

import ru.mirea.project.util.DatabaseManager;
import ru.mirea.project.model.BloodGroup;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BloodGroupRepository {
    public void ensureDefaults() throws SQLException {
        String sql = """
                INSERT INTO blood_group (blood_type, rh_factor)
                VALUES ('0', '+'), ('0', '-'), ('A', '+'), ('A', '-'),
                       ('B', '+'), ('B', '-'), ('AB', '+'), ('AB', '-')
                ON CONFLICT (blood_type, rh_factor) DO NOTHING
                """;
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    public List<BloodGroup> findAll() throws SQLException {
        String sql = "SELECT blood_group_id, blood_type, rh_factor FROM blood_group ORDER BY blood_group_id";
        List<BloodGroup> result = new ArrayList<>();
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql);
             var rows = statement.executeQuery()) {
            while (rows.next()) {
                result.add(new BloodGroup(rows.getInt(1), rows.getString(2), rows.getString(3)));
            }
        }
        return result;
    }
}



