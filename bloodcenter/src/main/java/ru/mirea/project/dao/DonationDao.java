package ru.mirea.project.dao;

import ru.mirea.project.db.DatabaseConnection;
import ru.mirea.project.model.Donation;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DonationDao {
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
        try (var connection = DatabaseConnection.getConnection();
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
        try (var connection = DatabaseConnection.getConnection();
             var statement = connection.prepareStatement(sql);
             var rows = statement.executeQuery()) {
            while (rows.next()) result.add(new Donation(rows.getInt(1), rows.getInt(2),
                    rows.getInt(3), rows.getDate(4).toLocalDate(), rows.getInt(5),
                    rows.getString(6), rows.getString(7), rows.getString(8), rows.getString(9)));
        }
        return result;
    }
}
