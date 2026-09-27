package ru.mirea.project.repository;

import ru.mirea.project.util.DatabaseManager;
import ru.mirea.project.model.Donation;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DonationRepository {
    public void ensureRules() throws SQLException {
        String sql = """
                DO $$
                BEGIN
                    IF NOT EXISTS (
                        SELECT 1 FROM pg_constraint
                        WHERE conname = 'chk_donation_volume_limit'
                    ) THEN
                        ALTER TABLE donation
                        ADD CONSTRAINT chk_donation_volume_limit CHECK (blood_volume <= 450);
                    END IF;
                END $$;
                """;
        try (var connection = ru.mirea.project.util.DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }

    public Donation create(int requestId, int examinationId, int batchId,
                           java.time.LocalDate donationDate, int bloodVolume,
                           String donationType, String result) throws SQLException {
        if (bloodVolume <= 0 || bloodVolume > 450) {
            throw new SQLException("Объём донации должен быть от 1 до 450 мл.");
        }
        try (var connection = ru.mirea.project.util.DatabaseManager.getConnection();
             var batchStatement = connection.prepareStatement("""
                     SELECT b.total_volume, b.status::text,
                            COALESCE((SELECT SUM(d.blood_volume) FROM donation d
                                      WHERE d.batch_id=b.batch_id), 0)
                     FROM blood_batch b WHERE b.batch_id=? FOR UPDATE
                     """);
             var validation = connection.prepareStatement("""
                     SELECT EXISTS (
                         SELECT 1
                         FROM medical_examination e
                         JOIN donation_request r ON r.request_id=e.request_id
                         WHERE e.examination_id=? AND r.donor_id=(
                             SELECT donor_id FROM donation_request WHERE request_id=?
                         ) AND e.admission_status='accepted'::admission_status
                     )
                     """);
             var insert = connection.prepareStatement("""
                     INSERT INTO donation(examination_id,batch_id,donation_date,blood_volume,donation_type,result)
                     VALUES (?, ?, ?, ?, ?::donation_type, ?::donation_result)
                     RETURNING donation_id
                     """);
             var updateBatch = connection.prepareStatement("""
                     UPDATE blood_batch
                     SET status='reserved'::batch_status
                     WHERE batch_id=? AND
                           (SELECT COALESCE(SUM(d.blood_volume), 0)
                            FROM donation d WHERE d.batch_id=?) >= total_volume
                     """)) {
            connection.setAutoCommit(false);
            batchStatement.setInt(1, batchId);
            try (var rows = batchStatement.executeQuery()) {
                if (!rows.next()) throw new SQLException("Партия крови не найдена.");
                int totalVolume = rows.getInt(1);
                String status = rows.getString(2);
                int usedVolume = rows.getInt(3);
                if (!"available".equals(status)) {
                    throw new SQLException("Выбранная партия крови уже недоступна.");
                }
                if (usedVolume + bloodVolume > totalVolume) {
                    throw new SQLException("Объём превышает остаток выбранной партии.");
                }
            }
            validation.setInt(1, examinationId);
            validation.setInt(2, requestId);
            try (var rows = validation.executeQuery()) {
                rows.next();
                if (!rows.getBoolean(1)) {
                    throw new SQLException("Не удалось связать донацию с положительным обследованием.");
                }
            }
            insert.setInt(1, examinationId);
            insert.setInt(2, batchId);
            insert.setDate(3, java.sql.Date.valueOf(donationDate));
            insert.setInt(4, bloodVolume);
            insert.setString(5, donationType);
            insert.setString(6, result);
            int donationId;
            try (var rows = insert.executeQuery()) {
                rows.next();
                donationId = rows.getInt(1);
            }
            updateBatch.setInt(1, batchId);
            updateBatch.setInt(2, batchId);
            updateBatch.executeUpdate();
            connection.commit();
            return findById(donationId);
        } catch (SQLException exception) {
            throw exception;
        }
    }

    public List<Donation> findByDonor(int donorId) throws SQLException {
        String sql = """
                SELECT x.donation_id,x.examination_id,x.batch_id,x.donation_date,x.blood_volume,
                       x.donation_type::text,x.result::text,d.full_name,g.blood_type||g.rh_factor
                FROM donation x
                JOIN medical_examination e ON e.examination_id=x.examination_id
                JOIN donation_request r ON r.request_id=e.request_id
                JOIN donor d ON d.donor_id=r.donor_id
                JOIN blood_batch b ON b.batch_id=x.batch_id
                JOIN blood_group g ON g.blood_group_id=b.blood_group_id
                WHERE d.donor_id=?
                ORDER BY x.donation_date,x.donation_id
                """;
        List<Donation> result = new ArrayList<>();
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, donorId);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) result.add(new Donation(rows.getInt(1), rows.getInt(2),
                        rows.getInt(3), rows.getDate(4).toLocalDate(), rows.getInt(5),
                        rows.getString(6), rows.getString(7), rows.getString(8), rows.getString(9)));
            }
        }
        return result;
    }

    public List<Donation> findAll() throws SQLException {
        String sql = """
                SELECT x.donation_id,x.examination_id,x.batch_id,x.donation_date,x.blood_volume,
                       x.donation_type::text,x.result::text,d.full_name,g.blood_type||g.rh_factor
                FROM donation x
                JOIN medical_examination e ON e.examination_id=x.examination_id
                JOIN donation_request r ON r.request_id=e.request_id
                JOIN donor d ON d.donor_id=r.donor_id
                JOIN blood_batch b ON b.batch_id=x.batch_id
                JOIN blood_group g ON g.blood_group_id=b.blood_group_id
                ORDER BY x.donation_date,x.donation_id
                """;
        List<Donation> result = new ArrayList<>();
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql);
             var rows = statement.executeQuery()) {
            while (rows.next()) result.add(new Donation(rows.getInt(1), rows.getInt(2),
                    rows.getInt(3), rows.getDate(4).toLocalDate(), rows.getInt(5),
                    rows.getString(6), rows.getString(7), rows.getString(8), rows.getString(9)));
        }
        return result;
    }

    private Donation findById(int donationId) throws SQLException {
        return findAll().stream()
                .filter(donation -> donation.id() == donationId)
                .findFirst()
                .orElseThrow(() -> new SQLException("Созданная донация не найдена."));
    }
}



