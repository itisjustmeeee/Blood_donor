1. Как устроен проект

Это MVC-ish JavaFX-приложение для центра крови:

- ui — всё, что связано с окнами, кнопками, табами, FXML, таблицами, событиями
- service — бизнес-логика: проверки, сценарии регистрации, обновления профиля, обработки обследований
- repository — работа с БД через JDBC/PostgreSQL
- model — “сущности” проекта: Donor, Donation, DonationRequest, MedicalExamination, BloodBatch и т.д.
- exception — пользовательские исключения и ошибки домена
- util — подключение к БД, экспорт в Excel/CSV, инициализация тестовых данных
- security — хэширование паролей
- resources — SQL-скрипт, свойства БД, fxml, css

Сами ключевые классы:
- `ru.mirea.project.ui.JavaFxApp` — главное JavaFX-приложение, 
создает окна, вкладки, заполняет данные
- `ru.mirea.project.ui.LoginController` — контроллер формы логина
- `ru.mirea.project.ui.MainController` — контроллер главного окна
- `ru.mirea.project.ui.DoctorView` / `DonorView` — обёртки над табами
- `ru.mirea.project.service.DonorService` — логика доноров
- `ru.mirea.project.repository.DonorRepository` — SQL-операции для доноров
- `ru.mirea.project.util.DatabaseInitializer` — инициализация схемы и данных
- `ru.mirea.project.util.ExcelExporter` — экспорт статистики

2. Какая структура папок и файлов

В `bloodcenter`:
- `src/main/java/ru/mirea/project/...` — Java-классы
- `src/main/resources/...` — FXML, CSS, SQL, database.properties

Примеры:
- `resources/fxml/login.fxml`
- `resources/fxml/main.fxml`
- `resources/styles.css`
- `resources/blood_donor.sql`
- `resources/database.properties`

3. Как запускается JavaFX

Запуск задаётся в `bloodcenter/pom.xml`:
- плагин `org.openjfx:javafx-maven-plugin`
- `mainClass` = `ru.mirea.project.Main`

Команда запуска:
- `mvn clean javafx:run`

Потом JavaFX создает `Application`, запускает 
`start(Stage primaryStage)`, где в `JavaFxApp`:
- устанавливается заголовок окна
- показывается сценка загрузки
- в фоне выполняется `DatabaseInitializer.initialize()`
- если БД пустая, запускается `TestDataSeeder.seed()`
- после этого вызывается `showLoginScene()`

4. Что такое Stage, Scene и Node

- `Stage` — это окно приложения, верхний контейнер. Один `Stage` = одно окно.
- `Scene` — содержимое окна: деревья JavaFX-компонентов. Внутри `Scene` находится корневой узел.
- `Node` — базовый класс для всех визуальных элементов: кнопки, поля, таблицы, панели, лэйауты, текст и т.д.

Пример:
- `stage.setScene(new Scene(root, 920, 600));`
- `root` может быть `BorderPane`, `VBox`, `TabPane` и т.п.

5. Зачем нужен FXML и как он работает

FXML — это XML-описание интерфейса. В проекте это:
- `login.fxml`
- `main.fxml`

Пример:
- `fx:controller="ru.mirea.project.ui.LoginController"`
- `fx:id="emailField"` для связывания XML-элемента с полем в контроллере
- `onAction="#login"` — указывает, что при нажатии кнопки нужно вызвать метод `login()` контроллера

FXML загружается так:
- `FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));`
- `Parent root = loader.load();`
- `loader.<LoginController>getController().setApplication(this);`

То есть FXML не “собирает сам интерфейс” в runtime, а превращается в реальные Java-объекты JavaFX.

6. Как FXML связан с Controller

Связь строится через:
- `fx:controller`
- `fx:id`
- `onAction`
- `@FXML` в контроллере

Пример из `LoginController`:
- `@FXML private TextField emailField;`
- `@FXML private PasswordField passwordField;`
- `@FXML private void login() { application.handleLogin(...); }`

При загрузке FXML JavaFX автоматически заполняет поля, если имена совпадают с `fx:id`. Это называется injection.

7. Как работают таблицы TableView

`TableView<T>` — это таблица с колонками и набором строк.

Пример из `JavaFxApp`:
- `TableView<Donor> table = new TableView<>();`
- `TableColumn<Donor, String> nameCol = new TableColumn<>("ФИО");`
- `nameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().fullName()));`

То есть у каждой колонки есть фабрика значения:
- получает объект строки (`cell.getValue()`)
- возвращает значение конкретного поля

