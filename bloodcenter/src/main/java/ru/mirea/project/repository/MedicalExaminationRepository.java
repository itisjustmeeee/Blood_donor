package ru.mirea.project.repository;

import ru.mirea.project.util.DatabaseManager;
import ru.mirea.project.model.MedicalExamination;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MedicalExaminationRepository {
    private static final String SELECT = """
            SELECT e.examination_id,e.request_id,e.examination_date,e.hemoglobin,
                   e.blood_pressure,e.conclusion,e.admission_status::text,d.full_name
            FROM medical_examination e
            JOIN donation_request r ON r.request_id=e.request_id
            JOIN donor d ON d.donor_id=r.donor_id
            """;

    public MedicalExamination create(int requestId, LocalDate date, BigDecimal hemoglobin) throws SQLException {
        String sql = """
                INSERT INTO medical_examination(request_id,examination_date,hemoglobin,admission_status)
                VALUES (?, ?, ?, 'pending'::admission_status) RETURNING examination_id
                """;
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, requestId);
            statement.setDate(2, Date.valueOf(date));
            statement.setBigDecimal(3, hemoglobin);
            try (var rows = statement.executeQuery()) {
                rows.next();
                return findById(rows.getInt(1));
            }
        }
    }

    public void updateResult(int id, BigDecimal hemoglobin, String pressure,
                             String conclusion, String status) throws SQLException {
        String sql = "UPDATE medical_examination SET hemoglobin=?,blood_pressure=?,conclusion=?,admission_status=?::admission_status WHERE examination_id=?";
        try (var connection = DatabaseManager.getConnection();
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

    public MedicalExamination findById(int id) throws SQLException {
        List<MedicalExamination> result = find(SELECT + " WHERE e.examination_id=?", id);
        return result.isEmpty() ? null : result.get(0);
    }

    public MedicalExamination findByIdAndDonor(int examinationId, int donorId) throws SQLException {
        List<MedicalExamination> result = find(SELECT
                + " WHERE e.examination_id=? AND r.donor_id=?", examinationId, donorId);
        return result.isEmpty() ? null : result.get(0);
    }

    public void updateDate(int examinationId, LocalDate date) throws SQLException {
        try (var connection = DatabaseManager.getConnection();
             var requestIdStatement = connection.prepareStatement(
                     "SELECT request_id FROM medical_examination WHERE examination_id=?");
             var examinationStatement = connection.prepareStatement(
                     "UPDATE medical_examination SET examination_date=? WHERE examination_id=?");
             var requestStatement = connection.prepareStatement(
                     "UPDATE donation_request SET donation_date=? WHERE request_id=?")) {
            connection.setAutoCommit(false);
            requestIdStatement.setInt(1, examinationId);
            int requestId;
            try (var rows = requestIdStatement.executeQuery()) {
                if (!rows.next()) {
                    throw new SQLException("Медицинское обследование не найдено.");
                }
                requestId = rows.getInt(1);
            }
            examinationStatement.setDate(1, Date.valueOf(date));
            examinationStatement.setInt(2, examinationId);
            examinationStatement.executeUpdate();
            requestStatement.setDate(1, Date.valueOf(date));
            requestStatement.setInt(2, requestId);
            requestStatement.executeUpdate();
            connection.commit();
        }
    }

    public void deleteWithRequest(int examinationId) throws SQLException {
        try (var connection = DatabaseManager.getConnection();
             var requestIdStatement = connection.prepareStatement(
                     "SELECT request_id FROM medical_examination WHERE examination_id=?");
             var donationStatement = connection.prepareStatement(
                     "SELECT EXISTS (SELECT 1 FROM donation WHERE examination_id=?)");
             var deleteExamination = connection.prepareStatement(
                     "DELETE FROM medical_examination WHERE examination_id=?");
             var deleteRequest = connection.prepareStatement(
                     "DELETE FROM donation_request WHERE request_id=?")) {
            connection.setAutoCommit(false);
            requestIdStatement.setInt(1, examinationId);
            int requestId;
            try (var rows = requestIdStatement.executeQuery()) {
                if (!rows.next()) {
                    throw new SQLException("Медицинское обследование не найдено.");
                }
                requestId = rows.getInt(1);
            }
            donationStatement.setInt(1, examinationId);
            try (var rows = donationStatement.executeQuery()) {
                rows.next();
                if (rows.getBoolean(1)) {
                    throw new SQLException("Нельзя удалить обследование, по нему уже оформлена донация.");
                }
            }
            deleteExamination.setInt(1, examinationId);
            deleteExamination.executeUpdate();
            deleteRequest.setInt(1, requestId);
            deleteRequest.executeUpdate();
            connection.commit();
        } catch (SQLException exception) {
            throw exception;
        }
    }

    private List<MedicalExamination> find(String sql, Integer... parameters) throws SQLException {
        List<MedicalExamination> result = new ArrayList<>();
        try (var connection = DatabaseManager.getConnection();
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
