package ru.mirea.project.ui;

import ru.mirea.project.dao.BloodBatchDao;
import ru.mirea.project.dao.BloodGroupDao;
import ru.mirea.project.dao.DonationDao;
import ru.mirea.project.dao.DonationRequestDao;
import ru.mirea.project.dao.DonorDao;
import ru.mirea.project.dao.MedicalExaminationDao;
import ru.mirea.project.model.BloodBatch;
import ru.mirea.project.model.BloodGroup;
import ru.mirea.project.model.Donation;
import ru.mirea.project.model.DonationRequest;
import ru.mirea.project.model.Donor;
import ru.mirea.project.model.MedicalExamination;
import ru.mirea.project.security.PasswordHasher;

import java.math.BigDecimal;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;

public class ConsoleUI {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.uuuu");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
                    + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "^[\\p{L}]+(?:[-'][\\p{L}]+)*$", Pattern.UNICODE_CHARACTER_CLASS);
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
            String surname = personName("Фамилия: ");
            String firstName = personName("Имя: ");
            String patronymic = personName("Отчество: ");
            String name = surname + " " + firstName + " " + patronymic;
            LocalDate birthDate = date("Дата рождения (ДД.ММ.ГГГГ): ");
            String gender = readGender();
            int weight = positiveInt("Вес (кг): ");
            String email = email("Email: ");
            if (donorDao.findByEmail(email) != null) {
                error("Пользователь с таким email уже зарегистрирован.");
                return;
            }
            String phone = phone("Телефон: ");
            if (donorDao.findByPhone(phone) != null) {
                error("Пользователь с таким телефоном уже зарегистрирован.");
                return;
            }
            String password = password();
            bloodGroupDao.ensureDefaults();
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
            if ("23505".equals(exception.getSQLState())) {
                error("Email или телефон уже используются.");
            } else {
                databaseError(exception);
            }
        }
    }

    private void login() {
        try {
            String email = email("Email: ");
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
                    + "\n3. Запись на донацию\n4. Мои обследования\n5. Мои записи на донацию"
                    + "\n6. Мои донации\n7. Статистика\n0. Выход");
            switch (read("Выберите действие: ")) {
                case "1" -> printDonor(donor);
                case "2" -> bookExamination(donor);
                case "3" -> bookDonation(donor);
                case "4" -> donorExaminations(donor);
                case "5" -> printRequests(requestDao.findByDonor(donor.id()));
                case "6" -> donorDonations(donor);
                case "7" -> donorStatistics(donor);
                case "0" -> active = false;
                default -> error("Неизвестная команда.");
            }
        }
    }

    private void doctorMenu(Donor doctor) throws SQLException {
        boolean active = true;
        while (active) {
            System.out.println("\n=== Кабинет врача ===\n1. Профиль\n2. Все записи на донацию"
                    + "\n3. Все обследования\n4. Обработать обследование\n5. Партии крови\n6. Донации"
                    + "\n7. Статистика\n0. Выход");
            switch (read("Выберите действие: ")) {
                case "1" -> printDonor(doctor);
                case "2" -> printRequests(requestDao.findAll());
                case "3" -> printExaminations(examinationDao.findAll());
                case "4" -> processExamination();
                case "5" -> bloodBatchMenu();
                case "6" -> doctorDonations();
                case "7" -> doctorStatistics();
                case "0" -> active = false;
                default -> error("Неизвестная команда.");
            }
        }
    }

    private void bookExamination(Donor donor) throws SQLException {
        LocalDate date = date("Дата обследования (ДД.ММ.ГГГГ): ");
        if (date.isBefore(donor.birthDate())) {
            error("Дата обследования не может быть раньше даты рождения.");
            return;
        }
        DonationRequest request = requestDao.create(donor.id(), date, "created");
        examinationDao.create(request.id(), date);
        System.out.println("Запись и обследование созданы в БД. Номер заявки: " + request.id());
    }

    private void bookDonation(Donor donor) throws SQLException {
        List<MedicalExamination> examinations = examinationDao.findByDonor(donor.id()).stream()
                .filter(examination -> "accepted".equalsIgnoreCase(examination.admissionStatus()))
                .toList();
        if (examinations.isEmpty()) {
            error("Запись на донацию доступна только после положительного медицинского обследования.");
            return;
        }
        System.out.println("\n=== Запись на донацию ===");
        System.out.println("Доступные положительные обследования:");
        printExaminations(examinations);
        LocalDate examinationDate = examinations.stream()
                .map(MedicalExamination::examinationDate)
                .max(LocalDate::compareTo)
                .orElseThrow();
        LocalDate donationDate = date("Дата донации (ДД.ММ.ГГГГ): ");
        if (donationDate.isBefore(donor.birthDate())) {
            error("Дата донации не может быть раньше даты рождения.");
            return;
        }
        if (donationDate.isBefore(examinationDate)) {
            error("Дата донации не может быть раньше даты медицинского обследования.");
            return;
        }
        DonationRequest request = requestDao.create(donor.id(), donationDate, "created");
        System.out.println("Запись на донацию создана. Номер заявки: " + request.id());
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

    private void donorExaminations(Donor donor) throws SQLException {
            List<MedicalExamination> all = examinationDao.findByDonor(donor.id());
            System.out.println("\nФильтр обследований: 1 - все, 2 - по статусу, 3 - по месяцу, 0 - назад");
            switch (read("Выберите фильтр: ")) {
                case "1" -> printExaminations(all);
                case "2" -> {
                    String status = examinationStatus();
                    printExaminations(all.stream()
                            .filter(item -> item.admissionStatus().equalsIgnoreCase(status))
                            .toList());
                }
                case "3" -> {
                    YearMonth month = month();
                    printExaminations(all.stream()
                            .filter(item -> YearMonth.from(item.examinationDate()).equals(month))
                            .toList());
                }
                case "0" -> { }
                default -> error("Неизвестный фильтр.");
            }
        }

        private String examinationStatus() {
            while (true) {
                String value = read("Статус: 1 - ожидает решения, 2 - допущен, 3 - не допущен: ");
                if ("1".equals(value)) return "pending";
                if ("2".equals(value)) return "accepted";
                if ("3".equals(value)) return "rejected";
                error("Выберите 1, 2 или 3.");
            }
        }

        private YearMonth month() {
            while (true) {
                try {
                    return YearMonth.parse(read("Месяц (ММ.ГГГГ): "),
                            DateTimeFormatter.ofPattern("MM.uuuu"));
                } catch (DateTimeParseException exception) {
                    error("Введите месяц в формате ММ.ГГГГ.");
                }
            }
        }

        private void donorDonations(Donor donor) throws SQLException {
            List<Donation> donations = donationDao.findByDonor(donor.id());
            LocalDate date = date("Дата донации (ДД.ММ.ГГГГ): ");
            printDonations(donations.stream()
                    .filter(item -> item.donationDate().equals(date))
                    .toList(), "Мои донации за " + date.format(DATE_FORMAT));
        }

        private void doctorDonations() throws SQLException {
            List<Donation> donations = donationDao.findAll();
            System.out.println("\nПоиск донаций: 1 - все, 2 - по ФИО донора, 0 - назад");
            String choice = read("Выберите действие: ");
            if ("0".equals(choice)) return;
            if ("2".equals(choice)) {
                String query = required("Введите ФИО или часть ФИО: ").toLowerCase(Locale.ROOT);
                donations = donations.stream()
                        .filter(item -> item.donorName().toLowerCase(Locale.ROOT).contains(query))
                        .toList();
            } else if (!"1".equals(choice)) {
                error("Неизвестный вариант поиска.");
                return;
            }
            printDonations(donations, "Донации");
        }

        private void printDonations(List<Donation> donations, String title) {
            System.out.println("\n=== " + title + " ===");
            if (donations.isEmpty()) {
                System.out.println("Донаций не найдено.");
                return;
            }
            donations.forEach(item -> System.out.printf(
                    "#%d | донор: %s | дата: %s | объём: %d мл | тип: %s | результат: %s | группа: %s%n",
                    item.id(), item.donorName(), item.donationDate().format(DATE_FORMAT),
                    item.bloodVolume(), donationTypeLabel(item.donationType()),
                    donationResultLabel(item.result()), item.bloodGroup()));
        }

        private void bloodBatchMenu() throws SQLException {
            List<BloodBatch> batches = batchDao.findAll();
            System.out.println("\nСортировка партий крови: 1 - по объёму (возрастание),"
                    + " 2 - по объёму (убывание), 0 - без сортировки");
            String choice = read("Выберите вариант: ");
            if ("1".equals(choice)) {
                batches = batches.stream()
                        .sorted(Comparator.comparingInt(BloodBatch::totalVolume))
                        .toList();
            } else if ("2".equals(choice)) {
                batches = batches.stream()
                        .sorted(Comparator.comparingInt(BloodBatch::totalVolume).reversed())
                        .toList();
            } else if (!"0".equals(choice)) {
                error("Неизвестный вариант сортировки.");
                return;
            }
            printBatches(batches);
        }

    private void printDonor(Donor donor) {
        System.out.println("\n=== Профиль ===");
        System.out.println("ФИО: " + donor.fullName());
        System.out.println("Пол: " + genderLabel(donor.gender()));
        System.out.println("Дата рождения: " + donor.birthDate().format(DATE_FORMAT));
        System.out.println("Email: " + donor.email());
        System.out.println("Телефон: " + donor.phone());
        System.out.println("Вес: " + donor.weight() + " кг");
        System.out.println("Группа крови: " + donor.bloodGroup());
    }

    private void printRequests(List<DonationRequest> requests) {
        System.out.println("\n=== Мои записи на донацию ===");
        if (requests.isEmpty()) {
            System.out.println("Записей на донацию пока нет.");
            return;
        }
        for (DonationRequest request : requests) {
            System.out.printf("#%d | дата: %s | статус: %s%n",
                    request.id(), request.donationDate().format(DATE_FORMAT),
                    requestStatusLabel(request.status()));
        }
    }

    private void printExaminations(List<MedicalExamination> examinations) {
        System.out.println("\n=== Мои обследования ===");
        if (examinations.isEmpty()) {
            System.out.println("Медицинских обследований пока нет.");
            return;
        }
        for (MedicalExamination examination : examinations) {
            System.out.printf("#%d | дата: %s | гемоглобин: %s | давление: %s | статус: %s%n",
                    examination.id(), examination.examinationDate().format(DATE_FORMAT),
                    valueOrDash(examination.hemoglobin()), valueOrDash(examination.bloodPressure()),
                    admissionStatusLabel(examination.admissionStatus()));
            System.out.println("  Заключение: " + valueOrDash(examination.conclusion()));
        }
    }

    private String genderLabel(String gender) {
        return switch (gender.toLowerCase(Locale.ROOT)) {
            case "male" -> "мужской";
            case "female" -> "женский";
            default -> gender;
        };
    }

    private String requestStatusLabel(String status) {
        return switch (status.toLowerCase(Locale.ROOT)) {
            case "created" -> "создана";
            case "confirmed" -> "подтверждена";
            case "completed" -> "завершена";
            case "cancelled" -> "отменена";
            default -> status;
        };
    }

    private String admissionStatusLabel(String status) {
        return switch (status.toLowerCase(Locale.ROOT)) {
            case "pending" -> "ожидает решения";
            case "accepted" -> "допущен";
            case "rejected" -> "не допущен";
            default -> status;
        };
    }

    private String valueOrDash(Object value) {
        return value == null ? "—" : value.toString();
    }

    private void printBatches(List<BloodBatch> batches) {
        if (batches.isEmpty()) System.out.println("Партий крови нет.");
        batches.forEach(System.out::println);
    }

    private void donorStatistics(Donor donor) throws SQLException {
        List<DonationRequest> requests = requestDao.findByDonor(donor.id());
        List<MedicalExamination> examinations = examinationDao.findByDonor(donor.id());
        List<Donation> donations = donationDao.findByDonor(donor.id());
        List<List<String>> report = new ArrayList<>();
        report.add(List.of("Статистика донора"));
        report.add(List.of("ФИО", donor.fullName()));
        report.add(List.of("Пол", genderLabel(donor.gender())));
        report.add(List.of("Дата рождения", donor.birthDate().format(DATE_FORMAT)));
        report.add(List.of("Email", donor.email()));
        report.add(List.of("Телефон", donor.phone()));
        report.add(List.of("Группа крови", donor.bloodGroup()));
        report.add(List.of("Количество записей на донацию", String.valueOf(requests.size())));
        report.add(List.of("Количество обследований", String.valueOf(examinations.size())));
        report.add(List.of("Количество фактических донаций", String.valueOf(donations.size())));
        report.add(List.of("Объём сданной крови, мл",
                String.valueOf(donations.stream().mapToInt(Donation::bloodVolume).sum())));
        report.add(List.of());
        report.add(List.of("Обследования"));
        report.add(List.of("ID", "Дата", "Гемоглобин", "Давление", "Статус", "Заключение"));
        for (MedicalExamination examination : examinations) {
            report.add(List.of(String.valueOf(examination.id()),
                    examination.examinationDate().format(DATE_FORMAT),
                    valueOrDash(examination.hemoglobin()), valueOrDash(examination.bloodPressure()),
                    admissionStatusLabel(examination.admissionStatus()),
                    valueOrDash(examination.conclusion())));
        }
        report.add(List.of());
        report.add(List.of("Записи на донацию"));
        report.add(List.of("ID", "Дата", "Статус"));
        for (DonationRequest request : requests) {
            report.add(List.of(String.valueOf(request.id()),
                    request.donationDate().format(DATE_FORMAT), requestStatusLabel(request.status())));
        }
        report.add(List.of());
        report.add(List.of("Фактические донации"));
        report.add(List.of("ID", "Дата", "Объём, мл", "Тип", "Результат", "Группа крови"));
        for (Donation donation : donations) {
            report.add(List.of(String.valueOf(donation.id()),
                    donation.donationDate().format(DATE_FORMAT), String.valueOf(donation.bloodVolume()),
                    donationTypeLabel(donation.donationType()), donationResultLabel(donation.result()),
                    donation.bloodGroup()));
        }
        printStatistics(report);
        exportStatistics(report, "donor_" + donor.id());
    }

    private void doctorStatistics() throws SQLException {
        List<DonationRequest> requests = requestDao.findAll();
        List<MedicalExamination> examinations = examinationDao.findAll();
        List<Donation> donations = donationDao.findAll();
        List<List<String>> report = new ArrayList<>();
        report.add(List.of("Общая статистика центра донорства"));
        report.add(List.of("Количество записей на донацию", String.valueOf(requests.size())));
        report.add(List.of("Количество обследований", String.valueOf(examinations.size())));
        report.add(List.of("Количество допущенных", String.valueOf(examinations.stream()
                .filter(e -> "accepted".equalsIgnoreCase(e.admissionStatus())).count())));
        report.add(List.of("Количество отклонённых", String.valueOf(examinations.stream()
                .filter(e -> "rejected".equalsIgnoreCase(e.admissionStatus())).count())));
        report.add(List.of("Ожидают решения", String.valueOf(examinations.stream()
                .filter(e -> "pending".equalsIgnoreCase(e.admissionStatus())).count())));
        report.add(List.of("Количество фактических донаций", String.valueOf(donations.size())));
        report.add(List.of("Общий объём крови, мл",
                String.valueOf(donations.stream().mapToInt(Donation::bloodVolume).sum())));
        report.add(List.of());
        report.add(List.of("Обследования"));
        report.add(List.of("ID", "Донор", "Дата", "Гемоглобин", "Давление", "Статус", "Заключение"));
        for (MedicalExamination examination : examinations) {
            report.add(List.of(String.valueOf(examination.id()), examination.donorName(),
                    examination.examinationDate().format(DATE_FORMAT),
                    valueOrDash(examination.hemoglobin()), valueOrDash(examination.bloodPressure()),
                    admissionStatusLabel(examination.admissionStatus()),
                    valueOrDash(examination.conclusion())));
        }
        report.add(List.of());
        report.add(List.of("Записи на донацию"));
        report.add(List.of("ID", "Донор", "Дата", "Статус"));
        for (DonationRequest request : requests) {
            report.add(List.of(String.valueOf(request.id()), request.donorName(),
                    request.donationDate().format(DATE_FORMAT), requestStatusLabel(request.status())));
        }
        report.add(List.of());
        report.add(List.of("Фактические донации"));
        report.add(List.of("ID", "Донор", "Дата", "Объём, мл", "Тип", "Результат", "Группа крови"));
        for (Donation donation : donations) {
            report.add(List.of(String.valueOf(donation.id()), donation.donorName(),
                    donation.donationDate().format(DATE_FORMAT), String.valueOf(donation.bloodVolume()),
                    donationTypeLabel(donation.donationType()), donationResultLabel(donation.result()),
                    donation.bloodGroup()));
        }
        printStatistics(report);
        exportStatistics(report, "doctor_statistics");
    }

    private void printStatistics(List<List<String>> report) {
        System.out.println("\n=== Подробная статистика ===");
        for (List<String> row : report) {
            if (row.isEmpty()) {
                System.out.println();
            } else {
                System.out.println(String.join(" | ", row));
            }
        }
    }

    private void exportStatistics(List<List<String>> report, String fileName) {
        String format = read("Экспорт статистики: 1 - CSV, 2 - Excel (.xlsx), 0 - не экспортировать: ");
        if ("0".equals(format)) return;
        if (!"1".equals(format) && !"2".equals(format)) {
            error("Выберите 1, 2 или 0.");
            return;
        }
        try {
            var exportedFile = StatisticsExporter.export(report, fileName, format);
            if (exportedFile == null) {
                System.out.println("Сохранение отменено.");
            } else {
                System.out.println("Файл сохранён: " + exportedFile.toAbsolutePath());
            }
        } catch (IOException exception) {
            error("Не удалось сохранить статистику: " + exception.getMessage());
        }
    }

    private String donationTypeLabel(String type) {
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "whole_blood" -> "цельная кровь";
            case "plasma" -> "плазма";
            case "platelets" -> "тромбоциты";
            default -> type;
        };
    }

    private String donationResultLabel(String result) {
        return switch (result.toLowerCase(Locale.ROOT)) {
            case "successful" -> "успешная";
            case "unsuccessful" -> "неуспешная";
            default -> result;
        };
    }

    private BloodGroup chooseGroup(List<BloodGroup> groups) {
        System.out.println("Доступные группы крови:");
        groups.forEach(group -> System.out.println("- " + group));
        while (true) {
            String bloodType = readBloodType();
            String rhFactor = readRhFactor();
            var selectedGroup = groups.stream()
                    .filter(group -> group.bloodType().equals(bloodType)
                            && group.rhFactor().equals(rhFactor))
                    .findFirst();
            if (selectedGroup.isPresent()) return selectedGroup.get();
            error("Такая группа крови отсутствует в таблице blood_group. Выберите другую.");
        }
    }

    private String readBloodType() {
        while (true) {
            String value = read("Группа крови (0, A, B или AB): ").toUpperCase(Locale.ROOT);
            if (value.equals("0") || value.equals("A") || value.equals("B") || value.equals("AB")) {
                return value;
            }
            error("Укажите группу крови: 0, A, B или AB.");
        }
    }

    private String readRhFactor() {
        while (true) {
            String value = read("Резус-фактор (+ или -): ");
            if (value.equals("+") || value.equals("-")) return value;
            error("Резус-фактор должен быть указан как + или -.");
        }
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
            String value = read("Пол (male/female): ").toLowerCase(Locale.ROOT);
            if (value.equals("male") || value.equals("female")) return value;
            error("Допустимые значения: male или female.");
        }
    }

    private String personName(String prompt) {
        while (true) {
            String value = read(prompt);
            if (NAME_PATTERN.matcher(value).matches()) return value;
            error("Введите только буквы; допускается дефис.");
        }
    }

    private String email(String prompt) {
        while (true) {
            String value = read(prompt).toLowerCase(Locale.ROOT);
            if (EMAIL_PATTERN.matcher(value).matches()) return value;
            error("Введите корректный email, например user@example.com.");
        }
    }

    private String phone(String prompt) {
        while (true) {
            String value = read(prompt);
            String digits = value.replaceAll("[\\s()\\-]", "");
            if (digits.matches("8\\d{10}")) return "+7" + digits.substring(1);
            if (digits.matches("\\+7\\d{10}")) return digits;
            error("Введите телефон в формате +79991234567 или 89991234567.");
        }
    }

    private String password() {
        while (true) {
            String value = required("Пароль: ");
            if (value.length() >= 6) return value;
            error("Пароль должен содержать не менее 6 символов.");
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