Данные в таблицу кладутся так:
- `table.setItems(donorRows);`
- `donorRows` — `ObservableList<Donor>`

8. Почему используется ObservableList

`ObservableList` — это список, который умеет оповещать JavaFX об изменениях:
- добавили строку
- удалили строку
- изменили запись

Если `TableView` связан с `ObservableList`, UI обновляется автоматически. В проекте это:
- `private final ObservableList<Donor> donorRows = FXCollections.observableArrayList();`
- `private final ObservableList<DonationRequest> requestRows = FXCollections.observableArrayList();`

Когда данные обновляются через `refreshDonorData()`, список обновляется, 
а TableView перерисовывается без ручного “пересоздания сцены”.

9. Как обновляются данные в TableView

В `JavaFxApp` есть методы вроде:
- `refreshDonorData()`
- `refreshDoctorData()`
- `refreshExaminationData()`

Они:
- читают данные из репозиториев
- заполняют `ObservableList`
- затем `table.setItems(...)` или просто список уже связан с таблицей
- иногда список пересоздается через `FXCollections.observableArrayList(filtered)`

Пример фильтрации:
- `search.textProperty().addListener(...)`
- `status.valueProperty().addListener(...)`
- `applyExaminationFilter(...)`
- `table.setItems(FXCollections.observableArrayList(filtered));`

То есть интерфейс реагирует на изменения в полях ввода и списках.

10. Где находится бизнес-логика

В пакете `service`:
- `DonorService`
- `MedicalExaminationService`
- `DonationService`

Например, `DonorService.register(...)` проверяет:
- возраст не меньше 18
- вес не меньше 50
- пол допустимый
- email уникален
- группа крови существует
- роль “donor”
- пароль хэшируется

Это и есть бизнес-правила.

11. Где работает с данными

В `repository`:
- `DonorRepository`
- `DonationRepository`
- `DonationRequestRepository`
- `MedicalExaminationRepository`
- `BloodBatchRepository`

Они:
- открывают соединение через `DatabaseManager`
- формируют SQL
- выполняют `PreparedStatement`
- маппят `ResultSet` в Java-объекты (`record`/классы)

То есть repository = слой доступа к данным, а service = логика.

12. Как обрабатываются исключения

В UI есть методы:
- `showError(String title, String message)`
- `showInfo(String title, String message)`

Они создают `Alert` и показывают пользователю сообщение.

Пример:
- `catch (BusinessException | SQLException exception) { showError("Ошибка...", exception.getMessage()); }`

То есть:
- сервис бросает бизнес-исключения
- репозиторий бросает SQL-исключения
- UI ловит их и показывает модальное окно

13. Как проверяются пользовательские данные

Проверки находятся в сервисах, а не в UI:
- `DonorService.register(...)`
- `DonorService.updateProfile(...)`
- также есть проверки в API для запросов/обследований/донаций

Примеры:
- возраст < 18 — ошибка
- вес < 50 — ошибка
- email уже существует — ошибка
- несуществующая группа крови — ошибка
- некорректный статус/результат — ошибка

То есть пользовательский ввод валидируется на уровне логики, а не только по UI-формам.

14. Как реализован поиск и фильтрация

В `JavaFxApp` для каждой вкладки есть фильтры и слушатели:
- `search.textProperty().addListener(...)`
- `status.valueProperty().addListener(...)`
- `from.valueProperty().addListener(...)`
- `to.valueProperty().addListener(...)`

Пример:
- поиск по имени или группе крови в `buildDonorTableTab()`
- фильтр по статусу обследования `applyExaminationFilter(...)`
- фильтр по результатам донаций `applyDonationFilter(...)`
- сортировка по имени/группе/дате

Всё это превращается в поток:
- взять `donorRows`/`requestRows`
- отфильтровать `.stream().filter(...)`
- отсортировать `.sorted(...)`
- положить в новый `ObservableList`

15. Как рассчитывается статистика

Статистика считается в `StatisticsPane` и в `JavaFxApp` через SQL-репозитории:
- берутся все заявки
- все обследования
- все донации
- считать количество по статусам
- суммарный объём крови
- количество принятия/отклонения/ожидания

Пример:
- `requests.stream().filter(item -> "created".equalsIgnoreCase(item.status())).count()`
- `examinations.stream().filter(item -> "accepted".equalsIgnoreCase(...)).count()`
- `donations.stream().mapToLong(Donation::bloodVolume).sum()`

Это формирует текст / таблицу статистики для интерфейса.

16. Как выполняется экспорт

