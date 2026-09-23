package ru.mirea.project.dao;

import ru.mirea.project.db.DatabaseConnection;
import ru.mirea.project.model.MedicalExamination;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MedicalExaminationDao {
    private static final String SELECT = """
            SELECT e.examination_id,e.request_id,e.examination_date,e.hemoglobin,
                   e.blood_pressure,e.conclusion,e.admission_status::text,d.full_name
            FROM medical_examination e
            JOIN donation_request r ON r.request_id=e.request_id
            JOIN donor d ON d.donor_id=r.donor_id
            """;

    public MedicalExamination create(int requestId, LocalDate date) throws SQLException {
        String sql = "INSERT INTO medical_examination(request_id,examination_date,admission_status) VALUES (?, ?, 'pending'::admission_status) RETURNING examination_id";
        try (var connection = DatabaseConnection.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, requestId);
            statement.setDate(2, Date.valueOf(date));
            try (var rows = statement.executeQuery()) {
                rows.next();
                return findById(rows.getInt(1));
            }
        }
    }

    public void updateResult(int id, BigDecimal hemoglobin, String pressure,
                             String conclusion, String status) throws SQLException {
        String sql = "UPDATE medical_examination SET hemoglobin=?,blood_pressure=?,conclusion=?,admission_status=?::admission_status WHERE examination_id=?";
        try (var connection = DatabaseConnection.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, hemoglobin);
            statement.setString(2, pressure);
            statement.setString(3, conclusion);
            statement.setString(4, status);
            statement.setInt(5, id);
            statement.executeUpdate();
        }
    }

    public List<MedicalExamination> findByDonor(int donorId) throws SQLException {
        return find(SELECT + " WHERE r.donor_id=? ORDER BY e.examination_date", donorId);
    }

    public List<MedicalExamination> findAll() throws SQLException {
        return find(SELECT + " ORDER BY e.examination_date,e.examination_id");
    }

    private MedicalExamination findById(int id) throws SQLException {
        return find(SELECT + " WHERE e.examination_id=?", id).get(0);
    }

    private List<MedicalExamination> find(String sql, Integer... parameters) throws SQLException {
        List<MedicalExamination> result = new ArrayList<>();
        try (var connection = DatabaseConnection.getConnection();
             var statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) statement.setInt(i + 1, parameters[i]);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) result.add(new MedicalExamination(rows.getInt(1), rows.getInt(2),
                        rows.getDate(3).toLocalDate(), rows.getObject(4, BigDecimal.class),
                        rows.getString(5), rows.getString(6), rows.getString(7), rows.getString(8)));
            }
        }
        return result;
    }
}
