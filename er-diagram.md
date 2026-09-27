# ER-диаграмма базы данных

Диаграмма соответствует текущей схеме из `sql_script/blood_donor.sql`.

```mermaid
erDiagram

    BLOOD_GROUP ||--o{ DONOR : "имеет"
    BLOOD_GROUP ||--o{ BLOOD_BATCH : "для группы"

    DONOR ||--o{ DONATION_REQUEST : "создаёт"

    DONATION_REQUEST ||--o| MEDICAL_EXAMINATION : "проходит"
    MEDICAL_EXAMINATION ||--o| DONATION : "используется в"

    BLOOD_BATCH ||--o{ DONATION : "содержит"

    BLOOD_GROUP {
        int blood_group_id PK
        varchar blood_type
        char rh_factor
    }

    DONOR {
        int donor_id PK
        varchar full_name
        int age
        user_role role
        gender gender
        int weight
        varchar email UK
        varchar password_hash
        int blood_group_id FK
    }

    DONATION_REQUEST {
        int request_id PK
        int donor_id FK
        date donation_date
        request_status request_status
    }

    MEDICAL_EXAMINATION {
        int examination_id PK
        int request_id FK
        date examination_date
        decimal hemoglobin
        varchar blood_pressure
        varchar conclusion
        admission_status admission_status
    }

    BLOOD_BATCH {
        int batch_id PK
        int blood_group_id FK
        varchar batch_number UK
        date preparation_date
        date expiration_date
        int total_volume
        batch_status status
    }

    DONATION {
        int donation_id PK
        int examination_id FK
        int batch_id FK
        date donation_date
        int blood_volume
        donation_type donation_type
        donation_result result
    }
```

Ограничение `UNIQUE` на `medical_examination.request_id` означает, что у одной заявки может быть не более одного обследования.<br>
Ограничение `UNIQUE` на `donation.examination_id` реализует правило «одно положительное обследование — одна донация».