Экспорт реализован в `util/ExcelExporter.java`.

Он:
- получает `List<List<String>> rows`
- формирует CSV или XLSX
- открывает `JFileChooser`
- спрашивает путь/перезапись файла
- пишет данные через `BufferedWriter` для CSV или `XSSFWorkbook` для Excel

В `JavaFxApp` есть метод `exportStatistics(String format, Integer donorId)`, который:
- собирает таблицу данных
- вызывает `ExcelExporter.export(rows, "statistics", format.equals("csv") ? "1" : "2");`
- показывает `showInfo(...)` после успешного экспорта

Итог: это типичный layered JavaFX-модульный проект:
- FXML + Controller + Model + Service + Repository + JDBC + SQLite-like architecture with PostgreSQL.

# Разбор `JavaFxApp`

Файл:

```text
bloodcenter/src/main/java/ru/mirea/project/ui/JavaFxApp.java
```

`JavaFxApp` — главный класс JavaFX-интерфейса. Он наследуется от `Application` и отвечает сразу за несколько задач:

- запуск JavaFX;
- инициализацию базы данных;
- создание сцен и вкладок;
- обработку кнопок и пользовательских действий;
- загрузку данных из репозиториев;
- фильтрацию таблиц;
- открытие диалоговых окон;
- отображение ошибок;
- построение статистики и запуск экспорта.

Архитектурно это достаточно большой UI-класс, в котором сосредоточено много логики представления. Бизнес-правила при этом вынесены в `Service`, а SQL — в `Repository`.

---

# 1. Поля класса

## Константа формата даты

```java
private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("dd.MM.yyyy");
```

Используется для отображения дат в таблицах и диалогах. Например, дата из `LocalDate` превращается в строку:

```text
23.10.2026
```

---

## Репозитории

```java
private final DonorRepository donorRepository;
private final BloodGroupRepository bloodGroupRepository;
private final DonationRequestRepository requestRepository;
private final MedicalExaminationRepository examinationRepository;
private final BloodBatchRepository batchRepository;
private final DonationRepository donationRepository;
```

Репозитории выполняют непосредственную работу с базой данных:

- ищут пользователей;
- получают заявки;
- загружают обследования;
- получают донации;
- читают партии крови.

`JavaFxApp` вызывает их методы, но сам SQL обычно не содержит.

---

## Сервисы

```java
private final DonorService donorService;
private final DonationService donationService;
private final MedicalExaminationService examinationService;
```

Сервисы содержат прикладные правила:

- можно ли зарегистрировать пользователя;
- можно ли изменить профиль;
- можно ли записаться на обследование;
- можно ли оформить донацию;
- допустимы ли выбранные даты и статусы.

Например:

```java
donationService.bookDonation(currentUser, datePicker.getValue());
```

UI передаёт данные сервису, а сервис проверяет их и изменяет БД через репозитории.

---

## ObservableList для таблиц

```java
private final ObservableList<Donor> donorRows;
private final ObservableList<DonationRequest> requestRows;
private final ObservableList<MedicalExamination> examinationRows;
private final ObservableList<Donation> donationRows;
private final ObservableList<BloodBatch> batchRows;
```

Каждый список связан с определённым типом таблицы:

| Список | Содержимое |
|---|---|
| `donorRows` | доноры |
| `requestRows` | заявки на донацию |
| `examinationRows` | медицинские обследования |
| `donationRows` | донации |
| `batchRows` | партии крови |

Когда список обновляется через:

```java
donorRows.setAll(newData);
```

связанный `TableView` может автоматически обновить отображение.

---

## Списки обновляющих функций

```java
private final List<Runnable> statisticsRefreshers;
private final List<Runnable> tableRefreshers;
```

В них сохраняются функции, которые нужно повторно выполнить после изменения данных.

Например, таблица врачей может иметь активный фильтр. После обновления данных нужно применить этот фильтр заново:

```java
tableRefreshers.forEach(Runnable::run);
```

Аналогично обновляется статистика:

```java
statisticsRefreshers.forEach(Runnable::run);
```

---

## Текущий пользователь и окно

```java
private Stage stage;
private Donor currentUser;
```

- `stage` — главное окно приложения;
- `currentUser` — пользователь, вошедший в систему.

Несмотря на название класса `Donor`, эта модель используется и для врача, потому что врач также хранится в таблице пользователей `donor`, а роль определяется полем `role`.

---

# 2. Запуск приложения

## `start(Stage primaryStage)`

```java
@Override
public void start(Stage primaryStage)
```

