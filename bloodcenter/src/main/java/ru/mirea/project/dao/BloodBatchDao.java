package ru.mirea.project.dao;

import ru.mirea.project.db.DatabaseConnection;
import ru.mirea.project.model.BloodBatch;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BloodBatchDao {
    public List<BloodBatch> findAll() throws SQLException {
        String sql = """
                SELECT b.batch_id,b.blood_group_id,b.batch_number,b.preparation_date,
                       b.expiration_date,b.total_volume,b.status::text,g.blood_type||g.rh_factor
                FROM blood_batch b JOIN blood_group g ON g.blood_group_id=b.blood_group_id
                ORDER BY b.batch_number
                """;
        List<BloodBatch> result = new ArrayList<>();
        try (var connection = DatabaseConnection.getConnection();
             var statement = connection.prepareStatement(sql);
             var rows = statement.executeQuery()) {
            while (rows.next()) result.add(new BloodBatch(rows.getInt(1), rows.getInt(2),
                    rows.getString(3), rows.getDate(4).toLocalDate(), rows.getDate(5).toLocalDate(),
                    rows.getInt(6), rows.getString(7), rows.getString(8)));
        }
        return result;
    }
}
