package ru.mirea.project.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ConsoleUI {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String PHONE_PATTERN = "^\\+?[0-9 ()-]+$";

    private final Scanner scanner;
    private final List<User> users;
    private final List<MedicalExamination> examinations;
    private final List<BloodRequest> bloodRequests;
    private final List<DonationAppointment> appointments;
    private final List<Notification> notifications;
    private final List<MedicalInstitution> institutions;
    private final List<Task> donorTasks;
    private final List<Task> doctorTasks;
    private final Map<User, MedicalInstitution> institutionsByDoctor;
    private boolean running;

    public ConsoleUI() {
        scanner = new Scanner(System.in);
        users = new ArrayList<>();
        examinations = new ArrayList<>();
        bloodRequests = new ArrayList<>();
        appointments = new ArrayList<>();
        notifications = new ArrayList<>();
        institutions = new ArrayList<>();
        donorTasks = new ArrayList<>();
        doctorTasks = new ArrayList<>();
        institutionsByDoctor = new HashMap<>();
        running = true;
    }

    public void start() {
        printWelcome();
        while (running) {
            printMainMenu();
            switch (readLine("Выберите действие: ")) {
                case "1" -> register();
                case "2" -> login();
                case "0" -> exit();
                default -> printError("Неизвестная команда.");
            }
        }
        scanner.close();
    }

    private void register() {
        System.out.println("\n=== Регистрация ===");
        Role role = chooseRole();
        if (role == null) {
            return;
        }

        String name = readRequired("ФИО / название клиники: ");
        String email = readEmail("Email: ");
        if (findUser(email) != null) {
            printError("Пользователь с таким email уже зарегистрирован.");
            return;
        }

        String password = readRequired("Пароль: ");
        String details;
        MedicalInstitution institution = null;
        if (role == Role.DONOR) {
            String birthDate = readDate("Дата рождения (ДД.ММ.ГГГГ): ");
            String phone = readPhone("Телефон: ");
            details = String.format("Дата рождения: %s, телефон: %s", birthDate, phone);
        } else {
            String address = readRequired("Адрес клиники: ");
            String phone = readPhone("Телефон клиники: ");
            details = String.format("Адрес: %s, телефон: %s", address, phone);
            institution = new MedicalInstitution(name, address, phone);
            institutions.add(institution);
            doctorTasks.add(new Task("Проверить новые записи на медобследование"));
        }

        User registeredUser = new User(name, email, password, role, details);
        users.add(registeredUser);
        if (institution != null) {
            institutionsByDoctor.put(registeredUser, institution);
        }
        System.out.println("Регистрация завершена. Теперь можно войти в систему.");
    }

    private Role chooseRole() {
        System.out.println("1. Донор");
        System.out.println("2. Врач");
        System.out.println("0. Назад");
        return switch (readLine("Выберите роль: ")) {
            case "1" -> Role.DONOR;
            case "2" -> Role.DOCTOR;
            case "0" -> null;
            default -> {
                printError("Неизвестная роль.");
                yield null;
            }
        };
    }

    private void login() {
        System.out.println("\n=== Вход ===");
        String email = readEmail("Email: ");
        String password = readRequired("Пароль: ");
        User user = findUser(email);

        if (user == null || !user.getPassword().equals(password)) {
            printError("Неверный email или пароль.");
            return;
        }

        System.out.printf("Добро пожаловать, %s! Роль: %s.%n", user.getName(), user.getRole().getTitle());
        if (user.getRole() == Role.DONOR) {
            donorMenu(user);
        } else {
            doctorMenu(user);
        }
    }

    private void donorMenu(User user) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n=== Кабинет донора ===");
            System.out.println("1. Мой профиль");
            System.out.println("2. Записаться на медобследование");
            System.out.println("3. Мои медобследования");
            System.out.println("4. Записаться на донацию");
            System.out.println("5. Мои записи на донацию");
            System.out.println("6. Статистика");
            System.out.println("7. Уведомления");
            System.out.println("8. Мои задачи");
            System.out.println("0. Выйти из аккаунта");
            switch (readLine("Выберите действие: ")) {
                case "1" -> showProfile(user);
                case "2" -> bookExamination(user);
                case "3" -> showDonorExaminations(user);
                case "4" -> makeAppointment(user);
                case "5" -> showDonorAppointments(user);
                case "6" -> showDonorStatistics(user);
                case "7" -> showDonorNotifications(user);
                case "8" -> showTasks(donorTasks, "Задачи донора");
                case "0" -> loggedIn = false;
                default -> printError("Неизвестная команда.");
            }
        }
    }

    private void doctorMenu(User user) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n=== Кабинет врача ===");
            System.out.println("1. Профиль клиники");
            System.out.println("2. Создать заявку на кровь");
            System.out.println("3. Просмотреть заявки на кровь");
            System.out.println("4. Медобследования доноров");
            System.out.println("5. Обработать результат обследования");
            System.out.println("6. Статистика");
            System.out.println("7. Мои задачи");
            System.out.println("0. Выйти из аккаунта");
            switch (readLine("Выберите действие: ")) {
                case "1" -> showProfile(user);
                case "2" -> createBloodRequest(user);
                case "3" -> showBloodRequests(user);
                case "4" -> showAllExaminations();
                case "5" -> processExamination();
                case "6" -> showAdminStatistics(user);
                case "7" -> showTasks(doctorTasks, "Задачи врача");
                case "0" -> loggedIn = false;
                default -> printError("Неизвестная команда.");
            }
        }
    }

    private void showProfile(User user) {
        System.out.println("\n--- Профиль ---");
        System.out.println("Имя: " + user.getName());
        System.out.println("Email: " + user.getEmail());
        System.out.println("Роль: " + user.getRole().getTitle());
        System.out.println(user.getDetails());
    }

    private void makeAppointment(User user) {
        System.out.println("\n=== Запись на донацию ===");
        if (findApprovedExamination(user) == null) {
            printError("Сначала пройдите медобследование и получите одобрение администратора.");
            return;
        }
        List<MedicalInstitution> availableInstitutions = institutions.stream()
                .filter(this::hasBloodRequest)
                .toList();
        if (availableInstitutions.isEmpty()) {
            printError("Нельзя записаться на донацию: ни у одного медицинского института нет заявки на кровь.");
            return;
        }
        System.out.println("Медицинские институты с заявками на кровь:");
        for (int index = 0; index < availableInstitutions.size(); index++) {
            System.out.println((index + 1) + ". " + availableInstitutions.get(index));
        }
        int institutionIndex = readIndex("Выберите медицинский институт: ", availableInstitutions.size());
        if (institutionIndex < 0) {
            return;
        }
        MedicalInstitution institution = availableInstitutions.get(institutionIndex);
        String date = readDate("Дата (ДД.ММ.ГГГГ): ");
        String time = readRequired("Время: ");
        appointments.add(new DonationAppointment(user, date, time, institution.getName()));
        System.out.printf("Запись создана для %s: %s в %s, %s.%n",
                user.getName(), date, time, institution.getName());
    }

    private void bookExamination(User donor) {
        System.out.println("\n=== Запись на медобследование ===");
        if (institutions.isEmpty()) {
            printError("Нет доступных клиник. Сначала зарегистрируйте врача.");
            return;
        }
        System.out.println("Доступные клиники:");
        for (int index = 0; index < institutions.size(); index++) {
            System.out.println((index + 1) + ". " + institutions.get(index));
        }
        int institutionIndex = readIndex("Выберите клинику: ", institutions.size());
        if (institutionIndex < 0) {
            return;
        }
        String date = readDate("Дата обследования (ДД.ММ.ГГГГ): ");
        MedicalInstitution institution = institutions.get(institutionIndex);
        examinations.add(new MedicalExamination(donor, institution, date));
        donorTasks.add(new Task("Пройти медобследование в клинике " + institution.getName()));
        doctorTasks.add(new Task("Обработать запись донора " + donor.getName()));
        System.out.println("Запись создана в клинике: " + institution.getName());
        System.out.println("Ожидайте внесения результата обследования.");
    }

    private void showDonorExaminations(User donor) {
        System.out.println("\n=== Мои медобследования ===");
        boolean found = false;
        for (MedicalExamination examination : examinations) {
            if (examination.getDonor().equals(donor)) {
                printExamination(examination);
                found = true;
            }
        }
        if (!found) {
            System.out.println("Записей на медобследование пока нет.");
        }
    }

    private void showDonorAppointments(User donor) {
        System.out.println("\n=== Мои записи на донацию ===");
        boolean found = false;
        for (DonationAppointment appointment : appointments) {
            if (appointment.getDonor().equals(donor)) {
                System.out.println(appointment);
                found = true;
            }
        }
        if (!found) {
            System.out.println("Записей на донацию пока нет.");
        }
    }

    private void createBloodRequest(User clinic) {
        System.out.println("\n=== Новая заявка на кровь ===");
        MedicalInstitution institution = institutionsByDoctor.get(clinic);
        if (institution == null) {
            printError("За врачом не закреплён медицинский институт.");
            return;
        }
        String bloodGroup = readRequired("Нужная группа крови: ");
        String quantity = readRequired("Количество (мл или единиц): ");
        String urgency = readRequired("Срочность: ");
        String date = readDate("Дата заявки (ДД.ММ.ГГГГ): ");
        bloodRequests.add(new BloodRequest(clinic, institution, bloodGroup, quantity, urgency, date));
        System.out.println("Заявка на кровь создана.");
    }

    private boolean hasBloodRequest(MedicalInstitution institution) {
        return bloodRequests.stream()
                .anyMatch(request -> request.getInstitution().equals(institution));
    }

    private void showBloodRequests(User clinic) {
        System.out.println("\n=== Заявки клиники ===");
        boolean found = false;
        for (BloodRequest request : bloodRequests) {
            if (request.getClinic().equals(clinic)) {
                System.out.println(request);
                found = true;
            }
        }
        if (!found) {
            System.out.println("Заявок пока нет.");
        }
    }

    private void showAllExaminations() {
        System.out.println("\n=== Медобследования доноров ===");
        if (examinations.isEmpty()) {
            System.out.println("Записей на медобследование пока нет.");
            return;
        }
        for (int index = 0; index < examinations.size(); index++) {
            System.out.print((index + 1) + ". ");
            printExamination(examinations.get(index));
        }
    }

    private void processExamination() {
        if (examinations.isEmpty()) {
            System.out.println("Обследований для обработки пока нет.");
            return;
        }
        showAllExaminations();
        int index = readIndex("Номер обследования: ", examinations.size());
        if (index < 0) {
            return;
        }
        MedicalExamination examination = examinations.get(index);
        System.out.println("Введите результаты обследования для " + examination.getDonor().getName());
        String bloodGroup = readRequired("Группа крови: ");
        String rhesus = readRequired("Резус-фактор (+/-): ");
        String hemoglobin = readRequired("Гемоглобин: ");
        String pressure = readRequired("Давление: ");
        String conclusion = readRequired("Заключение: ");
        examination.setResult(bloodGroup, rhesus, hemoglobin, pressure, conclusion);
        String previousDecision = examination.getDonationDecision();
        String decision = readLine("1 - одобрить запись на донацию, 2 - отклонить: ");
        if ("1".equals(decision)) {
            examination.setDonationDecision("Одобрено");
            completeDonorTask("Пройти медобследование в клинике "
                    + examination.getInstitution().getName());
            completeDoctorTask("Обработать запись донора " + examination.getDonor().getName());
            if (!"Одобрено".equals(previousDecision)) {
                notifications.add(new Notification(
                        examination.getDonor(),
                        "Врач одобрил вам дальнейшую запись на донацию по результатам медобследования."));
            }
        } else if ("2".equals(decision)) {
            examination.setDonationDecision("Отклонено");
            completeDonorTask("Пройти медобследование в клинике "
                    + examination.getInstitution().getName());
            completeDoctorTask("Обработать запись донора " + examination.getDonor().getName());
        } else {
            printError("Решение не изменено: неизвестная команда.");
            return;
        }
        System.out.println("Результат обследования сохранён.");
    }

    private void printExamination(MedicalExamination examination) {
        System.out.println("Донор: " + examination.getDonor().getName()
                + ", клиника: " + examination.getInstitution().getName()
                + ", дата: " + examination.getDate()
                + ", группа крови: " + examination.getBloodGroup()
                + ", резус: " + examination.getRhesus()
                + ", гемоглобин: " + examination.getHemoglobin()
                + ", давление: " + examination.getPressure()
                + ", заключение: " + examination.getConclusion()
                + ", решение: " + examination.getDonationDecision());
    }

    private MedicalExamination findApprovedExamination(User donor) {
        for (MedicalExamination examination : examinations) {
            if (examination.getDonor().equals(donor) && examination.isApproved()) {
                return examination;
            }
        }
        return null;
    }

    private int readIndex(String prompt, int size) {
        try {
            int index = Integer.parseInt(readLine(prompt)) - 1;
            if (index >= 0 && index < size) {
                return index;
            }
        } catch (NumberFormatException ignored) {
            // Сообщение об ошибке выводится ниже.
        }
        printError("Укажите номер из списка.");
        return -1;
    }

    private void showDonorStatistics(User donor) {
        long examinationCount = examinations.stream()
                .filter(examination -> examination.getDonor().equals(donor)).count();
        long donationCount = appointments.stream()
                .filter(appointment -> appointment.getDonor().equals(donor)).count();
        System.out.println("\n=== Статистика донора ===");
        System.out.println("Медобследований: " + examinationCount);
        System.out.println("Записей на донацию: " + donationCount);
        System.out.println("Сдано крови: пока нет данных из БД.");
    }

    private void showDonorNotifications(User donor) {
        System.out.println("\n=== Уведомления ===");
        boolean found = false;
        for (Notification notification : notifications) {
            if (notification.getRecipient().equals(donor)) {
                System.out.println((notification.isRead() ? "[Прочитано] " : "[Новое] ")
                        + notification.getMessage());
                notification.markAsRead();
                found = true;
            }
        }
        if (!found) {
            System.out.println("Новых уведомлений нет.");
        }
    }

    private void showTasks(List<Task> tasks, String title) {
        System.out.println("\n=== " + title + " ===");
        if (tasks.isEmpty()) {
            System.out.println("Задач пока нет.");
            return;
        }
        for (Task task : tasks) {
            System.out.println(task);
        }
    }

    private void completeDoctorTask(String text) {
        for (Task task : doctorTasks) {
            if (!task.isCompleted() && task.getText().equals(text)) {
                task.complete();
                return;
            }
        }
    }

    private void completeDonorTask(String text) {
        for (Task task : donorTasks) {
            if (!task.isCompleted() && task.getText().equals(text)) {
                task.complete();
                return;
            }
        }
    }

    private void showAdminStatistics(User clinic) {
        long requestCount = bloodRequests.stream()
                .filter(request -> request.getClinic().equals(clinic)).count();
        System.out.println("\n=== Статистика клиники ===");
        System.out.println("Заявок на кровь: " + requestCount);
        System.out.println("Обработано обследований: " + examinations.stream()
                .filter(examination -> !"Ожидает решения".equals(examination.getDonationDecision())).count());
        System.out.println("Выдано партий крови: пока нет данных из БД.");
    }

    private User findUser(String email) {
        return users.stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElse(null);
    }

    private String readRequired(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (!value.isBlank()) {
                return value;
            }
            printError("Поле не может быть пустым.");
        }
    }

    private String readEmail(String prompt) {
        while (true) {
            String email = readLine(prompt).toLowerCase();
            if (email.matches(EMAIL_PATTERN)) {
                return email;
            }
            printError("Введите корректный email, например donor@example.com.");
        }
    }

    private String readPhone(String prompt) {
        while (true) {
            String phone = readLine(prompt);
            long digitCount = phone.chars().filter(Character::isDigit).count();
            if (phone.matches(PHONE_PATTERN) && digitCount >= 10 && digitCount <= 15) {
                return phone;
            }
            printError("Введите корректный телефон: от 10 до 15 цифр, можно использовать +, пробелы, скобки и дефисы.");
        }
    }

    private String readDate(String prompt) {
        while (true) {
            String date = readLine(prompt);
            try {
                LocalDate.parse(date, DATE_FORMATTER);
                return date;
            } catch (DateTimeParseException exception) {
                printError("Введите существующую дату в формате ДД.ММ.ГГГГ.");
            }
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private void printWelcome() {
        System.out.println("=== Центр донорства крови ===");
        System.out.println("Регистрация и управление донорскими записями");
    }

    private void printMainMenu() {
        System.out.println("\n=== Главное меню ===");
        System.out.println("1. Регистрация");
        System.out.println("2. Вход");
        System.out.println("0. Выход");
    }

    private void printError(String message) {
        System.out.println("Ошибка: " + message);
    }

    private void exit() {
        running = false;
        System.out.println("До свидания!");
    }
}