Это главный метод жизненного цикла JavaFX-приложения. JavaFX вызывает его автоматически после запуска `Application`.

Метод выполняет следующие действия:

1. сохраняет объект окна:

```java
this.stage = primaryStage;
```

2. устанавливает заголовок:

```java
stage.setTitle("Центр донорства крови");
```

3. задаёт минимальный размер окна;

4. показывает временное окно загрузки:

```java
showLoadingScene();
```

5. создаёт фоновую задачу и инициализирует БД:

```java
DatabaseInitializer.initialize();
```

6. если в базе нет пользователей, добавляет тестовые данные:

```java
if (donorRepository.findAll().isEmpty()) {
    TestDataSeeder.seed();
}
```

7. после успешной инициализации открывает окно авторизации:

```java
initialization.setOnSucceeded(event -> showLoginScene());
```

8. запускает задачу в отдельном daemon-потоке.

Важно: работа с БД не выполняется непосредственно в JavaFX Application Thread, поэтому окно не должно зависать во время подключения.

---

# 3. Работа со сценами

## `showLoadingScene()`

Показывает временную сцену:

```java
VBox loading = new VBox(
        new Label("Подключение к базе данных..."),
        new ProgressIndicator()
);
```

На экран выводятся:

- сообщение о подключении;
- индикатор загрузки.

Затем создаётся `Scene`, которая устанавливается в `Stage`.

---

## `showLoginScene()`

Загружает FXML-файл авторизации:

```java
FXMLLoader loader =
        new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
Parent root = loader.load();
```

После загрузки получает контроллер и передаёт ему ссылку на приложение:

```java
loader.<LoginController>getController().setApplication(this);
```

Затем:

- создаёт сцену размером `460 x 300`;
- подключает `styles.css`;
- устанавливает сцену в окно;
- показывает окно.

Таким образом, `JavaFxApp` управляет сценой, а `LoginController` обрабатывает действия на форме авторизации.

---

## `logout()`

```java
public void logout()
```

Выполняет выход пользователя:

```java
currentUser = null;
showLoginScene();
```

Сначала очищается текущая авторизация, затем показывается окно входа.

Этот метод используется:

- кнопкой выхода в профиле;
- кнопкой выхода из главного контроллера.

---

## `showMainScene()`

Загружает главное окно из:

```text
resources/fxml/main.fxml
```

После загрузки:

```java
loader.<MainController>getController().setApplication(this);
```

Контроллер получает ссылку на `JavaFxApp`, а затем:

- отображает имя пользователя;
- получает нужные вкладки;
- устанавливает их в `TabPane`.

После этого создаётся основная сцена размером `920 x 600`.

---

## `showLegacyLoginScene()` и `showLegacyMainScene()`

Это старые способы создания интерфейса без FXML.

В них интерфейс создаётся напрямую в Java:

```java
VBox root = new VBox(...);
Button loginButton = new Button(...);
```

Сейчас основная версия приложения использует:

- `login.fxml`;
- `main.fxml`;
- контроллеры `LoginController` и `MainController`.

Поэтому методы `showLegacyLoginScene()` и `showLegacyMainScene()` являются совместимостью со старой реализацией и обычно не участвуют в основном сценарии запуска.

---

# 4. Авторизация и регистрация

## `handleLogin(String email, String password)`

Метод вызывается из `LoginController`.

Сначала проверяется, что поля заполнены:

```java
if (email.isBlank() || password.isBlank()) {
    showError(...);
    return;
}
```

Затем вызывается сервис:

```java
Donor donor = donorService.authenticate(email, password);
```

Сервис:

- ищет пользователя по email;
- проверяет пароль;
- возвращает объект `Donor` или `null`.

При успешном входе:

```java
currentUser = donor;
showMainScene();
```

При ошибке показывается `Alert`.

---

## `openRegistrationDialog()`

Открывает модальное окно регистрации.

В нём создаются:

- поле ФИО;
- поле возраста;
- список пола;
- поле веса;
- поле email;
- поле пароля;
- список групп крови.

Группа крови загружается из БД:

```java
bloodGroupRepository.findAll()
```

Для кнопки регистрации добавляется обработчик:

```java
saveButton.addEventFilter(ActionEvent.ACTION, event -> { ... });
```

До закрытия окна проверяется:

- заполнено ли ФИО;
- является ли возраст числом;
- является ли вес числом;
- выбрана ли группа крови.

После этого данные передаются в:

```java
donorService.register(...)
```

Именно сервис выполняет основные бизнес-проверки:

