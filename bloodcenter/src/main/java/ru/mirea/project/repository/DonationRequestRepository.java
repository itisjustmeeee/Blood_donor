package ru.mirea.project.repository;

import ru.mirea.project.util.DatabaseManager;
import ru.mirea.project.model.DonationRequest;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DonationRequestRepository {
    private static final String SELECT = """
            SELECT r.request_id, r.donor_id, r.donation_date, r.request_status::text, d.full_name
            FROM donation_request r JOIN donor d ON d.donor_id=r.donor_id
            """;

    public DonationRequest create(int donorId, LocalDate date, String status) throws SQLException {
        String sql = "INSERT INTO donation_request(donor_id, donation_date, request_status) VALUES (?, ?, ?::request_status) RETURNING request_id";
        try (var connection = DatabaseManager.getConnection();
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

    public DonationRequest findById(int id) throws SQLException {
        List<DonationRequest> result = find(SELECT + " WHERE r.request_id=?", id);
        return result.isEmpty() ? null : result.get(0);
    }

    public boolean hasPendingRequest(int donorId) throws SQLException {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM donation_request r
                    WHERE r.donor_id=?
                      AND r.request_status IN ('created', 'confirmed')
                      AND NOT EXISTS (
                          SELECT 1 FROM medical_examination e
                          WHERE e.request_id=r.request_id
                      )
                )
                """;
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, donorId);
            try (var rows = statement.executeQuery()) {
                rows.next();
                return rows.getBoolean(1);
            }
        }
    }

    public List<DonationRequest> findForProcessing() throws SQLException {
        String sql = SELECT + """
                WHERE r.request_status IN ('created', 'confirmed')
                  AND NOT EXISTS (
                      SELECT 1 FROM medical_examination me
                      WHERE me.request_id=r.request_id
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM donation d
                      JOIN medical_examination e ON e.examination_id=d.examination_id
                      WHERE e.request_id=r.request_id
                  )
                ORDER BY r.donation_date, r.request_id
                """;
        return find(sql);
    }

    public void updateStatus(int requestId, String status) throws SQLException {
        String sql = "UPDATE donation_request SET request_status=?::request_status WHERE request_id=?";
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, requestId);
            statement.executeUpdate();
        }
    }

    private List<DonationRequest> find(String sql, Integer... parameters) throws SQLException {
        List<DonationRequest> result = new ArrayList<>();
        try (var connection = DatabaseManager.getConnection();
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



