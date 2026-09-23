package org.example.blood_donation_system;

import org.example.blood_donation_system.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

public class TestConnection {
    public static void main(String[] args) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            System.out.println("Подключение к базе данных успешно");
        } catch (SQLException e) {
            System.out.println("Ошибка подключения к базе данных: ");
            e.printStackTrace();
        }
    }
}