- возраст должен быть не меньше 18 лет;
- вес должен быть не меньше 50 кг;
- email должен быть уникальным;
- пол должен быть допустимым;
- группа крови должна существовать.

После успешной регистрации новый пользователь становится текущим:

```java
currentUser = user;
showMainScene();
```

---

# 5. Формирование вкладок

## `buildCurrentUserTabs()`

Выбирает интерфейс в зависимости от роли:

```java
return "doctor".equalsIgnoreCase(currentUser.role())
        ? new DoctorView(this).create()
        : new DonorView(this).create();
```

Если роль пользователя — `doctor`, создаются врачебные вкладки. Иначе создаются вкладки донора.

---

## `buildDonorTabs()`

Создаёт `TabPane` для донора.

Вкладки:

1. Профиль;
2. Обследования;
3. Запись на донацию;
4. Мои донации;
5. Статистика.

Пример:

```java
Tab profileTab = new Tab("Профиль");
profileTab.setContent(scrollable(buildDonorProfileTab()));
```

Каждая вкладка содержит JavaFX-компонент, обычно `VBox`, обёрнутый в `ScrollPane`.

---

## `buildDoctorTabs()`

Создаёт интерфейс врача.

Вкладки:

1. Профиль;
2. Доноры;
3. Обследования;
4. Заявки на донацию;
5. Донации;
6. Партии крови;
7. Статистика.

Врач получает доступ к общим данным, а не только к собственному профилю.

---

## `getCurrentUser()`

Возвращает текущего авторизованного пользователя:

```java
public Donor getCurrentUser() {
    return currentUser;
}
```

Используется контроллерами:

```java
application.getCurrentUser().fullName()
```

---

# 6. Вспомогательные методы интерфейса

## `scrollable(Node content)`

Оборачивает содержимое вкладки в `ScrollPane`.

Это нужно, чтобы:

- длинные формы прокручивались;
- таблицы и панели помещались в окно;
- содержимое подстраивалось по ширине.

Настройки:

```java
scrollPane.setPannable(true);
scrollPane.setFitToWidth(true);
scrollPane.setFitToHeight(true);
```

---

## `prepareTable(TableView<T> table)`

Настраивает ширину таблицы и её колонок.

Метод:

- отключает стандартное равномерное изменение ширины колонок;
- вычисляет примерную длину самого длинного значения;
- устанавливает ширину колонок;
- ограничивает ширину минимумом `90` и максимумом `360`.

Используется:

```java
prepareTable(table);
```

после создания колонок.

`Platform.runLater(...)` нужен, потому что реальные размеры ячеек и колонок корректно доступны уже после того, как JavaFX обработает текущую операцию построения интерфейса.

---

# 7. Профиль пользователя

## `buildDonorProfileTab()`

Создаёт профиль донора.

На экран выводятся:

- ФИО;
- возраст;
- пол;
- вес;
- группа крови;
- email.

Кнопка `Изменить профиль` вызывает:

```java
openProfileEditor();
```

Кнопка выхода очищает `currentUser` и возвращает пользователя к авторизации.

---

## `buildDoctorProfileTab()`

Создаёт профиль врача.

Отображаются:

- ФИО;
- возраст;
- пол;
- email;
- роль.

В профиле врача доступна кнопка:

```text
Выйти из профиля
```

Она вызывает:

```java
logout();
```

---

## `openProfileEditor()`

Открывает диалог изменения профиля донора.

Изменять можно:

- возраст;
- пол;
- вес.

После нажатия сохранения:

```java
Donor updated = donorService.updateProfile(
        currentUser,
        age,
        gender,
        weight
);
```

Если сервис успешно сохранил данные, `currentUser` заменяется обновлённым объектом:

```java
currentUser = updated;
```

Затем главное окно пересоздаётся через:

```java
showMainScene();
```

---

# 8. Вкладки донора

## `buildDonorExaminationTab()`

Создаёт таблицу обследований донора.

Колонки:

- дата обследования;
- гемоглобин;
- давление;
- статус;
- заключение.

Возможные действия:

- записаться на обследование;
- отменить обследование;
- изменить обследование.

Удаление разрешено только для обследования со статусом `pending`.

При выборе строки проверяется:

```java
boolean pending =
        selected != null
        && "pending".equalsIgnoreCase(selected.admissionStatus());
```

Если запись не ожидает рассмотрения, кнопки редактирования и отмены блокируются.

Фильтрация выполняется по:

- статусу;
- начальной дате;
- конечной дате.

---

## `buildDonationRequestTab()`

