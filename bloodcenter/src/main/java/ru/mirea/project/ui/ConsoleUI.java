package ru.mirea.project.ui;

import ru.mirea.project.repository.BloodBatchRepository;
import ru.mirea.project.repository.BloodGroupRepository;
import ru.mirea.project.repository.DonationRepository;
import ru.mirea.project.repository.DonationRequestRepository;
import ru.mirea.project.repository.DonorRepository;
import ru.mirea.project.repository.MedicalExaminationRepository;
import ru.mirea.project.model.BloodBatch;
import ru.mirea.project.model.BloodGroup;
import ru.mirea.project.model.Donation;
import ru.mirea.project.model.DonationRequest;
import ru.mirea.project.model.Donor;
import ru.mirea.project.model.MedicalExamination;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.util.ExcelExporter;
import ru.mirea.project.util.TestDataSeeder;
import ru.mirea.project.service.DonationService;
import ru.mirea.project.service.DonorService;
import ru.mirea.project.service.MedicalExaminationService;
import ru.mirea.project.service.AscendingBloodBatchSorter;
import ru.mirea.project.service.BloodBatchSorter;
import ru.mirea.project.service.DescendingBloodBatchSorter;

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
    private final DonorRepository donorDao = new DonorRepository();
    private final BloodGroupRepository bloodGroupDao = new BloodGroupRepository();
    private final DonationRequestRepository requestDao = new DonationRequestRepository();
    private final MedicalExaminationRepository examinationDao = new MedicalExaminationRepository();
    private final BloodBatchRepository batchDao = new BloodBatchRepository();
    private final DonationRepository donationDao = new DonationRepository();
    private final DonorService donorService = new DonorService(donorDao, bloodGroupDao);
    private final DonationService donationService =
            new DonationService(requestDao, examinationDao, donationDao, batchDao);
    private final MedicalExaminationService examinationService =
            new MedicalExaminationService(examinationDao, requestDao, donorDao);
    public void start() {
        System.out.println("=== Центр донорства крови ===");
        try {
            bloodGroupDao.ensureDefaults();
            batchDao.ensureDefaults();
            batchDao.ensureRules();
            donorDao.ensureRules();
            donationDao.ensureRules();
        } catch (SQLException exception) {
            databaseError(exception);
            return;
        }
        boolean running = true;
        while (running) {
            System.out.println("\n1. Регистрация\n2. Вход\n3. Проверить подключение"
                    + "\n4. Внести тестовые данные\n0. Выход");
            switch (read("Выберите действие: ")) {
                case "1" -> register();
                case "2" -> login();
                case "3" -> testConnection();
                case "4" -> seedTestData();
                case "0" -> running = false;
                default -> error("Неизвестная команда.");
            }
        }
    }

    private void seedTestData() {
        try {
            bloodGroupDao.ensureDefaults();
            batchDao.ensureDefaults();
            boolean inserted = TestDataSeeder.seed();
            if (inserted) {
                System.out.println("Тестовые данные добавлены.");
                System.out.println("\nДанные для входа в тестовые аккаунты:");
                System.out.println("Донор с несколькими обследованиями и донациями:"
                        + "\n  Email: test.multi@example.com\n  Пароль: password");
                System.out.println("Донор с обследованием без донации:"
                        + "\n  Email: test.examination@example.com\n  Пароль: password");
                System.out.println("Донор с положительным обследованием и донацией:"
                        + "\n  Email: test.accepted@example.com\n  Пароль: password");
                System.out.println("Донор с отклонённым обследованием:"
                        + "\n  Email: test.rejected@example.com\n  Пароль: password");
                System.out.println("Врач:"
                        + "\n  Email: test.doctor@example.com\n  Пароль: password");
            } else {
                System.out.println("Тестовые данные уже существуют.");
                System.out.println("\nДанные для входа в тестовые аккаунты:");
                System.out.println("test.multi@example.com / password");
                System.out.println("test.examination@example.com / password");
                System.out.println("test.accepted@example.com / password");
                System.out.println("test.rejected@example.com / password");
                System.out.println("test.doctor@example.com / password");
            }
        } catch (SQLException exception) {
            databaseError(exception);
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
            int age = positiveInt("Сколько лет: ");
            String gender = readGender();
            int weight = positiveInt("Вес (кг): ");
            String email = email("Email: ");
            String password = password();
            bloodGroupDao.ensureDefaults();
            List<BloodGroup> groups = bloodGroupDao.findAll();
            if (groups.isEmpty()) {
                error("В таблице blood_group нет групп крови.");
                return;
            }
            BloodGroup group = chooseGroup(groups);
            Donor donor = donorService.register(name, age, role, gender, weight, email,
                    password, group.id());
            System.out.println("Регистрация завершена. Идентификатор: " + donor.id());
        } catch (BusinessException exception) {
            error(exception.getMessage());
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
            String password = required("Пароль: ");
            Donor donor = donorService.authenticate(email, password);
            if (donor == null) {
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
            System.out.println("\n=== Кабинет донора ===\n1. Профиль\n2. Изменить профиль"
                    + "\n3. Записаться на обследование\n4. Изменить запись на обследование"
                    + "\n5. Удалить запись на обследование\n6. Запись на донацию"
                    + "\n7. Мои обследования\n8. Мои записи на донацию"
                    + "\n9. Мои донации\n10. Статистика\n0. Выход");
            switch (read("Выберите действие: ")) {
                case "1" -> printDonor(donor);
                case "2" -> donor = updateProfile(donor);
                case "3" -> bookExamination(donor);
                case "4" -> updateDonorExamination(donor);
                case "5" -> deleteDonorExamination(donor);
                case "6" -> bookDonation(donor);
                case "7" -> donorExaminations(donor);
                case "8" -> printRequests(requestDao.findByDonor(donor.id()));
                case "9" -> donorDonations(donor);
                case "10" -> donorStatistics(donor);
                case "0" -> active = false;
                default -> error("Неизвестная команда.");
            }
        }
    }

    private void doctorMenu(Donor doctor) throws SQLException {
        boolean active = true;
        while (active) {
            System.out.println("\n=== Кабинет врача ===\n1. Профиль\n2. Все записи на донацию"
                    + "\n3. Все обследования\n4. Обработать обследование\n5. Обработать донацию"
                    + "\n6. Изменить запись на обследование\n7. Удалить запись на обследование"
                    + "\n8. Партии крови\n9. Донации\n10. Все доноры\n11. Статистика\n0. Выход");
            switch (read("Выберите действие: ")) {
                case "1" -> printDonor(doctor);
                case "2" -> printRequests(requestDao.findAll());
                case "3" -> printExaminations(examinationDao.findAll());
                case "4" -> processExamination();
                case "5" -> processDonation();
                case "6" -> updateDoctorExamination();
                case "7" -> deleteDoctorExamination();
                case "8" -> bloodBatchMenu();
                case "9" -> doctorDonations();
                case "10" -> printAllDonors();
                case "11" -> doctorStatistics();
                case "0" -> active = false;
                default -> error("Неизвестная команда.");
            }
        }
    }

    private void bookExamination(Donor donor) throws SQLException {
        LocalDate date = date("Дата обследования (ДД.ММ.ГГГГ): ");
        List<MedicalExamination> previousExaminations = examinationDao.findByDonor(donor.id());
        BigDecimal hemoglobin = previousExaminations.isEmpty()
                ? null
                : previousExaminations.get(previousExaminations.size() - 1).hemoglobin();
        if (hemoglobin == null) {
            hemoglobin = decimal("Гемоглобин (г/л): ");
        } else {
            System.out.println("Будет использован гемоглобин последнего обследования: "
                    + hemoglobin + " г/л.");
        }
        try {
            DonationRequest request = donationService.bookExamination(donor, date, hemoglobin);
            System.out.println("Запись и обследование созданы в БД. Номер заявки: " + request.id());
        } catch (BusinessException exception) {
            error(exception.getMessage());
        } catch (SQLException exception) {
            databaseError(exception);
        }
    }

    private Donor updateProfile(Donor donor) throws SQLException {
        int age = positiveInt("Новый возраст: ");
        int weight = positiveInt("Новый вес (кг): ");
        String gender = readGender();
        try {
            Donor updated = donorService.updateProfile(donor, age, gender, weight);
            System.out.println("Профиль обновлён.");
            return updated;
        } catch (BusinessException exception) {
            error(exception.getMessage());
            return donor;
        }
    }

    private void bookDonation(Donor donor) throws SQLException {
        List<Integer> usedExaminationIds = donationDao.findByDonor(donor.id()).stream()
                .map(Donation::examinationId)
                .toList();
        List<MedicalExamination> examinations = examinationDao.findByDonor(donor.id()).stream()
                .filter(examination -> "accepted".equalsIgnoreCase(examination.admissionStatus()))
                .filter(examination -> !usedExaminationIds.contains(examination.id()))
                .toList();
        if (examinations.isEmpty()) {
            error("Для записи нужна одна положительная медкомиссия без оформленной донации.");
            return;
        }
        System.out.println("\n=== Запись на донацию ===");
        System.out.println("Доступные положительные обследования:");
        printExaminations(examinations);
        LocalDate donationDate = date("Дата донации (ДД.ММ.ГГГГ): ");
        try {
            DonationRequest request = donationService.bookDonation(donor, donationDate);
            System.out.println("Запись на донацию создана. Номер заявки: " + request.id());
        } catch (BusinessException exception) {
            error(exception.getMessage());
        }
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
        try {
            examinationService.process(examination.id(), examination.requestId(), hemoglobin,
                    pressure, conclusion, status);
            System.out.println("Результат обследования сохранён.");
        } catch (BusinessException exception) {
            error(exception.getMessage());
        }
    }

    private void updateDonorExamination(Donor donor) throws SQLException {
        List<MedicalExamination> examinations = examinationDao.findByDonor(donor.id());
        if (examinations.isEmpty()) {
            error("Записей на обследование нет.");
            return;
        }
        MedicalExamination examination = selectExamination(examinations);
        LocalDate newDate = date("Новая дата обследования (ДД.ММ.ГГГГ): ");
        try {
            examinationService.updateDateForDonor(examination.id(), donor, newDate);
            System.out.println("Дата записи на обследование изменена.");
        } catch (BusinessException exception) {
            error(exception.getMessage());
        }
    }

    private void deleteDonorExamination(Donor donor) throws SQLException {
        List<MedicalExamination> examinations = examinationDao.findByDonor(donor.id());
        if (examinations.isEmpty()) {
            error("Записей на обследование нет.");
            return;
        }
        MedicalExamination examination = selectExamination(examinations);
        try {
            examinationService.deleteForDonor(examination.id(), donor);
            System.out.println("Запись на обследование удалена.");
        } catch (BusinessException exception) {
            error(exception.getMessage());
        } catch (SQLException exception) {
            error(exception.getMessage());
        }
    }

    private void updateDoctorExamination() throws SQLException {
        List<MedicalExamination> examinations = examinationDao.findAll();
        if (examinations.isEmpty()) {
            error("Записей на обследование нет.");
            return;
        }
        MedicalExamination examination = selectExamination(examinations);
        LocalDate newDate = date("Новая дата обследования (ДД.ММ.ГГГГ): ");
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
        try {
            examinationService.updateDateForDoctor(examination.id(), newDate);
            examinationService.updateResultForDoctor(examination.id(), hemoglobin, pressure,
                    conclusion, status);
            System.out.println("Запись и заключение обследования изменены.");
        } catch (BusinessException exception) {
            error(exception.getMessage());
        }
    }

    private void deleteDoctorExamination() throws SQLException {
        List<MedicalExamination> examinations = examinationDao.findAll();
        if (examinations.isEmpty()) {
            error("Записей на обследование нет.");
            return;
        }
        MedicalExamination examination = selectExamination(examinations);
        try {
            examinationService.deleteForDoctor(examination.id());
            System.out.println("Запись на обследование удалена.");
        } catch (BusinessException exception) {
            error(exception.getMessage());
        }
    }

    private MedicalExamination selectExamination(List<MedicalExamination> examinations) {
        System.out.println("\nВыберите запись на обследование:");
        for (int i = 0; i < examinations.size(); i++) {
            MedicalExamination examination = examinations.get(i);
            System.out.printf("%d. #%d | донор: %s | дата: %s | статус: %s%n",
                    i + 1, examination.id(), examination.donorName(),
                    examination.examinationDate().format(DATE_FORMAT),
                    admissionStatusLabel(examination.admissionStatus()));
        }
        return examinations.get(index(examinations.size()));
    }

    private void processDonation() throws SQLException {
        List<DonationRequest> requests = requestDao.findForProcessing();
        if (requests.isEmpty()) {
            error("Нет необработанных записей на донацию.");
            return;
        }
        System.out.println("\n=== Обработка донации ===");
        for (int i = 0; i < requests.size(); i++) {
            DonationRequest request = requests.get(i);
            System.out.printf("%d. #%d | донор: %s | дата: %s | статус: %s%n",
                    i + 1, request.id(), request.donorName(),
                    request.donationDate().format(DATE_FORMAT),
                    requestStatusLabel(request.status()));
        }
        DonationRequest request = requests.get(index(requests.size()));

        List<Integer> usedExaminationIds = donationDao.findByDonor(request.donorId()).stream()
                .map(Donation::examinationId)
                .toList();
        List<MedicalExamination> examinations = examinationDao.findByDonor(request.donorId()).stream()
                .filter(item -> "accepted".equalsIgnoreCase(item.admissionStatus()))
                .filter(item -> !usedExaminationIds.contains(item.id()))
                .toList();
        if (examinations.isEmpty()) {
            error("Для этой записи нет свободного положительного медицинского обследования.");
            return;
        }
        MedicalExamination examination = examinations.get(0);
        System.out.println("Используется положительное обследование #" + examination.id()
                + " донора " + examination.donorName() + ".");

        List<BloodBatch> batches = batchDao.findAvailableForRequest(request.id());
        if (batches.isEmpty()) {
            error("Нет доступной партии крови соответствующей группы.");
            return;
        }
        System.out.println("Выберите партию крови:");
        for (int i = 0; i < batches.size(); i++) {
            BloodBatch batch = batches.get(i);
            System.out.printf("%d. #%d | группа: %s | объём: %d мл | статус: %s%n",
                    i + 1, batch.id(), batch.bloodGroup(), batch.totalVolume(),
                    batch.status());
        }
        BloodBatch batch = batches.get(index(batches.size()));
        int volume = donationVolume(batch);
        String donationType = donationType();
        String result = donationResult();
        try {
            Donor donor = donorDao.findById(request.donorId());
            Donation donation = donationService.process(request, examination, batch, volume,
                    donationType, result, donor);
            System.out.println("Донация обработана. Заключение: " + donationResultLabel(donation.result())
                    + ". Номер записи: " + donation.id());
        } catch (BusinessException exception) {
            error(exception.getMessage());
        }
    }

    private int donationVolume(BloodBatch batch) {
        while (true) {
            int volume = positiveInt("Объём донации (мл, не более 450): ");
            if (volume <= 450 && volume <= batch.totalVolume()) return volume;
            error("Объём донации должен быть от 1 до 450 мл и не превышать остаток партии.");
        }
    }

    private void printAllDonors() throws SQLException {
        List<Donor> donors = donorDao.findAll();
        System.out.println("\n=== Все доноры и группы крови ===");
        if (donors.isEmpty()) {
            System.out.println("Доноров нет.");
            return;
        }
        for (int i = 0; i < donors.size(); i++) {
            Donor donor = donors.get(i);
            System.out.printf("%d. %s | возраст: %d | пол: %s | вес: %d кг | группа крови и резус: %s%n",
                    i + 1, donor.fullName(), donor.age(), genderLabel(donor.gender()),
                    donor.weight(), donor.bloodGroup());
        }
    }

    private String donationType() {
        while (true) {
            String value = read("Тип донации: 1 - цельная кровь, 2 - плазма, 3 - тромбоциты: ");
            if ("1".equals(value)) return "whole_blood";
            if ("2".equals(value)) return "plasma";
            if ("3".equals(value)) return "platelets";
            error("Выберите 1, 2 или 3.");
        }
    }

    private String donationResult() {
        while (true) {
            String value = read("Заключение: 1 - успешная, 2 - неуспешная: ");
            if ("1".equals(value)) return "successful";
            if ("2".equals(value)) return "unsuccessful";
            error("Выберите 1 или 2.");
        }
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
            System.out.println("\nМои донации: 1 - все, 2 - поиск по месяцу и году, 0 - назад");
            String choice = read("Выберите действие: ");
            if ("0".equals(choice)) return;
            if ("2".equals(choice)) {
                YearMonth selectedMonth = month();
                donations = donations.stream()
                        .filter(item -> YearMonth.from(item.donationDate()).equals(selectedMonth))
                        .toList();
                printDonations(donations, "Мои донации за " + selectedMonth.format(
                        DateTimeFormatter.ofPattern("MM.uuuu")));
            } else if ("1".equals(choice)) {
                printDonations(donations, "Мои донации");
            } else {
                error("Неизвестный вариант поиска.");
            }
        }

        private void doctorDonations() throws SQLException {
            List<Donation> donations = donationDao.findAll();
            System.out.println("\nПоиск донаций: 1 - все, 2 - по месяцу и году, 0 - назад");
            String choice = read("Выберите действие: ");
            if ("0".equals(choice)) return;
            if ("2".equals(choice)) {
                YearMonth selectedMonth = month();
                donations = donations.stream()
                        .filter(item -> YearMonth.from(item.donationDate()).equals(selectedMonth))
                        .toList();
                printDonations(donations, "Донации за " + selectedMonth.format(
                        DateTimeFormatter.ofPattern("MM.uuuu")));
            } else if (!"1".equals(choice)) {
                error("Неизвестный вариант поиска.");
                return;
            } else {
                printDonations(donations, "Все донации");
            }
        }

        private void printDonations(List<Donation> donations, String title) {
            System.out.println("\n=== " + title + " ===");
            if (donations.isEmpty()) {
                System.out.println("Донаций не найдено.");
                return;
            }
            for (int i = 0; i < donations.size(); i++) {
                Donation item = donations.get(i);
                System.out.printf("%d. Донация #%d | донор: %s | дата: %s | объём: %d мл | тип: %s | результат: %s | группа: %s%n",
                    i + 1, item.id(), item.donorName(), item.donationDate().format(DATE_FORMAT),
                    item.bloodVolume(), donationTypeLabel(item.donationType()),
                    donationResultLabel(item.result()), item.bloodGroup());
            }
        }

        private void bloodBatchMenu() throws SQLException {
            List<BloodBatch> batches = batchDao.findAll();
            System.out.println("\nСортировка партий крови: 1 - по объёму (возрастание),"
                    + " 2 - по объёму (убывание), 0 - без сортировки");
            String choice = read("Выберите вариант: ");
            BloodBatchSorter sorter = null;
            if ("1".equals(choice)) {
                sorter = new AscendingBloodBatchSorter();
            } else if ("2".equals(choice)) {
                sorter = new DescendingBloodBatchSorter();
            } else if (!"0".equals(choice)) {
                error("Неизвестный вариант сортировки.");
                return;
            }
            if (sorter != null) {
                batches = sorter.sort(batches);
            }
            printBatches(batches);
        }

    private void printDonor(Donor donor) {
        System.out.println("\n=== Профиль ===");
        System.out.println("ФИО: " + donor.fullName());
        System.out.println("Пол: " + genderLabel(donor.gender()));
        System.out.println("Возраст: " + donor.age() + " лет");
        System.out.println("Email: " + donor.email());
        System.out.println("Вес: " + donor.weight() + " кг");
        System.out.println("Группа крови: " + donor.bloodGroup());
    }

    private void printRequests(List<DonationRequest> requests) {
        System.out.println("\n=== Мои записи на донацию ===");
        if (requests.isEmpty()) {
            System.out.println("Записей на донацию пока нет.");
            return;
        }
        for (int i = 0; i < requests.size(); i++) {
            DonationRequest request = requests.get(i);
            System.out.printf("%d. Заявка #%d | дата: %s | статус: %s%n",
                    i + 1, request.id(), request.donationDate().format(DATE_FORMAT),
                    requestStatusLabel(request.status()));
        }
    }

    private void printExaminations(List<MedicalExamination> examinations) {
        System.out.println("\n=== Мои обследования ===");
        if (examinations.isEmpty()) {
            System.out.println("Медицинских обследований пока нет.");
            return;
        }
        for (int i = 0; i < examinations.size(); i++) {
            MedicalExamination examination = examinations.get(i);
            System.out.printf("%d. Обследование #%d | донор: %s | дата: %s | гемоглобин: %s | давление: %s | статус: %s%n",
                    i + 1, examination.id(), examination.donorName(),
                    examination.examinationDate().format(DATE_FORMAT),
                    valueOrDash(examination.hemoglobin()), valueOrDash(examination.bloodPressure()),
                    admissionStatusLabel(examination.admissionStatus()));
            System.out.println("  Заключение: " + valueOrDash(examination.conclusion()));
        }
    }

    private String genderLabel(String gender) {
        return switch (gender.toLowerCase(Locale.ROOT)) {
            case "male" -> "муж.";
            case "female" -> "жен.";
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
        System.out.println("\n=== Партии крови ===");
        if (batches.isEmpty()) {
            System.out.println("Партий крови нет.");
            return;
        }
        System.out.printf("%-4s %-14s %-8s %-14s %-14s %-12s %-12s%n",
                "ID", "Номер", "Группа", "Заготовка", "Годна до", "Объём", "Статус");
        System.out.println("-".repeat(82));
        for (BloodBatch batch : batches) {
            System.out.printf("%-4d %-14s %-8s %-14s %-14s %-12s %-12s%n",
                    batch.id(), batch.batchNumber(), batch.bloodGroup(),
                    batch.preparationDate().format(DATE_FORMAT),
                    batch.expirationDate().format(DATE_FORMAT),
                    batch.totalVolume() + " мл", batchStatusLabel(batch.status()));
        }
    }

    private String batchStatusLabel(String status) {
        return switch (status.toLowerCase(Locale.ROOT)) {
            case "available" -> "доступна";
            case "reserved" -> "зарезервирована";
            case "used" -> "использована";
            case "expired" -> "просрочена";
            case "disposed" -> "утилизирована";
            default -> status;
        };
    }

    private void donorStatistics(Donor donor) throws SQLException {
        List<DonationRequest> requests = requestDao.findByDonor(donor.id());
        List<MedicalExamination> examinations = examinationDao.findByDonor(donor.id());
        List<Donation> donations = donationDao.findByDonor(donor.id());
        List<List<String>> report = new ArrayList<>();
        report.add(List.of("Статистика донора"));
        report.add(List.of("ФИО", donor.fullName()));
        report.add(List.of("Пол", genderLabel(donor.gender())));
        report.add(List.of("Возраст", donor.age() + " лет"));
        report.add(List.of("Email", donor.email()));
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
            var exportedFile = ExcelExporter.export(report, fileName, format);
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
            String value = read("Пол (муж./жен.): ").toLowerCase(Locale.ROOT);
            if (value.equals("муж") || value.equals("муж." ) || value.equals("male")) return "male";
            if (value.equals("жен") || value.equals("жен.") || value.equals("female")) return "female";
            error("Введите «муж.» или «жен.».");
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

    private BigDecimal decimal(String prompt) {
        while (true) {
            try {
                return new BigDecimal(read(prompt).replace(',', '.'));
            } catch (NumberFormatException exception) {
                error("Введите число, например 145 или 145.5.");
            }
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
        try (var connection = ru.mirea.project.util.DatabaseManager.getConnection()) {
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
