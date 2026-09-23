/*
CREATE TYPE user_role AS ENUM (
	'Донор',
	'Врач'
);

CREATE TYPE gender AS ENUM (
	'мужской',
	'женский'
);

CREATE TYPE admission_status AS ENUM (
	'допущен',
	'не допущен'
);

CREATE TYPE request_status AS ENUM (
	'created',
	'confirmed',
	'completed',
	'cancelled'
);

CREATE TYPE donation_type AS ENUM (
	'whole_blood',
	'plasma',
	'platelets'
);

CREATE TYPE batch_status AS ENUM (
	'available',
	'reserved',
	'used',
	'expired',
	'disposed'
);

CREATE TYPE donation_result AS ENUM (
	'successful',
	'unsuccessful'
);

CREATE TABLE blood_group (
	blood_group_id SERIAL PRIMARY KEY,
	blood_type VARCHAR(2) NOT NULL,
	rh_factor CHAR(1) NOT NULL,

	CONSTRAINT chk_blood_type
		CHECK (blood_type IN ('A', 'B', 'AB', '0')),

	CONSTRAINT chk_rh_factor
		CHECK (rh_factor IN ('+', '-')),

	CONSTRAINT uq_blood_group
		UNIQUE (blood_type, rh_factor)
);

CREATE TABLE donor (
	donor_id SERIAL PRIMARY KEY,
	full_name VARCHAR(150) NOT NULL,
	birth_date DATE NOT NULL,
	role user_role NOT NULL,
	gender gender NOT NULL,
	weight INT NOT NULL,
	email VARCHAR(100) NOT NULL UNIQUE,
	phone VARCHAR(20) NOT NULL UNIQUE,
	blood_group_id INT NOT NULL,

	CONSTRAINT fk_donor_blood_group
		FOREIGN KEY (blood_group_id)
		REFERENCES blood_group(blood_group_id),

	CONSTRAINT chk_donor_weight
		CHECK (weight > 0),

	CONSTRAINT chk_donor_birth_date
		CHECK (birth_date < CURRENT_DATE)
);

CREATE TABLE donation_request (
	request_id SERIAL PRIMARY KEY,
	donor_id INT NOT NULL,
	donation_date DATE NOT NULL,
	request_status request_status NOT NULL,

	CONSTRAINT fk_request_donor
		FOREIGN KEY (donor_id)
		REFERENCES donor(donor_id)
);

CREATE TABLE medical_examination (
	examination_id SERIAL PRIMARY KEY,
	request_id INT NOT NULL UNIQUE,
	examination_date DATE NOT NULL,
	hemoglobin DECIMAL (5, 2),
	blood_pressure VARCHAR(20),
	conclusion VARCHAR(200),
	admission_status admission_status NOT NULL,

	CONSTRAINT fk_examination_request
		FOREIGN KEY (request_id)
		REFERENCES donation_request(request_id),

	CONSTRAINT chk_hemoglobin
		CHECK (hemoglobin IS NULL OR hemoglobin > 0)
);

CREATE TABLE blood_batch (
	batch_id SERIAL PRIMARY KEY,
	blood_group_id INT NOT NULL,
	batch_number VARCHAR(50) NOT NULL UNIQUE,
	preparation_date DATE NOT NULL,
	expiration_date DATE NOT NULL,
	total_volume INT NOT NULL,
	status batch_status NOT NULL,

	CONSTRAINT fk_batch_blood_group
		FOREIGN KEY (blood_group_id)
		REFERENCES blood_group(blood_group_id),

	CONSTRAINT chk_batch_volume
		CHECK (total_volume > 0),

	CONSTRAINT chk_batch_dates
		CHECK (expiration_date > preparation_date)
);

CREATE TABLE donation (
	donation_id SERIAL PRIMARY KEY,
	examination_id INT NOT NULL UNIQUE,
	batch_id INT NOT NULL,
	donation_date DATE NOT NULL,
	blood_volume INT NOT NULL,
	donation_type donation_type NOT NULL,
	result donation_result NOT NULL,

	CONSTRAINT fk_donation_examination
		FOREIGN KEY (examination_id)
		REFERENCES medical_examination(examination_id),

	CONSTRAINT fk_donation_batch
		FOREIGN KEY (batch_id)
		REFERENCES blood_batch(batch_id),

	CONSTRAINT chk_donation_volume
		CHECK (blood_volume > 0)
);
*/
-- вставка значений (тестовые)
/*
INSERT INTO blood_group (blood_type, rh_factor)
VALUES
    ('0', '+'),
    ('0', '-'),
    ('A', '+'),
    ('A', '-'),
    ('B', '+'),
    ('B', '-'),
    ('AB', '+'),
    ('AB', '-');
*/
/*
INSERT INTO donor
    (full_name, birth_date, role, gender, weight, email, phone, blood_group_id)
VALUES
    ('Иванов Иван Иванович', '1995-03-12', 'Донор', 'мужской', 82,
     'ivanov@example.com', '+79990000001', 3),

    ('Петрова Анна Сергеевна', '1998-07-25', 'Донор', 'женский', 64,
     'petrova@example.com', '+79990000002', 1),

    ('Сидоров Алексей Павлович', '1992-11-08', 'Донор', 'мужской', 90,
     'sidorov@example.com', '+79990000003', 3),

    ('Кузнецова Мария Андреевна', '2000-01-17', 'Донор', 'женский', 58,
     'kuznetsova@example.com', '+79990000004', 5),

    ('Смирнов Дмитрий Олегович', '1989-06-30', 'Донор', 'мужской', 76,
     'smirnov@example.com', '+79990000005', 1),

    ('Волкова Елена Игоревна', '1996-09-14', 'Донор', 'женский', 61,
     'volkova@example.com', '+79990000006', 7),

    ('Орлов Максим Романович', '1994-02-21', 'Донор', 'мужской', 85,
     'orlov@example.com', '+79990000007', 3),

    ('Морозова Ольга Викторовна', '1999-12-05', 'Донор', 'женский', 63,
     'morozova@example.com', '+79990000008', 5);
*/
/*
INSERT INTO donation_request
    (donor_id, donation_date, request_status)
VALUES
    (1, '2026-09-20', 'completed'),
    (2, '2026-09-20', 'completed'),
    (3, '2026-09-20', 'completed'),
    (4, '2026-09-21', 'completed'),
    (5, '2026-09-21', 'completed'),
    (6, '2026-09-22', 'completed'),
    (7, '2026-09-22', 'completed'),
    (8, '2026-09-23', 'confirmed');
*/
/*
INSERT INTO medical_examination
    (request_id, examination_date, hemoglobin, blood_pressure,
     conclusion, admission_status)
VALUES
    (1, '2026-09-20', 145.00, '120/80',
     'Противопоказаний не выявлено', 'допущен'),

    (2, '2026-09-20', 138.00, '118/76',
     'Противопоказаний не выявлено', 'допущен'),

    (3, '2026-09-20', 152.00, '125/82',
     'Противопоказаний не выявлено', 'допущен'),

    (4, '2026-09-21', 141.00, '115/75',
     'Противопоказаний не выявлено', 'допущен'),

    (5, '2026-09-21', 136.00, '122/80',
     'Противопоказаний не выявлено', 'допущен'),

    (6, '2026-09-22', 132.00, '120/78',
     'Противопоказаний не выявлено', 'допущен'),

    (7, '2026-09-22', 148.00, '126/80',
     'Противопоказаний не выявлено', 'допущен'),

    (8, '2026-09-23', 110.00, '118/75',
     'Необходимо повторное обследование', 'не допущен');
*/
/*
INSERT INTO blood_batch
    (blood_group_id, batch_number, preparation_date,
     expiration_date, total_volume, status)
VALUES
    (3, 'A20260920-001', '2026-09-20', '2026-10-20', 900, 'available'),

    (1, 'O20260920-001', '2026-09-20', '2026-10-20', 900, 'available'),

    (5, 'B20260921-001', '2026-09-21', '2026-10-21', 450, 'available'),

    (7, 'AB20260922-001', '2026-09-22', '2026-10-22', 450, 'available');
*/
/*
INSERT INTO donation
    (examination_id, batch_id, donation_date,
     blood_volume, donation_type, result)
VALUES
    (1, 1, '2026-09-20', 450, 'whole_blood', 'successful'),

    (3, 1, '2026-09-20', 450, 'whole_blood', 'successful'),

    (2, 2, '2026-09-20', 450, 'whole_blood', 'successful'),

    (5, 2, '2026-09-21', 450, 'whole_blood', 'successful'),

    (4, 3, '2026-09-21', 450, 'whole_blood', 'successful'),

    (6, 4, '2026-09-22', 450, 'whole_blood', 'successful');
*/
-- проверка связки таблиц (запросы)
-- донации с донорами
/*
SELECT
    d.donation_id,
    dn.full_name AS donor,
    bg.blood_type || bg.rh_factor AS blood_group,
    d.donation_date,
    d.blood_volume,
    d.donation_type,
    d.result
FROM donation d
JOIN medical_examination me
    ON d.examination_id = me.examination_id
JOIN donation_request dr
    ON me.request_id = dr.request_id
JOIN donor dn
    ON dr.donor_id = dn.donor_id
JOIN blood_group bg
    ON dn.blood_group_id = bg.blood_group_id;
*/
-- партии крови
/*
SELECT
    b.batch_number,
    bg.blood_type || bg.rh_factor AS blood_group,
    COUNT(d.donation_id) AS donation_count,
    SUM(d.blood_volume) AS calculated_volume,
    b.total_volume
FROM blood_batch b
JOIN blood_group bg
    ON b.blood_group_id = bg.blood_group_id
LEFT JOIN donation d
    ON b.batch_id = d.batch_id
GROUP BY
    b.batch_id,
    b.batch_number,
    bg.blood_type,
    bg.rh_factor,
    b.total_volume
ORDER BY b.batch_number;
*/
-- те, кто не допущен
/*
SELECT
    dn.full_name,
    me.examination_date,
    me.hemoglobin,
    me.admission_status,
    me.conclusion
FROM medical_examination me
JOIN donation_request dr
    ON me.request_id = dr.request_id
JOIN donor dn
    ON dr.donor_id = dn.donor_id
WHERE me.admission_status = 'не допущен';
*/