Создаёт список заявок донора на донацию.

Колонки:

- дата заявки;
- статус.

Возможности:

- создать заявку;
- отменить заявку;
- фильтровать по статусу;
- фильтровать по диапазону дат.

Отмена возможна только для заявки со статусом `created`.

---

## `buildMyDonationTab()`

Показывает завершённые донации текущего пользователя.

Колонки:

- дата;
- объём;
- тип донации;
- результат.

Есть фильтры:

- результат;
- дата начала;
- дата окончания.

---

# 9. Вкладки врача

## `buildDonorTableTab()`

Создаёт таблицу всех доноров.

Колонки:

- ФИО;
- email;
- группа крови;
- возраст;
- пол;
- вес.

Есть:

- поиск по имени;
- поиск по группе крови;
- сортировка по имени;
- сортировка по группе крови.

Локальная функция `apply` собирает отфильтрованный список:

```java
List<Donor> filtered = donorRows.stream()
        .filter(...)
        .sorted(...)
        .toList();
```

Затем результат устанавливается в таблицу:

```java
table.setItems(
    FXCollections.observableArrayList(filtered)
);
```

Функция добавляется в `tableRefreshers`, чтобы после повторной загрузки данных фильтр применялся заново.

---

## `buildDoctorExaminationTab()`

Создаёт таблицу всех обследований для врача.

Колонки:

- донор;
- дата;
- гемоглобин;
- давление;
- статус;
- заключение.

Врач может выбрать обследование и нажать `Обработать обследование`.

После этого вызывается:

```java
openMedicalExaminationDecision(selected);
```

Поддерживаются:

- поиск по имени донора или заключению;
- фильтрация по статусу;
- фильтрация по датам;
- сортировка по дате;
- сортировка по имени донора.

---

## `buildDoctorRequestTab()`

Создаёт таблицу заявок на донацию для врача.

Колонки:

- донор;
- группа крови;
- дата донации;
- статус.

Врач может:

- искать по имени донора;
- искать по группе крови;
- фильтровать по датам;
- изменять заявку.

Редактирование выполняется через:

```java
openDoctorRequestEditor(selected);
```

---

## `buildDoctorDonationTab()`

Создаёт таблицу всех донаций.

Колонки:

- донор;
- дата;
- объём;
- тип донации;
- результат.

Врач может:

- оформить новую донацию;
- изменить результат существующей;
- фильтровать по результату;
- фильтровать по типу;
- фильтровать по датам.

Методы:

```java
openDonationProcessingDialog();
openDonationResultDialog(selected);
```

---

## `buildBatchTab()`

Создаёт таблицу партий крови.

Колонки:

- номер партии;
- группа крови;
- дата подготовки;
- срок годности;
- объём;
- статус.

Данные берутся из:

```java
batchRows
```

который заполняется в `refreshDoctorData()`.

---

# 10. Диалоги врача

## `openDoctorRequestEditor(DonationRequest request)`

Открывает форму изменения заявки.

Можно изменить:

- дату донации;
- статус.

Перед сохранением проверяется:

```java
date.getValue().isBefore(LocalDate.now())
```

Дата не может быть раньше текущей.

После сохранения вызывается:

```java
requestRepository.update(...)
refreshDoctorData()
```

---

## `openDonationResultDialog(Donation donation)`

Позволяет изменить только результат донации:

- `успешно`;
- `отмена`.

После выбора вызывается:

```java
donationRepository.updateResult(...)
```

---

## `openMedicalExaminationDecision(MedicalExamination examination)`

Открывает диалог принятия решения по обследованию.

Врач выбирает статус и, вероятно, указывает медицинские данные/заключение. После подтверждения вызывается сервис обследований, а затем данные обновляются.

Главная идея метода: UI собирает решение врача, а окончательная проверка и сохранение выполняются через `MedicalExaminationService`.

---

## `openDonationProcessingDialog()`

Открывает форму оформления новой донации.

В ней выбираются:

- заявка;
- партия крови;
- тип донации;
- объём;
- результат.

Перед сохранением проверяется:

- выбрана ли заявка;
- выбрана ли партия;
- существует ли донор;
- существует ли обследование для заявки.

Затем вызывается:

```java
donationService.process(...)
```

Этот сервис связывает донацию:

- с заявкой;
- с обследованием;
- с донором;
- с партией крови.

---

# 11. Статистика

## `buildStatisticsTab(Integer donorId)`

Создаёт панель статистики.

Если `donorId == null`, статистика считается по всей системе, что используется для врача.

Если передан идентификатор донора, статистика считается только для него.

