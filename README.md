# Blood donor console application

Рабочее консольное приложение находится в каталоге `bloodcenter`. Оно использует
пакетную структуру `ru.mirea.project`, PostgreSQL через JDBC и таблицы из
`sql_script/blood_donor.sql`.

## Подготовка базы данных

1. Создайте базу PostgreSQL `blood_donor`.
2. Выполните файл `sql_script/blood_donor.sql`.
3. Проверьте параметры подключения в
   `bloodcenter/src/main/java/ru/mirea/project/db/DatabaseConnection.java`.

Подключение повторяет исходную конфигурацию:

```text
jdbc:postgresql://localhost:5432/blood_donor
user: postgres
```

## Запуск

Из каталога `bloodcenter`:

```powershell
mvn compile
mvn exec:java
```

Если в терминале IntelliJ IDEA русские символы отображаются как `����`,
проверьте настройку **Settings | Editor | General | Console | Default Encoding**:
должно быть выбрано `UTF-8`. Для встроенного терминала PowerShell в настройке
**Settings | Tools | Terminal | Shell path** укажите `powershell.exe`.

Приложение дополнительно принудительно использует UTF-8 для стандартного вывода
и ввода. После изменения кода выполните `mvn clean compile`, чтобы пересобрать
классы.

Консоль поддерживает регистрацию и вход, создание записей на донацию,
медицинские обследования, обработку результатов, просмотр групп крови,
партий и выполненных донаций. Модели и DAO соответствуют таблицам
`blood_group`, `donor`, `donation_request`, `medical_examination`,
`blood_batch` и `donation`.

Пароли сохраняются в `donor.password_hash` как SHA-256-хеш длиной 64 символа.
Тестовые записи в SQL-файле используют пароль `password`; в реальном проекте
следует использовать более стойкий алгоритм с солью, например Argon2id или bcrypt.
