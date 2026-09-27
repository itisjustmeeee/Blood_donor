package ru.mirea.project.repository;

import ru.mirea.project.util.DatabaseManager;
import ru.mirea.project.model.BloodBatch;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BloodBatchRepository {
    public void ensureRules() throws SQLException {
        String sql = """
                DO $$
                BEGIN
                    IF NOT EXISTS (
                        SELECT 1 FROM pg_constraint WHERE conname = 'chk_batch_dates'
                    ) THEN
                        ALTER TABLE blood_batch
                        ADD CONSTRAINT chk_batch_dates CHECK (expiration_date > preparation_date);
                    END IF;
                END $$;
                """;
        try (var connection = ru.mirea.project.util.DatabaseManager.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }

    public void ensureDefaults() throws SQLException {
        String sql = """
                INSERT INTO blood_batch
                    (blood_group_id, batch_number, preparation_date, expiration_date, total_volume, status)
                SELECT bg.blood_group_id, values.batch_number, CURRENT_DATE, CURRENT_DATE + 30,
                       450, 'available'::batch_status
                FROM (VALUES
                    ('0', '+', 'AUTO-0P'),
                    ('0', '-', 'AUTO-0M'),
                    ('A', '+', 'AUTO-AP'),
                    ('A', '-', 'AUTO-AM'),
                    ('B', '+', 'AUTO-BP'),
                    ('B', '-', 'AUTO-BM'),
                    ('AB', '+', 'AUTO-ABP'),
                    ('AB', '-', 'AUTO-ABM')
                ) AS values(blood_type, rh_factor, batch_number)
                JOIN blood_group bg
                  ON bg.blood_type=values.blood_type AND bg.rh_factor=values.rh_factor
                ON CONFLICT (batch_number) DO NOTHING
                """;
        try (var connection = ru.mirea.project.util.DatabaseManager.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
        String statusSql = """
                UPDATE blood_batch b
                SET status='available'::batch_status
                WHERE b.batch_number IN
                      ('AUTO-0P','AUTO-0M','AUTO-AP','AUTO-AM',
                       'AUTO-BP','AUTO-BM','AUTO-ABP','AUTO-ABM')
                  AND COALESCE((SELECT SUM(d.blood_volume)
                                FROM donation d WHERE d.batch_id=b.batch_id), 0) < b.total_volume
                """;
        try (var connection = ru.mirea.project.util.DatabaseManager.getConnection();
                var statement = connection.prepareStatement(statusSql)) {
            statement.executeUpdate();
        }
    }

    public List<BloodBatch> findAvailableForRequest(int requestId) throws SQLException {
        String sql = """
                SELECT b.batch_id,b.blood_group_id,b.batch_number,b.preparation_date,
                       b.expiration_date,b.total_volume,b.status::text,g.blood_type||g.rh_factor
                FROM blood_batch b
                JOIN blood_group g ON g.blood_group_id=b.blood_group_id
                JOIN donor d ON d.blood_group_id=b.blood_group_id
                JOIN donation_request r ON r.donor_id=d.donor_id
                WHERE r.request_id=? AND b.status='available'::batch_status
                ORDER BY b.batch_number
                """;
        List<BloodBatch> result = new ArrayList<>();
        try (var connection = DatabaseManager.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, requestId);
            try (var rows = statement.executeQuery()) {
                while (rows.next())
                    result.add(new BloodBatch(rows.getInt(1), rows.getInt(2),
                            rows.getString(3), rows.getDate(4).toLocalDate(), rows.getDate(5).toLocalDate(),
                            rows.getInt(6), rows.getString(7), rows.getString(8)));
            }
        }
        return result;
    }

    public List<BloodBatch> findAll() throws SQLException {
        String sql = """
                SELECT b.batch_id,b.blood_group_id,b.batch_number,b.preparation_date,
                       b.expiration_date,b.total_volume,b.status::text,g.blood_type||g.rh_factor
                FROM blood_batch b JOIN blood_group g ON g.blood_group_id=b.blood_group_id
                ORDER BY b.batch_number
                """;
        List<BloodBatch> result = new ArrayList<>();
        try (var connection = DatabaseManager.getConnection();
                var statement = connection.prepareStatement(sql);
                var rows = statement.executeQuery()) {
            while (rows.next())
                result.add(new BloodBatch(rows.getInt(1), rows.getInt(2),
                        rows.getString(3), rows.getDate(4).toLocalDate(), rows.getDate(5).toLocalDate(),
                        rows.getInt(6), rows.getString(7), rows.getString(8)));
        }
        return result;
    }
}