На панели есть кнопки:

- `Экспорт CSV`;
- `Экспорт Excel`.

Информация обновляется через:

```java
updateStatisticsLabel(summary, donorId);
```

Функция обновления добавляется в список:

```java
statisticsRefreshers.add(
    () -> updateStatisticsLabel(summary, donorId)
);
```

---

## `createStatisticsPane(Integer donorId)`

Публичная обёртка над:

```java
buildStatisticsTab(donorId)
```

Нужна, чтобы другие UI-компоненты, например `StatisticsPane`, могли получить готовый блок статистики.

---

## `updateStatisticsLabel(Label summary, Integer donorId)`

Загружает данные из репозиториев и вычисляет:

- количество доноров;
- общее количество заявок;
- количество заявок в каждом статусе;
- количество обследований;
- количество принятых обследований;
- количество отклонённых обследований;
- количество ожидающих обследований;
- количество отменённых обследований;
- количество донаций;
- общий объём перелитой крови.

Пример расчёта:

```java
long bloodVolume = donations.stream()
        .mapToLong(Donation::bloodVolume)
        .sum();
```

Результат записывается в `Label` через `summary.setText(...)`.

---

# 12. Обновление данных

## `refreshDonorData()`

Загружает данные для текущего донора:

```java
donorRows.setAll(donorRepository.findAll());
requestRows.setAll(requestRepository.findByDonor(currentUser.id()));
examinationRows.setAll(examinationRepository.findByDonor(currentUser.id()));
donationRows.setAll(donationRepository.findByDonor(currentUser.id()));
```

После этого:

1. повторно выполняются фильтры таблиц;
2. обновляется доступность кнопок;
3. обновляется статистика.

```java
tableRefreshers.forEach(Runnable::run);
updateDonorActionButtons(...);
refreshStatistics();
```

---

## `refreshDoctorData()`

Загружает все данные для врача:

```java
donorRows.setAll(donorRepository.findAll());
requestRows.setAll(requestRepository.findAll());
examinationRows.setAll(examinationRepository.findAll());
donationRows.setAll(donationRepository.findAll());
batchRows.setAll(batchRepository.findAll());
```

После этого повторно применяются фильтры таблиц и обновляется статистика.

---

## `refreshStatistics()`

Просто запускает все зарегистрированные функции обновления статистики:

```java
statisticsRefreshers.forEach(Runnable::run);
```

Это позволяет обновлять уже отображённые панели после изменения данных.

---

## `updateDonorActionButtons(...)`

Определяет, какие действия доступны донору.

Проверяется:

- есть ли активная заявка;
- есть ли ожидающее обследование;
- можно ли создать новое обследование;
- можно ли создать новую заявку на донацию.

Например, если у донора уже есть активная заявка, кнопка записи на новую донацию блокируется:

```java
donationButton.setDisable(hasActiveRequest);
```

---

# 13. Фильтрация

## `applyExaminationFilter(...)`

Фильтрует обследования донора по:

- статусу;
- диапазону дат.

Результат устанавливается в таблицу через новый `ObservableList`.

---

## `applyDoctorExaminationFilter(...)`

Фильтрует обследования врача по:

- имени донора;
- заключению;
- статусу;
- датам.

Затем сортирует:

```java
Comparator.comparing(MedicalExamination::donorName)
```

или:

```java
Comparator.comparing(MedicalExamination::examinationDate)
```

---

## `applyRequestFilter(...)`

Фильтрует заявки по:

- статусу;
- диапазону дат.

Использует Stream API:

```java
requestRows.stream()
    .filter(...)
    .toList()
```

---

## `applyDonationFilter(...)`

Фильтрует донации по:

- результату;
- типу;
- датам.

Затем сортирует по дате донации.

---

## `inRange(...)`

Проверяет, входит ли дата в выбранный диапазон:

```java
return value != null
        && (from == null || !value.isBefore(from))
        && (to == null || !value.isAfter(to));
```

Логика:

- если начальная дата не указана — нижней границы нет;
- если конечная дата не указана — верхней границы нет;
- дата должна быть не раньше `from`;
- дата должна быть не позже `to`.

---

## `constrainDateRange(...)`

Связывает два `DatePicker`:

- начальную дату;
- конечную дату.

Метод запрещает выбрать:

- в начальном календаре дату позже конечной;
- в конечном календаре дату раньше начальной.

Если пользователь меняет одну дату так, что диапазон становится некорректным, вторая дата автоматически корректируется.

---

# 14. Преобразование статусов

