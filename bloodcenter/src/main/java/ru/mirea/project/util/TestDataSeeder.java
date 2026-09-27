package ru.mirea.project.util;

import ru.mirea.project.security.PasswordHasher;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class TestDataSeeder {
    private static final String TEST_PASSWORD = "password";
    private static final String MARKER_EMAIL = "test.multi@example.com";

    private TestDataSeeder() {
    }

    public static boolean seed() throws SQLException {
        try (Connection connection = DatabaseManager.getConnection()) {
            if (exists(connection, "SELECT 1 FROM donor WHERE email=?")) {
                return false;
            }
            connection.setAutoCommit(false);
            try {
                List<Integer> donors = new ArrayList<>();
                donors.add(insertDonor(connection, "Иванов Иван Иванович", 36,
                        "male", 82, MARKER_EMAIL, "A", "+"));
                donors.add(insertDonor(connection, "Петрова Анна Сергеевна", 31,
                        "female", 64, "test.examination@example.com", "0", "+"));
                donors.add(insertDonor(connection, "Сидоров Алексей Павлович", 33,
                        "male", 90, "test.accepted@example.com", "B", "+"));
                donors.add(insertDonor(connection, "Кузнецова Мария Андреевна", 28,
                        "female", 58, "test.rejected@example.com", "AB", "+"));
                insertDonor(connection, "Врачова Елена Игоревна", 39,
                        "female", 61, "test.doctor@example.com", "0", "-", "doctor");

                List<Integer> examinations = new ArrayList<>();
                for (int i = 0; i < 7; i++) {
                    examinations.add(insertExamination(connection, donors.get(0),
                            LocalDate.now().minusDays(20L - i), "accepted", "145.00",
                            "120/80", "Противопоказаний не выявлено",
                            i < 2 ? "completed" : "confirmed"));
                }
                examinations.add(insertExamination(connection, donors.get(1),
                        LocalDate.now().minusDays(3), "pending", null, null, null, "confirmed"));
                examinations.add(insertExamination(connection, donors.get(2),
                        LocalDate.now().minusDays(2), "accepted", "150.00",
                        "120/80", "Противопоказаний не выявлено", "completed"));
                examinations.add(insertExamination(connection, donors.get(3),
                        LocalDate.now().minusDays(1), "rejected", "110.00",
                        "118/75", "Требуется повторное обследование", "confirmed"));

                insertDonation(connection, examinations.get(0), "AUTO-AP",
                        LocalDate.now().minusDays(20));
                insertDonation(connection, examinations.get(1), "AUTO-BP",
                        LocalDate.now().minusDays(19));
                insertDonation(connection, examinations.get(8), "AUTO-ABP",
                        LocalDate.now().minusDays(2));
                connection.commit();
                return true;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private static int insertDonor(Connection connection, String name, int age,
            String gender, int weight, String email,
            String bloodType, String rhFactor) throws SQLException {
        return insertDonor(connection, name, age, gender, weight, email,
                bloodType, rhFactor, "donor");
    }

    private static int insertDonor(Connection connection, String name, int age,
            String gender, int weight, String email,
            String bloodType, String rhFactor, String role)
            throws SQLException {
        String sql = """
                INSERT INTO donor(full_name,age,role,gender,weight,email,password_hash,blood_group_id)
                SELECT ?, ?, ?::user_role, ?::gender, ?, ?, ?, blood_group_id
                FROM blood_group WHERE blood_type=? AND rh_factor=?
                RETURNING donor_id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setInt(2, age);
            statement.setString(3, role);
            statement.setString(4, gender);
            statement.setInt(5, weight);
            statement.setString(6, email);
            statement.setString(7, PasswordHasher.hash(TEST_PASSWORD));
            statement.setString(8, bloodType);
            statement.setString(9, rhFactor);
            try (var rows = statement.executeQuery()) {
                if (!rows.next()) {
                    throw new SQLException("Не найдена группа крови для тестовых данных.");
                }
                return rows.getInt(1);
            }
        }
    }

    private static int insertExamination(Connection connection, int donorId, LocalDate date,
            String admissionStatus, String hemoglobin,
            String pressure, String conclusion, String requestStatus)
            throws SQLException {
        int requestId;
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO donation_request(donor_id,donation_date,request_status)
                VALUES (?, ?, ?::request_status) RETURNING request_id
                """)) {
            statement.setInt(1, donorId);
            statement.setDate(2, Date.valueOf(date));
            statement.setString(3, requestStatus);
            try (var rows = statement.executeQuery()) {
                rows.next();
                requestId = rows.getInt(1);
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO medical_examination(
                    request_id,examination_date,hemoglobin,blood_pressure,conclusion,admission_status)
                VALUES (?, ?, ?, ?, ?, ?::admission_status) RETURNING examination_id
                """)) {
            statement.setInt(1, requestId);
            statement.setDate(2, Date.valueOf(date));
            if (hemoglobin == null)
                statement.setNull(3, java.sql.Types.NUMERIC);
            else
                statement.setBigDecimal(3, new java.math.BigDecimal(hemoglobin));
            statement.setString(4, pressure);
            statement.setString(5, conclusion);
            statement.setString(6, admissionStatus);
            try (var rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        }
    }

    private static void insertDonation(Connection connection, int examinationId,
            String batchNumber, LocalDate date) throws SQLException {
        String sql = """
                INSERT INTO donation(examination_id,batch_id,donation_date,blood_volume,donation_type,result)
                SELECT ?, batch_id, ?, 450, 'whole_blood'::donation_type,
                       'successful'::donation_result
                FROM blood_batch WHERE batch_number=?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, examinationId);
            statement.setDate(2, Date.valueOf(date));
            statement.setString(3, batchNumber);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Не найдена партия крови " + batchNumber + ".");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE blood_batch SET status='reserved'::batch_status
                WHERE batch_number=? AND
                    (SELECT COALESCE(SUM(blood_volume), 0) FROM donation
                     WHERE batch_id=blood_batch.batch_id) >= total_volume
                """)) {
            statement.setString(1, batchNumber);
            statement.executeUpdate();
        }
    }

    private static boolean exists(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, MARKER_EMAIL);
            try (var rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }
}
