package ru.mirea.project;

import ru.mirea.project.ui.ConsoleUI;
import ru.mirea.project.util.DatabaseInitializer;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        try {
            DatabaseInitializer.initialize();
        } catch (SQLException exception) {
            System.err.println("Не удалось создать или открыть базу данных: " + exception.getMessage());
            return;
        }
        new ConsoleUI().start();
    }
}