В базе хранятся технические значения на английском:

```text
pending
accepted
rejected
created
confirmed
successful
```

Пользователю нужно показывать русские значения.

## `localizeExaminationStatus(...)`

Преобразует:

```text
pending   -> на рассмотрении
accepted  -> допущен(а)
rejected  -> не допущен(а)
cancelled -> отменена
```

## `examinationStatusValue(...)`

Делает обратное преобразование:

```text
допущен(а)       -> accepted
не допущен(а)    -> rejected
```

Аналогично работают:

- `localizeRequestStatus`;
- `requestStatusValue`;
- `localizeDonationResult`;
- `donationResultValue`;
- `localizeDonationType`;
- `localizeGender`;
- `localizeBatchStatus`.

Это разделяет внутренние значения БД и пользовательские подписи интерфейса.

---

# 15. Вспомогательные элементы интерфейса

## `labeledControl(String caption, Node control)`

Создаёт блок:

```text
Подпись
[элемент управления]
```

Например:

```java
labeledControl("Статус", status)
```

создаёт вертикальный `VBox` из `Label` и `ComboBox`.

Это используется в панелях фильтров.

---

## `confirmDelete(Runnable action, String message)`

Показывает диалог подтверждения.

Если пользователь нажал `OK`, выполняется переданное действие:

```java
action.run();
```

Например, действие удаления может быть передано так:

```java
confirmDelete(() -> {
    examinationService.deleteForDonor(...);
}, "Отменить запись?");
```

---

## `showError(...)`

Показывает окно ошибки:

```java
Alert.AlertType.ERROR
```

Используется для:

- SQL-ошибок;
- ошибок валидации;
- ошибок сохранения;
- ошибок загрузки интерфейса.

---

## `showInfo(...)`

Показывает информационное окно:

```java
Alert.AlertType.INFORMATION
```

Используется после успешных операций:

- регистрация;
- сохранение профиля;
- создание заявки;
- оформление донации;
- экспорт.

---

# 16. Экспорт статистики

## `exportStatistics(String format, Integer donorId)`

Сначала формируется список строк:

```java
List<List<String>> rows
```

Первая строка содержит заголовки:

```text
Параметр | Значение
```

Затем добавляются показатели:

- количество доноров;
- заявки по статусам;
- количество обследований;
- количество донаций;
- отменённые обследования.

Далее вызывается:

```java
ExcelExporter.export(
    rows,
    "statistics",
    format.equals("csv") ? "1" : "2"
);
```

`ExcelExporter` создаёт:

- CSV, если передан формат `csv`;
- XLSX, если передан другой формат.

После успешного сохранения показывается путь к файлу.

---

# 17. Общий жизненный цикл

В упрощённом виде работа класса выглядит так:

```text
Main
  ↓
JavaFX вызывает start(Stage)
  ↓
showLoadingScene()
  ↓
DatabaseInitializer.initialize()
  ↓
showLoginScene()
  ↓
LoginController.handleLogin()
  ↓
JavaFxApp.handleLogin()
  ↓
DonorService.authenticate()
  ↓
currentUser = найденный пользователь
  ↓
showMainScene()
  ↓
MainController.setApplication()
  ↓
buildCurrentUserTabs()
  ↓
buildDonorTabs() или buildDoctorTabs()
  ↓
refreshDonorData() или refreshDoctorData()
  ↓
TableView отображает ObservableList
```

После любого действия пользователя цепочка обычно выглядит так:

```text
Кнопка/поле интерфейса
  ↓
обработчик события
  ↓
метод JavaFxApp
  ↓
Service или Repository
  ↓
изменение БД
  ↓
refresh...
  ↓
ObservableList.setAll(...)
  ↓
обновление TableView
```

---

# 18. Роль `JavaFxApp` в архитектуре

`JavaFxApp` является центральным координатором UI:

- контролирует окна;
- знает, какой пользователь вошёл;
- создаёт вкладки;
- подключает обработчики;
- вызывает сервисы;
- загружает данные;
- отображает ошибки.

Но класс получился довольно крупным: в нём находятся и создание интерфейса, и обработка событий, и координация обновления данных.

Более строгая архитектура могла бы разделить его на отдельные контроллеры:

```text
LoginController
DonorProfileController
DoctorDonorsController
DoctorExaminationsController
StatisticsController
```

Однако в текущем проекте `JavaFxApp` выполняет роль главного UI-фасада, 
а классы `DoctorView`, `DonorView`, `MainController` лишь 
делегируют ему построение отдельных частей интерфейса.
