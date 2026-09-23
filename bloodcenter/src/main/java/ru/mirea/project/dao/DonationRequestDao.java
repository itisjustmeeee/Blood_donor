package ru.mirea.project.dao;

import ru.mirea.project.db.DatabaseConnection;
import ru.mirea.project.model.DonationRequest;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DonationRequestDao {
    private static final String SELECT = """
            SELECT r.request_id, r.donor_id, r.donation_date, r.request_status::text, d.full_name
            FROM donation_request r JOIN donor d ON d.donor_id=r.donor_id
            """;

    public DonationRequest create(int donorId, LocalDate date, String status) throws SQLException {
        String sql = "INSERT INTO donation_request(donor_id, donation_date, request_status) VALUES (?, ?, ?::request_status) RETURNING request_id";
        try (var connection = DatabaseConnection.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, donorId);
            statement.setDate(2, Date.valueOf(date));
            statement.setString(3, status);
            try (var rows = statement.executeQuery()) {
                rows.next();
                return findById(rows.getInt(1));
            }
        }
    }

    public List<DonationRequest> findByDonor(int donorId) throws SQLException {
        return find(SELECT + " WHERE r.donor_id=? ORDER BY r.donation_date", donorId);
    }

    public List<DonationRequest> findAll() throws SQLException {
        return find(SELECT + " ORDER BY r.donation_date, r.request_id");
    }

    private DonationRequest findById(int id) throws SQLException {
        return find(SELECT + " WHERE r.request_id=?", id).get(0);
    }

    private List<DonationRequest> find(String sql, Integer... parameters) throws SQLException {
        List<DonationRequest> result = new ArrayList<>();
        try (var connection = DatabaseConnection.getConnection();
             var statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) statement.setInt(i + 1, parameters[i]);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) result.add(new DonationRequest(rows.getInt(1), rows.getInt(2),
                        rows.getDate(3).toLocalDate(), rows.getString(4), rows.getString(5)));
            }
        }
        return result;
    }
}
