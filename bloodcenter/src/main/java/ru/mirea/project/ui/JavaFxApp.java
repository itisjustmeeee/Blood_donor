package ru.mirea.project.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.fxml.FXMLLoader;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.model.BloodBatch;
import ru.mirea.project.model.BloodGroup;
import ru.mirea.project.model.Donation;
import ru.mirea.project.model.DonationRequest;
import ru.mirea.project.model.Donor;
import ru.mirea.project.model.MedicalExamination;
import ru.mirea.project.repository.BloodBatchRepository;
import ru.mirea.project.repository.BloodGroupRepository;
import ru.mirea.project.repository.DonationRepository;
import ru.mirea.project.repository.DonationRequestRepository;
import ru.mirea.project.repository.DonorRepository;
import ru.mirea.project.repository.MedicalExaminationRepository;
import ru.mirea.project.service.DonationService;
import ru.mirea.project.service.DonorService;
import ru.mirea.project.service.MedicalExaminationService;
import ru.mirea.project.util.DatabaseInitializer;
import ru.mirea.project.util.ExcelExporter;
import ru.mirea.project.util.TestDataSeeder;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class JavaFxApp extends Application {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final DonorRepository donorRepository = new DonorRepository();
    private final BloodGroupRepository bloodGroupRepository = new BloodGroupRepository();
    private final DonationRequestRepository requestRepository = new DonationRequestRepository();
    private final MedicalExaminationRepository examinationRepository = new MedicalExaminationRepository();
    private final BloodBatchRepository batchRepository = new BloodBatchRepository();
    private final DonationRepository donationRepository = new DonationRepository();

    private final DonorService donorService = new DonorService(donorRepository, bloodGroupRepository);
    private final DonationService donationService = new DonationService(
            requestRepository, examinationRepository, donationRepository, batchRepository);
    private final MedicalExaminationService examinationService = new MedicalExaminationService(
            examinationRepository, requestRepository, donorRepository);

    private final ObservableList<Donor> donorRows = FXCollections.observableArrayList();
    private final ObservableList<DonationRequest> requestRows = FXCollections.observableArrayList();
    private final ObservableList<MedicalExamination> examinationRows = FXCollections.observableArrayList();
    private final ObservableList<Donation> donationRows = FXCollections.observableArrayList();
    private final ObservableList<BloodBatch> batchRows = FXCollections.observableArrayList();
    private final List<Runnable> statisticsRefreshers = new ArrayList<>();
    private final List<Runnable> tableRefreshers = new ArrayList<>();
    private Button donorExaminationButton;
    private Button donorDonationButton;

    private Stage stage;
    private Donor currentUser;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        stage.setTitle("Центр донорства крови");
        stage.setMinWidth(800);
        stage.setMinHeight(520);
        showLoadingScene();
        Task<Void> initialization = new Task<>() {
            @Override
            protected Void call() throws Exception {
                DatabaseInitializer.initialize();
                if (donorRepository.findAll().isEmpty()) {
                    TestDataSeeder.seed();
                }

                return null;
            }
        };
        initialization.setOnSucceeded(event -> showLoginScene());
        initialization.setOnFailed(event ->
                showError("Ошибка подключения к базе данных",
                        initialization.getException() == null
                                ? "Неизвестная ошибка."
                                : initialization.getException().getMessage()));
        Thread thread = new Thread(initialization, "database-initialization");
        thread.setDaemon(true);
        thread.start();
    }

    private void showLoadingScene() {
        VBox loading = new VBox(14, new Label("Подключение к базе данных..."),
                new ProgressIndicator());
        loading.setAlignment(Pos.CENTER);
        stage.setScene(new Scene(loading, 420, 220));
        stage.show();
    }

    public void showLoginScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            loader.<LoginController>getController().setApplication(this);
            stage.setScene(new Scene(root, 460, 300));
            stage.getScene().getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/styles.css")).toExternalForm());
            stage.show();
            stage.sizeToScene();
        } catch (IOException exception) {
            showError("Ошибка интерфейса", "Не удалось загрузить окно входа: " + exception.getMessage());
        }
    }

    private void showLegacyLoginScene() {
        VBox root = new VBox(18);
        root.setPadding(new Insets(28));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("login-root");

        Label title = new Label("Центр донорства крови");
        title.getStyleClass().add("title");

        GridPane form = new GridPane();
        form.setVgap(12);
        form.setHgap(12);
        form.setAlignment(Pos.CENTER);

        Label emailLabel = new Label("Email");
        TextField emailField = new TextField();
        emailField.setPromptText("example@mail.com");

        Label passwordLabel = new Label("Пароль");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Введите пароль");

        form.add(emailLabel, 0, 0);
        form.add(emailField, 1, 0);
        form.add(passwordLabel, 0, 1);
        form.add(passwordField, 1, 1);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER);
        Button loginButton = new Button("Войти");
        loginButton.getStyleClass().add("primary-button");
        Button registerButton = new Button("Регистрация");
        registerButton.getStyleClass().add("secondary-button");
        actions.getChildren().addAll(loginButton, registerButton);

        loginButton.setOnAction(event -> {
            String email = emailField.getText() == null ? "" : emailField.getText().trim();
            String password = passwordField.getText() == null ? "" : passwordField.getText();
            handleLogin(email, password);
        });

        registerButton.setOnAction(event -> openRegistrationDialog());

        root.getChildren().addAll(title, form, actions);
        stage.setScene(new Scene(root, 460, 300));
        stage.getScene().getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/styles.css")).toExternalForm());
        stage.show();
        stage.sizeToScene();
    }

    public void handleLogin(String email, String password) {
        if (email.isBlank() || password.isBlank()) {
            showError("Ошибка входа", "Email и пароль обязательны для заполнения.");
            return;
        }
        try {
            Donor donor = donorService.authenticate(email, password);
            if (donor == null) {
                showError("Ошибка входа", "Неверный email или пароль.");
                return;
            }
            currentUser = donor;
            showMainScene();
        } catch (SQLException exception) {
            showError("Ошибка входа", exception.getMessage());
        }
    }

    public void openRegistrationDialog() {
        Dialog<Donor> dialog = new Dialog<>();
        dialog.setTitle("Регистрация пользователя");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(stage);

        ButtonType saveButtonType = new ButtonType("Зарегистрировать", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(
                saveButtonType, new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE));

        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.setHgap(12);
        grid.setPadding(new Insets(16));

        TextField fullNameField = new TextField();
        fullNameField.setPromptText("ФИО");
        TextField ageField = new TextField();
        ageField.setPromptText("Возраст");
        ComboBox<String> genderBox = new ComboBox<>(FXCollections.observableArrayList("male", "female", "other"));
        genderBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(String value) {
                return value == null ? "" : localizeGender(value);
            }

            @Override
            public String fromString(String value) {
                return value;
            }
        });
        genderBox.setValue("male");
        TextField weightField = new TextField();
        weightField.setPromptText("Вес (кг)");
        TextField emailField = new TextField();
        emailField.setPromptText("email@example.com");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Пароль");
        ComboBox<BloodGroup> bloodGroupBox = new ComboBox<>();
        try {
            bloodGroupBox.setItems(FXCollections.observableArrayList(bloodGroupRepository.findAll()));
        } catch (SQLException exception) {
            showError("Ошибка загрузки групп крови", exception.getMessage());
            return;
        }
        bloodGroupBox.setValue(bloodGroupBox.getItems().isEmpty() ? null : bloodGroupBox.getItems().get(0));

        grid.add(new Label("ФИО"), 0, 1);
        grid.add(fullNameField, 1, 1);
        grid.add(new Label("Возраст"), 0, 2);
        grid.add(ageField, 1, 2);
        grid.add(new Label("Пол"), 0, 3);
        grid.add(genderBox, 1, 3);
        grid.add(new Label("Вес"), 0, 4);
        grid.add(weightField, 1, 4);
        grid.add(new Label("Email"), 0, 5);
        grid.add(emailField, 1, 5);
        grid.add(new Label("Пароль"), 0, 6);
        grid.add(passwordField, 1, 6);
        grid.add(new Label("Группа крови"), 0, 7);
        grid.add(bloodGroupBox, 1, 7);
        dialog.getDialogPane().setContent(grid);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        Donor[] registrationResult = new Donor[1];
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (fullNameField.getText() == null || fullNameField.getText().isBlank()) {
                showError("Ошибка регистрации", "ФИО обязательно для заполнения.");
                event.consume();
                return;
            }
            try {
                int age = Integer.parseInt(ageField.getText().trim());
                int weight = Integer.parseInt(weightField.getText().trim());
                BloodGroup group = bloodGroupBox.getValue();
                if (group == null) {
                    showError("Ошибка регистрации", "Не выбрана группа крови.");
                    event.consume();
                    return;
                }
                registrationResult[0] = donorService.register(
                        fullNameField.getText().trim(), age, "donor", genderBox.getValue(),
                        weight, emailField.getText().trim(), passwordField.getText(), group.id());
                showInfo("Регистрация завершена", "Пользователь успешно зарегистрирован.");
            } catch (NumberFormatException exception) {
                showError("Ошибка регистрации", "Возраст и вес должны быть числами.");
                event.consume();
            } catch (BusinessException | SQLException exception) {
                showError("Ошибка регистрации", exception.getMessage());
                event.consume();
            }
        });

        dialog.setResultConverter(buttonType -> {
            return buttonType == saveButtonType ? registrationResult[0] : null;
        });

        Optional<Donor> result = dialog.showAndWait();
        result.ifPresent(user -> {
            currentUser = user;
            showMainScene();
        });
    }

    public void showMainScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            loader.<MainController>getController().setApplication(this);
            stage.setScene(new Scene(root, 920, 600));
            stage.getScene().getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/styles.css")).toExternalForm());
            stage.show();
            stage.sizeToScene();
        } catch (IOException exception) {
            showError("Ошибка интерфейса", "Не удалось загрузить основное окно: " + exception.getMessage());
        }
    }

    private void showLegacyMainScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("main-root");
        root.setPadding(new Insets(16));

        Label welcome = new Label("Добро пожаловать, " + currentUser.fullName());
        welcome.getStyleClass().add("header");

        Button logoutButton = new Button("Выход");
        logoutButton.getStyleClass().add("secondary-button");
        logoutButton.setOnAction(event -> {
            currentUser = null;
            showLoginScene();
        });

        HBox topBar = new HBox(14, welcome, logoutButton);
        topBar.setAlignment(Pos.CENTER_LEFT);
        root.setTop(topBar);

        TabPane tabs = "doctor".equalsIgnoreCase(currentUser.role()) ? buildDoctorTabs() : buildDonorTabs();
        root.setCenter(tabs);

        stage.setScene(new Scene(root, 920, 600));
        stage.getScene().getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/styles.css")).toExternalForm());
        stage.show();
    }

    public TabPane buildDonorTabs() {
        TabPane tabs = new TabPane();

        Tab profileTab = new Tab("Профиль");
        profileTab.setContent(scrollable(buildDonorProfileTab()));

        Tab examTab = new Tab("Обследования");
        examTab.setContent(scrollable(buildDonorExaminationTab()));

        Tab requestTab = new Tab("Запись на донацию");
        requestTab.setContent(scrollable(buildDonationRequestTab()));

        Tab donationTab = new Tab("Мои донации");
        donationTab.setContent(scrollable(buildMyDonationTab()));

        Tab statsTab = new Tab("Статистика");
        statsTab.setContent(scrollable(new StatisticsPane(this, currentUser.id()).create()));

        tabs.getTabs().addAll(profileTab, examTab, requestTab, donationTab, statsTab);
        return tabs;
    }

    public TabPane buildDoctorTabs() {
        TabPane tabs = new TabPane();

        Tab donorTab = new Tab("Доноры");
        donorTab.setContent(scrollable(buildDonorTableTab()));

        Tab examTab = new Tab("Обследования");
        examTab.setContent(scrollable(buildDoctorExaminationTab()));

        Tab requestTab = new Tab("Заявки на донацию");
        requestTab.setContent(scrollable(buildDoctorRequestTab()));

        Tab donationTab = new Tab("Донации");
        donationTab.setContent(scrollable(buildDoctorDonationTab()));

        Tab batchTab = new Tab("Партии крови");
        batchTab.setContent(scrollable(buildBatchTab()));

        Tab statsTab = new Tab("Статистика");
        statsTab.setContent(scrollable(new StatisticsPane(this, null).create()));

        tabs.getTabs().addAll(donorTab, examTab, requestTab, donationTab, batchTab, statsTab);
        return tabs;
    }

    public Donor getCurrentUser() {
        return currentUser;
    }

    public TabPane buildCurrentUserTabs() {
        return "doctor".equalsIgnoreCase(currentUser.role())
                ? new DoctorView(this).create() : new DonorView(this).create();
    }

    private ScrollPane scrollable(Node content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setPannable(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("page-scroll");
        return scrollPane;
    }

    private <T> TableView<T> prepareTable(TableView<T> table) {
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setMinWidth(Region.USE_PREF_SIZE);
        table.setMaxWidth(Region.USE_PREF_SIZE);
        Platform.runLater(() -> {
            for (TableColumn<T, ?> column : table.getColumns()) {
                int longestValue = column.getText() == null ? 0 : column.getText().length();
                int rowsToMeasure = Math.min(table.getItems().size(), 100);
                for (int row = 0; row < rowsToMeasure; row++) {
                    Object value = column.getCellData(row);
                    if (value != null) {
                        longestValue = Math.max(longestValue, String.valueOf(value).length());
                    }
                }
                double width = Math.max(90, Math.min(360, longestValue * 8.0 + 28));
                column.setPrefWidth(width);
                column.setMinWidth(width);
                column.setMaxWidth(width);
            }
            double contentWidth = table.getColumns().stream()
                    .mapToDouble(TableColumnBase::getWidth)
                    .sum() + 24;
            table.setPrefWidth(contentWidth);
            table.setMinWidth(contentWidth);
            table.setMaxWidth(contentWidth);
        });
        return table;
    }

    private VBox buildDonorProfileTab() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(18));
        Label title = new Label("Профиль донора");
        title.getStyleClass().add("section-title");

        Label info = new Label(String.format(
                "ФИО: %s%nВозраст: %d%nПол: %s%nВес: %d кг%nГруппа крови: %s%nEmail: %s",
                currentUser.fullName(), currentUser.age(), localizeGender(currentUser.gender()), currentUser.weight(),
                currentUser.bloodGroup(), currentUser.email()));
        info.setWrapText(true);

        Button updateButton = new Button("Изменить профиль");
        updateButton.getStyleClass().add("primary-button");
        updateButton.setOnAction(event -> openProfileEditor());

        Button logoutButton = new Button("Выйти из профиля");
        logoutButton.getStyleClass().add("secondary-button");
        logoutButton.setOnAction(event -> {
            currentUser = null;
            showLoginScene();
        });

        box.getChildren().addAll(title, info, updateButton, logoutButton);
        return box;
    }

    private VBox buildDonorExaminationTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));

        HBox toolbar = new HBox(10);
        ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList(
                "Все", "на рассмотрении", "допущен(а)", "не допущен(а)", "отменена"));
        status.setValue("Все");
        Button addButton = new Button("Записаться на обследование");
        donorExaminationButton = addButton;
        addButton.getStyleClass().add("primary-button");
        Button deleteButton = new Button("Отменить запись");
        deleteButton.getStyleClass().add("warning-button");
        Button editButton = new Button("Изменить обследование");
        editButton.getStyleClass().add("secondary-button");
        DatePicker from = new DatePicker();
        DatePicker to = new DatePicker();
        constrainDateRange(from, to);

        TableView<MedicalExamination> table = new TableView<>();
        TableColumn<MedicalExamination, String> dateCol = new TableColumn<>("Дата обследования");
        dateCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().examinationDate().format(DATE_FORMAT)));
        TableColumn<MedicalExamination, String> hemoglobinCol = new TableColumn<>("Гемоглобин");
        hemoglobinCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().hemoglobin() == null ? "-" : cell.getValue().hemoglobin().toPlainString()));
        TableColumn<MedicalExamination, String> pressureCol = new TableColumn<>("Давление");
        pressureCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().bloodPressure() == null ? "-" : cell.getValue().bloodPressure()));
        TableColumn<MedicalExamination, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(cell -> new SimpleStringProperty(localizeExaminationStatus(cell.getValue().admissionStatus())));
        TableColumn<MedicalExamination, String> conclusionCol = new TableColumn<>("Заключение");
        conclusionCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().conclusion() == null ? "-" : cell.getValue().conclusion()));
        table.getColumns().addAll(dateCol, hemoglobinCol, pressureCol, statusCol, conclusionCol);
        prepareTable(table);

        addButton.setOnAction(event -> openExaminationBookingDialog());
        deleteButton.setOnAction(event -> {
            MedicalExamination selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showError("Выбор записи", "Сначала выберите обследование для отмены.");
                return;
            }
            confirmDelete(() -> {
                try {
                    examinationService.deleteForDonor(selected.id(), currentUser);
                    refreshDonorData();
                    table.setItems(examinationRows);
                    showInfo("Удаление", "Запись на обследование отменена.");
                } catch (BusinessException | SQLException exception) {
                    showError("Ошибка отмены", exception.getMessage());
                }
            }, "Отменить запись на обследование?");
        });
        status.valueProperty().addListener((obs, oldValue, newValue) ->
                applyExaminationFilter(table, newValue, from.getValue(), to.getValue()));
        from.valueProperty().addListener((obs, oldValue, newValue) ->
                applyExaminationFilter(table, status.getValue(), newValue, to.getValue()));
        to.valueProperty().addListener((obs, oldValue, newValue) ->
                applyExaminationFilter(table, status.getValue(), from.getValue(), newValue));
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            boolean pending = selected != null && "pending".equalsIgnoreCase(selected.admissionStatus());
            deleteButton.setDisable(!pending);
            editButton.setDisable(!pending);
        });
        deleteButton.setDisable(true);
        editButton.setDisable(true);
        editButton.setOnAction(event -> {
            MedicalExamination selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openDonorExaminationEditor(selected);
            }
        });

        toolbar.getChildren().addAll(addButton, editButton, deleteButton, labeledControl("Статус", status),
                labeledControl("Начальная дата", from), labeledControl("Конечная дата", to));
        box.getChildren().addAll(toolbar, table);
        refreshDonorData();
        table.setItems(examinationRows);
        VBox.setVgrow(table, Priority.ALWAYS);
        updateDonorActionButtons(addButton, null);
        return box;
    }

    private VBox buildDonationRequestTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        Button bookButton = new Button("Записаться на донацию");
        donorDonationButton = bookButton;
        bookButton.getStyleClass().add("primary-button");
        Button cancelButton = new Button("Отменить запись");
        cancelButton.getStyleClass().add("warning-button");
        ComboBox<String> statusFilter = new ComboBox<>(FXCollections.observableArrayList(
                "Все", "создано", "принято", "завершено", "отклонено"));
        statusFilter.setValue("Все");
        DatePicker from = new DatePicker();
        DatePicker to = new DatePicker();
        constrainDateRange(from, to);
        TableView<DonationRequest> table = new TableView<>();
        TableColumn<DonationRequest, String> dateCol = new TableColumn<>("Дата заявки");
        dateCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().donationDate().format(DATE_FORMAT)));
        TableColumn<DonationRequest, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(cell -> new SimpleStringProperty(localizeRequestStatus(cell.getValue().status())));
        table.getColumns().addAll(dateCol, statusCol);
        prepareTable(table);
        bookButton.setOnAction(event -> openDonationBookingDialog());
        cancelButton.setOnAction(event -> {
            DonationRequest selected = table.getSelectionModel().getSelectedItem();
            if (selected == null || !"created".equalsIgnoreCase(selected.status())) {
                showError("Отмена записи", "Выберите запись со статусом «создано».");
                return;
            }
            confirmDelete(() -> {
                try {
                    requestRepository.updateStatus(selected.id(), "cancelled");
                    refreshDonorData();
                } catch (SQLException exception) {
                    showError("Ошибка отмены", exception.getMessage());
                }
            }, "Отменить выбранную запись на донацию?");
        });
        statusFilter.valueProperty().addListener((obs, oldValue, newValue) ->
                applyRequestFilter(table, newValue, from.getValue(), to.getValue()));
        from.valueProperty().addListener((obs, oldValue, newValue) ->
                applyRequestFilter(table, statusFilter.getValue(), newValue, to.getValue()));
        to.valueProperty().addListener((obs, oldValue, newValue) ->
                applyRequestFilter(table, statusFilter.getValue(), from.getValue(), newValue));
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) ->
                cancelButton.setDisable(selected == null || !"created".equalsIgnoreCase(selected.status())));
        box.getChildren().addAll(new HBox(10, bookButton, cancelButton,
                labeledControl("Статус", statusFilter),
                labeledControl("Начальная дата", from),
                labeledControl("Конечная дата", to)), table);
        table.setItems(requestRows);
        refreshDonorData();
        VBox.setVgrow(table, Priority.ALWAYS);
        updateDonorActionButtons(null, bookButton);
        cancelButton.setDisable(true);
        return box;
    }

    private VBox buildMyDonationTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        TableView<Donation> table = new TableView<>();
        TableColumn<Donation, String> dateCol = new TableColumn<>("Дата донации");
        dateCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().donationDate().format(DATE_FORMAT)));
        TableColumn<Donation, String> volumeCol = new TableColumn<>("Объём");
        volumeCol.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().bloodVolume())));
        TableColumn<Donation, String> typeCol = new TableColumn<>("Тип");
        typeCol.setCellValueFactory(cell -> new SimpleStringProperty(localizeDonationType(cell.getValue().donationType())));
        TableColumn<Donation, String> resultCol = new TableColumn<>("Результат");
        resultCol.setCellValueFactory(cell -> new SimpleStringProperty(localizeDonationResult(cell.getValue().result())));
        table.getColumns().addAll(dateCol, volumeCol, typeCol, resultCol);
        prepareTable(table);
        ComboBox<String> resultFilter = new ComboBox<>(FXCollections.observableArrayList(
                "Все", "успешно", "отмена"));
        resultFilter.setValue("Все");
        DatePicker from = new DatePicker();
        DatePicker to = new DatePicker();
        constrainDateRange(from, to);
        resultFilter.valueProperty().addListener((obs, oldValue, newValue) ->
                applyDonationFilter(table, newValue, from.getValue(), to.getValue(), null));
        from.valueProperty().addListener((obs, oldValue, newValue) ->
                applyDonationFilter(table, resultFilter.getValue(), newValue, to.getValue(), null));
        to.valueProperty().addListener((obs, oldValue, newValue) ->
                applyDonationFilter(table, resultFilter.getValue(), from.getValue(), newValue, null));
        box.getChildren().addAll(new HBox(10,
                labeledControl("Результат", resultFilter),
                labeledControl("Начальная дата", from),
                labeledControl("Конечная дата", to)), table);
        table.setItems(donationRows);
        refreshDonorData();
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    private VBox buildDonorTableTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        TextField search = new TextField();
        search.setPromptText("Поиск по имени или группе крови");
        ComboBox<String> sort = new ComboBox<>(FXCollections.observableArrayList("По имени", "По группе крови"));
        sort.setValue("По имени");
        TableView<Donor> table = new TableView<>();
        TableColumn<Donor, String> nameCol = new TableColumn<>("ФИО");
        nameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().fullName()));
        TableColumn<Donor, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().email()));
        TableColumn<Donor, String> groupCol = new TableColumn<>("Группа");
        groupCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().bloodGroup()));
        TableColumn<Donor, String> ageCol = new TableColumn<>("Возраст");
        ageCol.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().age())));
        TableColumn<Donor, String> genderCol = new TableColumn<>("Пол");
        genderCol.setCellValueFactory(cell -> new SimpleStringProperty(localizeGender(cell.getValue().gender())));
        TableColumn<Donor, String> weightCol = new TableColumn<>("Вес");
        weightCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().weight() + " кг"));
        table.getColumns().addAll(nameCol, emailCol, groupCol, ageCol, genderCol, weightCol);
        prepareTable(table);
        Runnable apply = () -> {
            String value = search.getText() == null ? "" : search.getText().toLowerCase();
            List<Donor> filtered = donorRows.stream()
                    .filter(d -> value.isBlank() || d.fullName().toLowerCase().contains(value)
                            || d.bloodGroup().toLowerCase().contains(value))
                    .sorted(("По группе крови".equals(sort.getValue())
                            ? Comparator.comparing(Donor::bloodGroup)
                            : Comparator.comparing(Donor::fullName)))
                    .toList();
            table.setItems(FXCollections.observableArrayList(filtered));
        };
        search.textProperty().addListener((obs, oldValue, newValue) -> apply.run());
        sort.valueProperty().addListener((obs, oldValue, newValue) -> apply.run());
        box.getChildren().addAll(new HBox(10,
                labeledControl("Поиск", search),
                labeledControl("Сортировка", sort)), table);
        table.setItems(donorRows);
        refreshDoctorData();
        VBox.setVgrow(table, Priority.ALWAYS);
        apply.run();
        tableRefreshers.add(apply);
        return box;
    }

    private VBox buildDoctorExaminationTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));

        HBox toolbar = new HBox(12);
        TextField filter = new TextField();
        filter.setPromptText("Поиск по имени или заключению");
        ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList(
                "Все", "на рассмотрении", "допущен(а)", "не допущен(а)", "отменена"));
        status.setValue("Все");
        Button processButton = new Button("Обработать обследование");
        processButton.getStyleClass().add("primary-button");
        DatePicker from = new DatePicker();
        DatePicker to = new DatePicker();
        constrainDateRange(from, to);
        ComboBox<String> sort = new ComboBox<>(FXCollections.observableArrayList("По дате", "По имени донора"));
        sort.setValue("По дате");
        toolbar.getChildren().addAll(processButton,
                labeledControl("Поиск по имени или заключению", filter),
                labeledControl("Статус", status),
                labeledControl("Начальная дата", from),
                labeledControl("Конечная дата", to),
                labeledControl("Сортировка", sort));

        TableView<MedicalExamination> table = new TableView<>();
        TableColumn<MedicalExamination, String> donorCol = new TableColumn<>("Донор");
        donorCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().donorName()));
        TableColumn<MedicalExamination, String> dateCol = new TableColumn<>("Дата");
        dateCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().examinationDate().format(DATE_FORMAT)));
        TableColumn<MedicalExamination, String> hemoglobinCol = new TableColumn<>("Гемоглобин");
        hemoglobinCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().hemoglobin() == null ? "-" : cell.getValue().hemoglobin().toPlainString()));
        TableColumn<MedicalExamination, String> pressureCol = new TableColumn<>("Давление");
        pressureCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().bloodPressure() == null ? "-" : cell.getValue().bloodPressure()));
        TableColumn<MedicalExamination, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(cell -> new SimpleStringProperty(localizeExaminationStatus(cell.getValue().admissionStatus())));
        TableColumn<MedicalExamination, String> conclusionCol = new TableColumn<>("Заключение");
        conclusionCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().conclusion() == null ? "-" : cell.getValue().conclusion()));
        table.getColumns().addAll(donorCol, dateCol, hemoglobinCol, pressureCol, statusCol, conclusionCol);
        prepareTable(table);

        processButton.setOnAction(event -> {
            MedicalExamination selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showError("Ошибка", "Сначала выберите обследование.");
                return;
            }
            openMedicalExaminationDecision(selected);
        });
        Runnable apply = () -> applyDoctorExaminationFilter(table, filter.getText(), status.getValue(),
                from.getValue(), to.getValue(), sort.getValue());
        filter.textProperty().addListener((obs, oldValue, newValue) -> apply.run());
        status.valueProperty().addListener((obs, oldValue, newValue) -> apply.run());
        from.valueProperty().addListener((obs, oldValue, newValue) -> apply.run());
        to.valueProperty().addListener((obs, oldValue, newValue) -> apply.run());
        sort.valueProperty().addListener((obs, oldValue, newValue) -> apply.run());

        box.getChildren().addAll(toolbar, table);
        table.setItems(examinationRows);
        refreshDoctorData();
        VBox.setVgrow(table, Priority.ALWAYS);
        apply.run();
        tableRefreshers.add(apply);
        return box;
    }

    private VBox buildDoctorRequestTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        TextField search = new TextField();
        search.setPromptText("Поиск по имени или группе крови");
        DatePicker from = new DatePicker();
        DatePicker to = new DatePicker();
        constrainDateRange(from, to);
        Button edit = new Button("Изменить заявку");
        edit.getStyleClass().add("primary-button");
        TableView<DonationRequest> table = new TableView<>();
        TableColumn<DonationRequest, String> name = new TableColumn<>("Донор");
        name.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().donorName()));
        TableColumn<DonationRequest, String> group = new TableColumn<>("Группа крови");
        group.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().bloodGroup()));
        TableColumn<DonationRequest, String> date = new TableColumn<>("Дата донации");
        date.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().donationDate().format(DATE_FORMAT)));
        TableColumn<DonationRequest, String> status = new TableColumn<>("Статус");
        status.setCellValueFactory(c -> new SimpleStringProperty(localizeRequestStatus(c.getValue().status())));
        table.getColumns().addAll(name, group, date, status);
        prepareTable(table);
        Runnable apply = () -> {
            String text = search.getText() == null ? "" : search.getText().toLowerCase();
            List<DonationRequest> filtered = requestRows.stream()
                    .filter(r -> (text.isBlank() || r.donorName().toLowerCase().contains(text)
                            || r.bloodGroup().toLowerCase().contains(text))
                            && inRange(r.donationDate(), from.getValue(), to.getValue()))
                    .toList();
            table.setItems(FXCollections.observableArrayList(filtered));
        };
        search.textProperty().addListener((o, a, b) -> apply.run());
        from.valueProperty().addListener((o, a, b) -> apply.run());
        to.valueProperty().addListener((o, a, b) -> apply.run());
        edit.setOnAction(event -> {
            DonationRequest selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showError("Изменение заявки", "Сначала выберите заявку.");
                return;
            }
            openDoctorRequestEditor(selected);
        });
        box.getChildren().addAll(new HBox(10, edit, labeledControl("Поиск", search),
                labeledControl("Начальная дата", from),
                labeledControl("Конечная дата", to)), table);
        table.setItems(requestRows);
        refreshDoctorData();
        VBox.setVgrow(table, Priority.ALWAYS);
        apply.run();
        tableRefreshers.add(apply);
        return box;
    }

    private VBox buildDoctorDonationTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        Button processButton = new Button("Обработать донацию");
        processButton.getStyleClass().add("primary-button");
        Button addButton = new Button("Добавить донацию");
        addButton.getStyleClass().add("secondary-button");
        ComboBox<String> resultFilter = new ComboBox<>(FXCollections.observableArrayList("Все", "успешно", "отмена"));
        resultFilter.setValue("Все");
        ComboBox<String> typeFilter = new ComboBox<>(FXCollections.observableArrayList(
                "Все", "цельная кровь", "плазма", "тромбоциты"));
        typeFilter.setValue("Все");
        DatePicker from = new DatePicker();
        DatePicker to = new DatePicker();
        constrainDateRange(from, to);
        ComboBox<String> sort = new ComboBox<>(FXCollections.observableArrayList("По дате"));
        sort.setValue("По дате");
        TableView<Donation> table = new TableView<>();
        TableColumn<Donation, String> donorCol = new TableColumn<>("Донор");
        donorCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().donorName()));
        TableColumn<Donation, String> dateCol = new TableColumn<>("Дата");
        dateCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().donationDate().format(DATE_FORMAT)));
        TableColumn<Donation, String> volumeCol = new TableColumn<>("Объём");
        volumeCol.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().bloodVolume())));
        TableColumn<Donation, String> typeCol = new TableColumn<>("Тип");
        typeCol.setCellValueFactory(cell -> new SimpleStringProperty(localizeDonationType(cell.getValue().donationType())));
        TableColumn<Donation, String> resultCol = new TableColumn<>("Результат");
        resultCol.setCellValueFactory(cell -> new SimpleStringProperty(localizeDonationResult(cell.getValue().result())));
        table.getColumns().addAll(donorCol, dateCol, volumeCol, typeCol, resultCol);
        prepareTable(table);
        processButton.setOnAction(event -> {
            Donation selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showError("Обработка донации", "Выберите донацию.");
                return;
            }
            openDonationResultDialog(selected);
        });
        addButton.setOnAction(event -> openDonationProcessingDialog());
        Runnable apply = () -> applyDonationFilter(table, resultFilter.getValue(), from.getValue(), to.getValue(), typeFilter.getValue());
        resultFilter.valueProperty().addListener((obs, oldValue, newValue) -> apply.run());
        typeFilter.valueProperty().addListener((obs, oldValue, newValue) -> apply.run());
        from.valueProperty().addListener((obs, oldValue, newValue) -> apply.run());
        to.valueProperty().addListener((obs, oldValue, newValue) -> apply.run());
        box.getChildren().addAll(new HBox(10, processButton, addButton,
                labeledControl("Результат", resultFilter),
                labeledControl("Тип донации", typeFilter),
                labeledControl("Начальная дата", from),
                labeledControl("Конечная дата", to),
                labeledControl("Сортировка", sort)), table);
        table.setItems(donationRows);
        refreshDoctorData();
        VBox.setVgrow(table, Priority.ALWAYS);
        apply.run();
        tableRefreshers.add(apply);
        return box;
    }

    private VBox buildBatchTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        TableView<BloodBatch> table = new TableView<>();
        TableColumn<BloodBatch, String> numberCol = new TableColumn<>("Номер");
        numberCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().batchNumber()));
        TableColumn<BloodBatch, String> groupCol = new TableColumn<>("Группа");
        groupCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().bloodGroup()));
        TableColumn<BloodBatch, String> prepCol = new TableColumn<>("Подготовка");
        prepCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().preparationDate().format(DATE_FORMAT)));
        TableColumn<BloodBatch, String> expiryCol = new TableColumn<>("Срок");
        expiryCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().expirationDate().format(DATE_FORMAT)));
        TableColumn<BloodBatch, String> volumeCol = new TableColumn<>("Объём");
        volumeCol.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().totalVolume())));
        TableColumn<BloodBatch, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(cell -> new SimpleStringProperty(localizeBatchStatus(cell.getValue().status())));
        table.getColumns().addAll(numberCol, groupCol, prepCol, expiryCol, volumeCol, statusCol);
        prepareTable(table);
        box.getChildren().add(table);
        table.setItems(batchRows);
        refreshDoctorData();
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    private void openDoctorRequestEditor(DonationRequest request) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Изменение заявки на донацию");
        dialog.initOwner(stage);
        ButtonType save = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(
                save, new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE));
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(16));
        grid.setHgap(10);
        grid.setVgap(10);
        DatePicker date = new DatePicker(request.donationDate());
        ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList(
                "создано", "принято", "завершено", "отклонено"));
        status.setValue(localizeRequestStatus(request.status()));
        grid.add(new Label("Дата донации"), 0, 0);
        grid.add(date, 1, 0);
        grid.add(new Label("Статус"), 0, 1);
        grid.add(status, 1, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> {
            if (button != save) {
                return null;
            }
            try {
                if (date.getValue() == null || date.getValue().isBefore(LocalDate.now())) {
                    throw new IllegalArgumentException("Дата донации должна быть сегодня или позже.");
                }
                requestRepository.update(request.id(), date.getValue(), requestStatusValue(status.getValue()));
                refreshDoctorData();
                showInfo("Заявка обновлена", "Информация о заявке сохранена.");
                return true;
            } catch (SQLException | IllegalArgumentException exception) {
                showError("Ошибка заявки", exception.getMessage());
                return false;
            }
        });
        dialog.showAndWait();
    }

    private void openDonationResultDialog(Donation donation) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Результат донации");
        dialog.initOwner(stage);
        ButtonType save = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(
                save, new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE));
        ComboBox<String> result = new ComboBox<>(FXCollections.observableArrayList("успешно", "отмена"));
        result.setValue(localizeDonationResult(donation.result()));
        VBox content = new VBox(10, new Label("Результат донации"), result);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(button -> {
            if (button != save) {
                return null;
            }
            try {
                donationRepository.updateResult(donation.id(), donationResultValue(result.getValue()));
                refreshDoctorData();
                showInfo("Результат обновлён", "Изменён только результат донации.");
                return true;
            } catch (SQLException exception) {
                showError("Ошибка донации", exception.getMessage());
                return false;
            }
        });
        dialog.showAndWait();
    }

    private VBox buildStatisticsTab(Integer donorId) {
        VBox box = new VBox(16);
        box.setPadding(new Insets(16));
        Label title = new Label("Статистика");
        title.getStyleClass().add("section-title");

        VBox statsBox = new VBox(8);
        statsBox.getStyleClass().add("stats-panel");

        Button exportCsv = new Button("Экспорт CSV");
        exportCsv.getStyleClass().add("secondary-button");
        Button exportExcel = new Button("Экспорт Excel");
        exportExcel.getStyleClass().add("primary-button");

        exportCsv.setOnAction(event -> exportStatistics("csv", donorId));
        exportExcel.setOnAction(event -> exportStatistics("xlsx", donorId));

        Label summary = new Label();
        summary.setWrapText(true);
        statsBox.getChildren().add(summary);
        updateStatisticsLabel(summary, donorId);
        statisticsRefreshers.add(() -> updateStatisticsLabel(summary, donorId));
        statsBox.getChildren().addAll(exportCsv, exportExcel);
        box.getChildren().addAll(title, statsBox);
        return box;
    }

    public VBox createStatisticsPane(Integer donorId) {
        return buildStatisticsTab(donorId);
    }

    private void updateStatisticsLabel(Label summary, Integer donorId) {
        try {
            List<Donor> donors = donorId == null
                    ? donorRepository.findAll()
                    : List.of(currentUser);
            List<DonationRequest> requests = donorId == null ? requestRepository.findAll() : requestRepository.findByDonor(donorId);
            List<MedicalExamination> examinations = donorId == null ? examinationRepository.findAll() : examinationRepository.findByDonor(donorId);
            List<Donation> donations = donorId == null ? donationRepository.findAll() : donationRepository.findByDonor(donorId);

            long accepted = examinations.stream().filter(item -> "accepted".equalsIgnoreCase(item.admissionStatus())).count();
            long rejected = examinations.stream().filter(item -> "rejected".equalsIgnoreCase(item.admissionStatus())).count();
            long pending = examinations.stream().filter(item -> "pending".equalsIgnoreCase(item.admissionStatus())).count();
            long cancelled = examinations.stream().filter(item -> "cancelled".equalsIgnoreCase(item.admissionStatus())).count();
            long bloodVolume = donations.stream().mapToLong(Donation::bloodVolume).sum();
            long createdRequests = requests.stream().filter(item -> "created".equalsIgnoreCase(item.status())).count();
            long confirmedRequests = requests.stream().filter(item -> "confirmed".equalsIgnoreCase(item.status())).count();
            long completedRequests = requests.stream().filter(item -> "completed".equalsIgnoreCase(item.status())).count();
            long cancelledRequests = requests.stream().filter(item -> "cancelled".equalsIgnoreCase(item.status())).count();

            String donorLine = donorId == null ? "Доноры: " + donors.size() + "\n" : "";
            summary.setText(
                            donorLine +
                            "Все заявки на донации: " + requests.size() + "\n" +
                            "Заявки со статусом «создано»: " + createdRequests + "\n" +
                            "Заявки со статусом «принято»: " + confirmedRequests + "\n" +
                            "Заявки со статусом «завершено»: " + completedRequests + "\n" +
                            "Заявки со статусом «отклонено»: " + cancelledRequests + "\n" +
                            "Обследования: " + examinations.size() + "\n" +
                            "Подтверждённых: " + accepted + "\n" +
                            "Отклонённых: " + rejected + "\n" +
                            "В ожидании: " + pending + "\n" +
                            "Отменённых: " + cancelled + "\n" +
                            "Донации: " + donations.size() + "\n" +
                            "Всего перелито крови: " + bloodVolume + " мл");
        } catch (SQLException exception) {
            showError("Ошибка статистики", exception.getMessage());
        }
    }

    private void openProfileEditor() {
        Dialog<Donor> dialog = new Dialog<>();
        dialog.setTitle("Изменение профиля");
        dialog.initOwner(stage);
        ButtonType saveType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(
                saveType, new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE));

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(16));
        grid.setHgap(12);
        grid.setVgap(10);

        TextField ageField = new TextField(String.valueOf(currentUser.age()));
        ComboBox<String> genderBox = new ComboBox<>(FXCollections.observableArrayList("male", "female", "other"));
        genderBox.setValue(currentUser.gender());
        TextField weightField = new TextField(String.valueOf(currentUser.weight()));

        grid.add(new Label("Возраст"), 0, 0);
        grid.add(ageField, 1, 0);
        grid.add(new Label("Пол"), 0, 1);
        grid.add(genderBox, 1, 1);
        grid.add(new Label("Вес"), 0, 2);
        grid.add(weightField, 1, 2);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveType) {
                return null;
            }
            try {
                int age = Integer.parseInt(ageField.getText());
                int weight = Integer.parseInt(weightField.getText());
                Donor updated = donorService.updateProfile(currentUser, age, genderBox.getValue(), weight);
                currentUser = updated;
                showInfo("Профиль обновлён", "Изменения сохранены.");
                return updated;
            } catch (BusinessException | SQLException | NumberFormatException exception) {
                showError("Ошибка профиля", exception.getMessage());
                return null;
            }
        });
        dialog.showAndWait();
        showMainScene();
    }

    private void openDonorExaminationEditor(MedicalExamination examination) {
        try {
            DonationRequest request = requestRepository.findById(examination.requestId());
            if (request == null) {
                showError("Изменение обследования", "Заявка на донацию не найдена.");
                return;
            }
            LocalDate minimumDate = request.donationDate();
            DatePicker datePicker = new DatePicker(examination.examinationDate());
            datePicker.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    setDisable(!empty && item != null && item.isBefore(minimumDate));
                }
            });

            Dialog<Boolean> dialog = new Dialog<>();
            dialog.setTitle("Изменение обследования");
            dialog.initOwner(stage);
            ButtonType save = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(
                    save, new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE));
            GridPane grid = new GridPane();
            grid.setPadding(new Insets(16));
            grid.setHgap(12);
            grid.setVgap(10);
            grid.add(new Label("Дата обследования"), 0, 0);
            grid.add(datePicker, 1, 0);
            dialog.getDialogPane().setContent(grid);
            dialog.setResultConverter(button -> {
                if (button != save) {
                    return null;
                }
                try {
                    examinationService.updateDateForDonor(
                            examination.id(), currentUser, datePicker.getValue());
                    return true;
                } catch (BusinessException | SQLException exception) {
                    showError("Ошибка изменения обследования", exception.getMessage());
                    return null;
                }
            });
            dialog.showAndWait().ifPresent(saved -> {
                if (saved) {
                    refreshDonorData();
                    showInfo("Изменение обследования", "Дата обследования изменена.");
                }
            });
        } catch (SQLException exception) {
            showError("Ошибка изменения обследования", exception.getMessage());
        }
    }

    private void openExaminationBookingDialog() {
        Dialog<DonationRequest> dialog = new Dialog<>();
        dialog.setTitle("Запись на медицинское обследование");
        dialog.initOwner(stage);
        ButtonType saveType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(
                saveType, new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE));

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(16));
        grid.setHgap(12);
        grid.setVgap(10);

        List<MedicalExamination> existingExaminations = examinationRows.stream().toList();
        DonationRequest availableRequest = requestRows.stream()
                .filter(item -> "created".equalsIgnoreCase(item.status()))
                .filter(item -> existingExaminations.stream()
                        .filter(examination -> examination.requestId() == item.id())
                        .allMatch(examination ->
                                "cancelled".equalsIgnoreCase(examination.admissionStatus())))
                .findFirst()
                .orElse(null);
        LocalDate minimumDate = availableRequest == null
                ? LocalDate.now() : availableRequest.donationDate();
        DatePicker datePicker = new DatePicker(
                LocalDate.now().isBefore(minimumDate) ? minimumDate : LocalDate.now());
        datePicker.setDayCellFactory(picker -> {
            DateCell cell = new DateCell();
            cell.itemProperty().addListener((observable, oldValue, value) ->
                    cell.setDisable(value != null && value.isBefore(minimumDate)));
            return cell;
        });
        grid.add(new Label("Дата обследования"), 0, 0);
        grid.add(datePicker, 1, 0);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveType) {
                return null;
            }
            try {
                List<MedicalExamination> existing = examinationRepository.findByDonor(currentUser.id());
                List<DonationRequest> active = requestRepository.findByDonor(currentUser.id()).stream()
                        .filter(r -> "created".equalsIgnoreCase(r.status()))
                        .filter(r -> existing.stream()
                                .filter(e -> e.requestId() == r.id())
                                .allMatch(e -> "cancelled".equalsIgnoreCase(e.admissionStatus())))
                        .toList();
                if (active.isEmpty()) {
                    showError("Запись на обследование",
                            "Сначала создайте заявку на донацию со статусом «создано».");
                    return null;
                }
                DonationRequest request = active.get(0);
                if (datePicker.getValue().isBefore(request.donationDate())) {
                    showError("Ошибка даты",
                            "Дата обследования не может быть раньше даты записи на донацию.");
                    return null;
                }
                donationService.bookExamination(currentUser, datePicker.getValue());
                showInfo("Успешно", "Запись на обследование создана.");
                return request;
            } catch (BusinessException | SQLException exception) {
                showError("Ошибка записи", exception.getMessage());
                return null;
            }
        });
        dialog.showAndWait();
        refreshDonorData();
    }

    private void openDonationBookingDialog() {
        Dialog<DonationRequest> dialog = new Dialog<>();
        dialog.setTitle("Запись на донацию");
        dialog.initOwner(stage);
        ButtonType saveType = new ButtonType("Записать", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(
                saveType, new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE));

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(16));
        grid.setHgap(12);
        grid.setVgap(10);
        DatePicker datePicker = new DatePicker(LocalDate.now().plusDays(1));
        grid.add(new Label("Дата донации"), 0, 0);
        grid.add(datePicker, 1, 0);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveType) {
                return null;
            }
            try {
                DonationRequest request = donationService.bookDonation(currentUser, datePicker.getValue());
                showInfo("Успешно", "Заявка на донацию создана. Номер заявки: " + request.id());
                return request;
            } catch (BusinessException | SQLException exception) {
                showError("Ошибка записи", exception.getMessage());
                return null;
            }
        });
        dialog.showAndWait();
        refreshDonorData();
    }

    private void openMedicalExaminationDecision(MedicalExamination examination) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Результат обследования");
        dialog.initOwner(stage);
        ButtonType saveType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(
                saveType, new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE));

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(16));
        grid.setHgap(10);
        grid.setVgap(10);

        TextField hemoglobinField = new TextField();
        hemoglobinField.setPromptText("140.00");
        TextField pressureField = new TextField(examination.bloodPressure() == null ? "" : examination.bloodPressure());
        TextArea conclusionArea = new TextArea(examination.conclusion() == null ? "" : examination.conclusion());
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList(
                "допущен(а)", "не допущен(а)"));
        statusBox.setValue("допущен(а)");

        grid.add(new Label("Гемоглобин"), 0, 0);
        grid.add(hemoglobinField, 1, 0);
        grid.add(new Label("Давление"), 0, 1);
        grid.add(pressureField, 1, 1);
        grid.add(new Label("Заключение"), 0, 2);
        grid.add(conclusionArea, 1, 2);
        grid.add(new Label("Решение"), 0, 3);
        grid.add(statusBox, 1, 3);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveType) {
                return null;
            }
            try {
                BigDecimal value = new BigDecimal(hemoglobinField.getText().trim());
                examinationService.updateResultForDoctor(
                        examination.id(), value, pressureField.getText().trim(), conclusionArea.getText().trim(),
                        examinationStatusValue(statusBox.getValue()));
                showInfo("Результат сохранён", "Заключение по обследованию тщательно обработано.");
                return true;
            } catch (NumberFormatException exception) {
                showError("Ошибка валидации", "Гемоглобин должен быть корректным числом.");
                return false;
            } catch (BusinessException | SQLException exception) {
                showError("Ошибка обработки", exception.getMessage());
                return false;
            }
        });
        dialog.showAndWait();
        refreshDoctorData();
    }

    private void openDonationProcessingDialog() {
        try {
            List<DonationRequest> pendingRequests = requestRepository.findConfirmedWithoutDonation();
            if (pendingRequests.isEmpty()) {
                showInfo("Нет заявок", "Не осталось заявок на обработку.");
                return;
            }
            Dialog<Boolean> dialog = new Dialog<>();
            dialog.setTitle("Обработка донации");
            dialog.initOwner(stage);
            ButtonType saveType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(
                    saveType, new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE));

            GridPane grid = new GridPane();
            grid.setPadding(new Insets(16));
            grid.setHgap(10);
            grid.setVgap(10);

            ComboBox<DonationRequest> requestBox = new ComboBox<>(FXCollections.observableArrayList(pendingRequests));
            requestBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(DonationRequest request) {
                    return request == null ? "" : request.donorName() + " (" + request.bloodGroup()
                            + ") / " + request.donationDate().format(DATE_FORMAT);
                }

                @Override
                public DonationRequest fromString(String string) {
                    return null;
                }
            });
            ComboBox<BloodBatch> batchBox = new ComboBox<>(FXCollections.observableArrayList(
                    batchRepository.findAll().stream()
                            .filter(batch -> "available".equalsIgnoreCase(batch.status()))
                            .toList()));
            ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList("whole_blood", "plasma", "platelets"));
            typeBox.setValue("whole_blood");
            ComboBox<String> resultBox = new ComboBox<>(FXCollections.observableArrayList("успешно", "отмена"));
            resultBox.setValue("успешно");
            Spinner<Integer> volumeSpinner = new Spinner<>(1, 450, 450);

            grid.add(new Label("Заявка"), 0, 0);
            grid.add(requestBox, 1, 0);
            grid.add(new Label("Партия крови"), 0, 1);
            grid.add(batchBox, 1, 1);
            grid.add(new Label("Тип донации"), 0, 2);
            grid.add(typeBox, 1, 2);
            grid.add(new Label("Объём (мл)"), 0, 3);
            grid.add(volumeSpinner, 1, 3);
            grid.add(new Label("Результат"), 0, 4);
            grid.add(resultBox, 1, 4);
            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(buttonType -> {
                if (buttonType != saveType) {
                    return null;
                }
                DonationRequest request = requestBox.getValue();
                BloodBatch batch = batchBox.getValue();
                if (request == null || batch == null) {
                    showError("Ошибка данных", "Необходимо выбрать заявку и партию крови.");
                    return false;
                }
                try {
                    Donor donor = donorRepository.findById(request.donorId());
                    if (donor == null) {
                        showError("Ошибка донации", "Донор заявки не найден.");
                        return false;
                    }
                    MedicalExamination exam = examinationRepository.findByDonor(request.donorId()).stream()
                            .filter(item -> item.requestId() == request.id())
                            .findFirst()
                            .orElse(null);
                    if (exam == null) {
                        showError("Ошибка донации", "Для заявки не найдено обследование.");
                        return false;
                    }
                    donationService.process(request, exam, batch, volumeSpinner.getValue(), typeBox.getValue(),
                            donationResultValue(resultBox.getValue()), donor);
                    showInfo("Успешно", "Донация оформлена и связана с партией крови.");
                    refreshDoctorData();
                    return true;
                } catch (BusinessException | SQLException exception) {
                    showError("Ошибка донации", exception.getMessage());
                    return false;
                }
            });
            dialog.showAndWait();
            refreshDoctorData();
        } catch (SQLException exception) {
            showError("Ошибка данных", exception.getMessage());
        }
    }

    private void refreshDonorData() {
        try {
            donorRows.setAll(donorRepository.findAll());
            requestRows.setAll(requestRepository.findByDonor(currentUser.id()));
            examinationRows.setAll(examinationRepository.findByDonor(currentUser.id()));
            donationRows.setAll(donationRepository.findByDonor(currentUser.id()));
            tableRefreshers.forEach(Runnable::run);
            updateDonorActionButtons(donorExaminationButton, donorDonationButton);
            refreshStatistics();
        } catch (SQLException exception) {
            showError("Ошибка обновления данных", exception.getMessage());
        }
    }

    private void updateDonorActionButtons(Button examinationButton, Button donationButton) {
            try {
                List<DonationRequest> requests = requestRepository.findByDonor(currentUser.id());
                boolean hasActiveRequest = requests.stream().anyMatch(r ->
                        "created".equalsIgnoreCase(r.status()) || "confirmed".equalsIgnoreCase(r.status()));
                boolean hasPendingExamination = examinationRows.stream()
                        .anyMatch(examination -> "pending".equalsIgnoreCase(examination.admissionStatus()));
                boolean canExamine = !hasPendingExamination && requests.stream()
                        .anyMatch(r -> ("created".equalsIgnoreCase(r.status())
                                && examinationRows.stream().noneMatch(e ->
                                e.requestId() == r.id()
                                        && !"cancelled".equalsIgnoreCase(e.admissionStatus())))
                                || ("confirmed".equalsIgnoreCase(r.status())
                                && examinationRows.stream().anyMatch(e ->
                                e.requestId() == r.id()
                                        && "cancelled".equalsIgnoreCase(e.admissionStatus()))));
                if (examinationButton != null) {
                    examinationButton.setDisable(!canExamine);
                }
                if (donationButton != null) {
                    donationButton.setDisable(hasActiveRequest);
                }
            } catch (SQLException exception) {
                showError("Ошибка проверки доступности действий", exception.getMessage());
        }
    }

    private void refreshDoctorData() {
        try {
            donorRows.setAll(donorRepository.findAll());
            requestRows.setAll(requestRepository.findAll());
            examinationRows.setAll(examinationRepository.findAll());
            donationRows.setAll(donationRepository.findAll());
            batchRows.setAll(batchRepository.findAll());
            tableRefreshers.forEach(Runnable::run);
            refreshStatistics();
        } catch (SQLException exception) {
            showError("Ошибка обновления данных", exception.getMessage());
        }
    }

    private void refreshStatistics() {
        statisticsRefreshers.forEach(Runnable::run);
    }

    private void applyExaminationFilter(TableView<MedicalExamination> table, String statusValue,
            LocalDate from, LocalDate to) {
        List<MedicalExamination> filtered = new ArrayList<>();
        for (MedicalExamination item : examinationRows) {
            boolean matchStatus = "Все".equals(statusValue)
                    || statusValue.equals(localizeExaminationStatus(item.admissionStatus()));
            if (matchStatus && inRange(item.examinationDate(), from, to)) {
                filtered.add(item);
            }
        }
        table.setItems(FXCollections.observableArrayList(filtered));
    }

    private void applyDoctorExaminationFilter(TableView<MedicalExamination> table, String text,
            String statusValue, LocalDate from, LocalDate to, String sort) {
        List<MedicalExamination> filtered = new ArrayList<>();
        for (MedicalExamination item : examinationRows) {
            String searchable = item.donorName() + " "
                    + (item.conclusion() == null ? "" : item.conclusion());
            boolean matchStatus = "Все".equals(statusValue)
                    || statusValue.equals(localizeExaminationStatus(item.admissionStatus()));
            boolean matchText = text == null || text.isBlank()
                    || searchable.toLowerCase().contains(text.toLowerCase());
            if (matchStatus && matchText && inRange(item.examinationDate(), from, to)) {
                filtered.add(item);
            }
        }
        filtered.sort("По имени донора".equals(sort)
                ? Comparator.comparing(MedicalExamination::donorName)
                : Comparator.comparing(MedicalExamination::examinationDate));
        table.setItems(FXCollections.observableArrayList(filtered));
    }

    private void applyRequestFilter(TableView<DonationRequest> table, String status,
            LocalDate from, LocalDate to) {
        table.setItems(FXCollections.observableArrayList(requestRows.stream()
                .filter(r -> "Все".equals(status) || status.equals(localizeRequestStatus(r.status())))
                .filter(r -> inRange(r.donationDate(), from, to))
                .toList()));
    }

    private void applyDonationFilter(TableView<Donation> table, String result,
            LocalDate from, LocalDate to, String type) {
        table.setItems(FXCollections.observableArrayList(donationRows.stream()
                .filter(d -> "Все".equals(result) || result.equals(localizeDonationResult(d.result())))
                .filter(d -> "Все".equals(type) || type == null || type.equals(localizeDonationType(d.donationType())))
                .filter(d -> inRange(d.donationDate(), from, to))
                .sorted(Comparator.comparing(Donation::donationDate))
                .toList()));
    }

    private boolean inRange(LocalDate value, LocalDate from, LocalDate to) {
        return value != null && (from == null || !value.isBefore(from))
                && (to == null || !value.isAfter(to));
    }

    private void constrainDateRange(DatePicker from, DatePicker to) {
        // Install each cell factory once. Replacing it while the calendar popup is
        // open can leave DatePickerBehavior with a disposed popup in JavaFX 21.
        to.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                LocalDate start = from.getValue();
                setDisable(!empty && start != null && item != null && item.isBefore(start));
            }
        });
        from.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                LocalDate end = to.getValue();
                setDisable(!empty && end != null && item != null && item.isAfter(end));
            }
        });

        from.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && to.getValue() != null && to.getValue().isBefore(newValue)) {
                to.setValue(newValue);
            }
        });
        to.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && from.getValue() != null && from.getValue().isAfter(newValue)) {
                from.setValue(newValue);
            }
        });
    }

    private String localizeExaminationStatus(String value) {
        return switch (value == null ? "" : value.toLowerCase()) {
            case "pending" -> "на рассмотрении";
            case "accepted" -> "допущен(а)";
            case "rejected" -> "не допущен(а)";
            case "cancelled" -> "отменена";
            default -> value;
        };
    }

    private String examinationStatusValue(String value) {
        return switch (value) {
            case "допущен(а)" -> "accepted";
            case "не допущен(а)" -> "rejected";
            default -> value;
        };
    }

    private String requestStatusValue(String value) {
        return switch (value) {
            case "создано" -> "created";
            case "принято" -> "confirmed";
            case "завершено" -> "completed";
            case "отклонено" -> "cancelled";
            default -> value;
        };
    }

    private String donationResultValue(String value) {
        return switch (value) {
            case "успешно" -> "successful";
            case "отмена" -> "unsuccessful";
            default -> value;
        };
    }

    private String localizeRequestStatus(String value) {
        return switch (value == null ? "" : value.toLowerCase()) {
            case "created" -> "создано";
            case "confirmed" -> "принято";
            case "completed" -> "завершено";
            case "cancelled" -> "отклонено";
            default -> value;
        };
    }

    private String localizeDonationResult(String value) {
        return "successful".equalsIgnoreCase(value) ? "успешно"
                : "unsuccessful".equalsIgnoreCase(value) ? "отмена" : value;
    }

    private String localizeDonationType(String value) {
        return switch (value == null ? "" : value.toLowerCase()) {
            case "whole_blood" -> "цельная кровь";
            case "plasma" -> "плазма";
            case "platelets" -> "тромбоциты";
            default -> value;
        };
    }

    private String localizeGender(String value) {
        return switch (value == null ? "" : value.toLowerCase()) {
            case "male" -> "мужской";
            case "female" -> "женский";
            case "other" -> "другой";
            default -> value;
        };
    }

    private String localizeBatchStatus(String value) {
        return switch (value == null ? "" : value.toLowerCase()) {
            case "available" -> "доступна";
            case "reserved" -> "зарезервирована";
            case "used" -> "использована";
            case "expired" -> "просрочена";
            case "disposed" -> "утилизирована";
            default -> value;
        };
    }

    private VBox labeledControl(String caption, Node control) {
        Label label = new Label(caption);
        label.setWrapText(true);
        label.getStyleClass().add("filter-label");
        VBox container = new VBox(3, label, control);
        container.setAlignment(Pos.BOTTOM_LEFT);
        container.setMinWidth(Region.USE_PREF_SIZE);
        return container;
    }

    private void confirmDelete(Runnable action, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK,
                new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE));
        alert.initOwner(stage);
        alert.showAndWait().ifPresent(button -> {
            if (button == ButtonType.OK) {
                action.run();
            }
        });
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    private void exportStatistics(String format, Integer donorId) {
        try {
            List<List<String>> rows = new ArrayList<>();
            rows.add(List.of("Параметр", "Значение"));
            if (donorId == null) {
                rows.add(List.of("Доноры", String.valueOf(donorRepository.findAll().size())));
            }
            List<DonationRequest> requests = donorId == null
                    ? requestRepository.findAll() : requestRepository.findByDonor(donorId);
            rows.add(List.of("Все заявки на донации", String.valueOf(requests.size())));
            rows.add(List.of("Заявки: создано", String.valueOf(requests.stream().filter(r -> "created".equalsIgnoreCase(r.status())).count())));
            rows.add(List.of("Заявки: принято", String.valueOf(requests.stream().filter(r -> "confirmed".equalsIgnoreCase(r.status())).count())));
            rows.add(List.of("Заявки: завершено", String.valueOf(requests.stream().filter(r -> "completed".equalsIgnoreCase(r.status())).count())));
            rows.add(List.of("Заявки: отклонено", String.valueOf(requests.stream().filter(r -> "cancelled".equalsIgnoreCase(r.status())).count())));
            rows.add(List.of("Обследования", String.valueOf(donorId == null ? examinationRepository.findAll().size() : examinationRepository.findByDonor(donorId).size())));
            rows.add(List.of("Донации", String.valueOf(donorId == null ? donationRepository.findAll().size() : donationRepository.findByDonor(donorId).size())));
            List<MedicalExamination> examinations = donorId == null
                    ? examinationRepository.findAll() : examinationRepository.findByDonor(donorId);
            rows.add(List.of("Обследования: отменено",
                    String.valueOf(examinations.stream()
                            .filter(e -> "cancelled".equalsIgnoreCase(e.admissionStatus())).count())));
            var file = ExcelExporter.export(rows, "statistics", format.equals("csv") ? "1" : "2");
            if (file != null) {
                showInfo("Экспорт", "Файл успешно сохранён: " + file.toString());
            }
        } catch (IOException | SQLException exception) {
            showError("Ошибка экспорта", exception.getMessage());
        }
    }
}
