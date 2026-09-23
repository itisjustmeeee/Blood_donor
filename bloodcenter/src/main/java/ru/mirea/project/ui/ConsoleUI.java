package ru.mirea.project.ui;

import ru.mirea.project.dao.BloodBatchDao;
import ru.mirea.project.dao.BloodGroupDao;
import ru.mirea.project.dao.DonationDao;
import ru.mirea.project.dao.DonationRequestDao;
import ru.mirea.project.dao.DonorDao;
import ru.mirea.project.dao.MedicalExaminationDao;
import ru.mirea.project.model.BloodBatch;
import ru.mirea.project.model.BloodGroup;
import ru.mirea.project.model.DonationRequest;
import ru.mirea.project.model.Donor;
import ru.mirea.project.model.MedicalExamination;
import ru.mirea.project.security.PasswordHasher;

import java.math.BigDecimal;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class ConsoleUI {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.uuuu");
    private final Scanner scanner = new Scanner(
            new InputStreamReader(System.in, StandardCharsets.UTF_8));
    private final DonorDao donorDao = new DonorDao();
    private final BloodGroupDao bloodGroupDao = new BloodGroupDao();
    private final DonationRequestDao requestDao = new DonationRequestDao();
    private final MedicalExaminationDao examinationDao = new MedicalExaminationDao();
    private final BloodBatchDao batchDao = new BloodBatchDao();
    private final DonationDao donationDao = new DonationDao();
    public void start() {
        System.out.println("=== Центр донорства крови ===");
        boolean running = true;
        while (running) {
            System.out.println("\n1. Регистрация\n2. Вход\n3. Проверить подключение\n0. Выход");
            switch (read("Выберите действие: ")) {
                case "1" -> register();
                case "2" -> login();
                case "3" -> testConnection();
                case "0" -> running = false;
                default -> error("Неизвестная команда.");
            }
        }
    }

    private void register() {
        try {
            System.out.println("\n=== Регистрация ===");
            String role = readRole();
            String name = required("ФИО: ");
            LocalDate birthDate = date("Дата рождения (ДД.ММ.ГГГГ): ");
            String gender = readGender();
            int weight = positiveInt("Вес (кг): ");
            String email = required("Email: ");
            String phone = required("Телефон: ");
            String password = required("Пароль: ");
            List<BloodGroup> groups = bloodGroupDao.findAll();
            if (groups.isEmpty()) {
                error("В таблице blood_group нет групп крови.");
                return;
            }
            BloodGroup group = chooseGroup(groups);
            Donor donor = donorDao.create(name, birthDate, role, gender, weight, email, phone,
                    PasswordHasher.hash(password), group.id());
            System.out.println("Регистрация завершена. Идентификатор: " + donor.id());
        } catch (SQLException exception) {
            databaseError(exception);
        }
    }

    private void login() {
        try {
            String email = required("Email: ");
            Donor donor = donorDao.findByEmail(email);
            String password = required("Пароль: ");
            if (donor == null || !PasswordHasher.matches(password, donor.passwordHash())) {
                error("Неверный email или пароль.");
                return;
            }
            System.out.println("Добро пожаловать, " + donor.fullName() + "!");
            if ("doctor".equalsIgnoreCase(donor.role())) doctorMenu(donor);
            else donorMenu(donor);
        } catch (SQLException exception) {
            databaseError(exception);
        }
    }

    private void donorMenu(Donor donor) throws SQLException {
        boolean active = true;
        while (active) {
            System.out.println("\n=== Кабинет донора ===\n1. Профиль\n2. Записаться на обследование"
                    + "\n3. Мои обследования\n4. Мои записи на донацию\n5. Статистика\n0. Выход");
            switch (read("Выберите действие: ")) {
                case "1" -> printDonor(donor);
                case "2" -> bookExamination(donor);
                case "3" -> printExaminations(examinationDao.findByDonor(donor.id()));
                case "4" -> printRequests(requestDao.findByDonor(donor.id()));
                case "5" -> donorStatistics(donor);
                case "0" -> active = false;
                default -> error("Неизвестная команда.");
            }
        }
    }

    private void doctorMenu(Donor doctor) throws SQLException {
        boolean active = true;
        while (active) {
            System.out.println("\n=== Кабинет врача ===\n1. Профиль\n2. Все записи на донацию"
                    + "\n3. Все обследования\n4. Обработать обследование\n5. Партии крови\n6. Донации\n0. Выход");
            switch (read("Выберите действие: ")) {
                case "1" -> printDonor(doctor);
                case "2" -> printRequests(requestDao.findAll());
                case "3" -> printExaminations(examinationDao.findAll());
                case "4" -> processExamination();
                case "5" -> printBatches(batchDao.findAll());
                case "6" -> donationDao.findAll().forEach(System.out::println);
                case "0" -> active = false;
                default -> error("Неизвестная команда.");
            }
        }
    }

    private void bookExamination(Donor donor) throws SQLException {
        LocalDate date = date("Дата обследования (ДД.ММ.ГГГГ): ");
        DonationRequest request = requestDao.create(donor.id(), date, "created");
        examinationDao.create(request.id(), date);
        System.out.println("Запись и обследование созданы в БД. Номер заявки: " + request.id());
    }

    private void processExamination() throws SQLException {
        List<MedicalExamination> examinations = examinationDao.findAll();
        if (examinations.isEmpty()) {
            System.out.println("Обследований нет.");
            return;
        }
        printExaminations(examinations);
        MedicalExamination examination = examinations.get(index(examinations.size()));
        BigDecimal hemoglobin = new BigDecimal(required("Гемоглобин: "));
        String pressure = required("Давление: ");
        String conclusion = required("Заключение: ");
        String status = read("1 - допустить, 2 - отклонить: ");
        if ("1".equals(status)) status = "accepted";
        else if ("2".equals(status)) status = "rejected";
        else {
            error("Решение не изменено.");
            return;
        }
        examinationDao.updateResult(examination.id(), hemoglobin, pressure, conclusion, status);
        System.out.println("Результат обследования сохранён.");
    }

    private void printDonor(Donor donor) {
        System.out.printf("%s | %s | %s | вес: %d кг | группа: %s%n",
                donor.fullName(), donor.email(), donor.phone(), donor.weight(), donor.bloodGroup());
    }

    private void printRequests(List<DonationRequest> requests) {
        if (requests.isEmpty()) System.out.println("Записей нет.");
        requests.forEach(System.out::println);
    }

    private void printExaminations(List<MedicalExamination> examinations) {
        if (examinations.isEmpty()) System.out.println("Обследований нет.");
        examinations.forEach(System.out::println);
    }

    private void printBatches(List<BloodBatch> batches) {
        if (batches.isEmpty()) System.out.println("Партий крови нет.");
        batches.forEach(System.out::println);
    }

    private void donorStatistics(Donor donor) throws SQLException {
        System.out.println("Записей: " + requestDao.findByDonor(donor.id()).size());
        System.out.println("Обследований: " + examinationDao.findByDonor(donor.id()).size());
    }

    private BloodGroup chooseGroup(List<BloodGroup> groups) {
        for (int i = 0; i < groups.size(); i++) System.out.println((i + 1) + ". " + groups.get(i));
        return groups.get(index(groups.size()));
    }

    private String readRole() {
        while (true) {
            String value = read("Роль (1 - донор, 2 - врач): ");
            if ("1".equals(value)) return "donor";
            if ("2".equals(value)) return "doctor";
            error("Выберите 1 или 2.");
        }
    }

    private String readGender() {
        while (true) {
            String value = read("Пол (male/female/other): ").toLowerCase();
            if (value.equals("male") || value.equals("female") || value.equals("other")) return value;
            error("Допустимые значения: male, female, other.");
        }
    }

    private int positiveInt(String prompt) {
        while (true) {
            try {
                int value = Integer.parseInt(read(prompt));
                if (value > 0) return value;
            } catch (NumberFormatException ignored) {
                // Повторный запрос с понятным сообщением ниже.
            }
            error("Введите положительное целое число.");
        }
    }

    private int index(int size) {
        while (true) {
            try {
                int value = Integer.parseInt(read("Номер: ")) - 1;
                if (value >= 0 && value < size) return value;
            } catch (NumberFormatException ignored) {
                // Повторный запрос с понятным сообщением ниже.
            }
            error("Укажите номер из списка.");
        }
    }

    private LocalDate date(String prompt) {
        while (true) {
            try {
                return LocalDate.parse(read(prompt), DATE_FORMAT);
            } catch (DateTimeParseException exception) {
                error("Введите существующую дату в формате ДД.ММ.ГГГГ.");
            }
        }
    }

    private String required(String prompt) {
        while (true) {
            String value = read(prompt);
            if (!value.isBlank()) return value;
            error("Поле не может быть пустым.");
        }
    }

    private String read(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private void testConnection() {
        try (var connection = ru.mirea.project.db.DatabaseConnection.getConnection()) {
            System.out.println("Подключение к базе данных успешно.");
        } catch (SQLException exception) {
            databaseError(exception);
        }
    }

    private void databaseError(SQLException exception) {
        error("Ошибка базы данных: " + exception.getMessage());
    }

    private void error(String message) {
        System.err.println(message);
    }
}
