package ru.mirea.project;

import ru.mirea.project.util.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;

public final class TestConnection {
    private TestConnection() {
    }

    public static void main(String[] args) {
        try (Connection ignored = DatabaseManager.getConnection()) {
            System.out.println("Подключение к базе данных успешно");
        } catch (SQLException exception) {
            System.err.println("Ошибка подключения к базе данных: " + exception.getMessage());
        }
    }
}
