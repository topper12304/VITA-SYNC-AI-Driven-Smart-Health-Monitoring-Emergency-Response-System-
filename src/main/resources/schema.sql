-- ============================================================
-- VITA-SYNC + VITAL-CONNECT — Integrated Hospital Database
-- Combines: Real-time Monitoring + Emergency Management
-- ============================================================

CREATE DATABASE IF NOT EXISTS hospital_db;
USE hospital_db;

-- 1. PATIENTS TABLE
CREATE TABLE IF NOT EXISTS patients (
    patient_id         VARCHAR(50)  NOT NULL PRIMARY KEY,
    name               VARCHAR(100) NOT NULL,
    age                INT          NOT NULL,
    current_heart_rate INT          DEFAULT 0,
    current_spo2       INT          DEFAULT 0,
    last_updated       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 2. VITAL SIGN HISTORY (every reading stored for analytics)
CREATE TABLE IF NOT EXISTS vital_sign_history (
    id          BIGINT    NOT NULL AUTO_INCREMENT PRIMARY KEY,
    patient_id  VARCHAR(50) NOT NULL,
    heart_rate  INT         NOT NULL,
    spo2        INT         NOT NULL,
    recorded_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

-- 3. BEDS TABLE
CREATE TABLE IF NOT EXISTS beds (
    bed_id     VARCHAR(20) NOT NULL PRIMARY KEY,
    status     ENUM('FREE','OCCUPIED','MAINTENANCE') DEFAULT 'FREE',
    patient_id VARCHAR(50) UNIQUE,
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE SET NULL
);

-- 4. EMERGENCY REQUESTS (VITA-SYNC → VITAL-CONNECT bridge)
CREATE TABLE IF NOT EXISTS emergency_requests (
    request_id      INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    patient_id      VARCHAR(50)  NOT NULL,
    priority        ENUM('LOW','MODERATE','HIGH','CRITICAL') DEFAULT 'MODERATE',
    description     TEXT,
    status          ENUM('PENDING','DISPATCHED','RESOLVED') DEFAULT 'PENDING',
    assigned_doctor VARCHAR(100) DEFAULT 'Awaiting Assignment',
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

-- 5. DOCTORS TABLE (from VITAL-CONNECT — Hash Table equivalent)
CREATE TABLE IF NOT EXISTS doctors (
    id                  INT          NOT NULL PRIMARY KEY,
    name                VARCHAR(100) NOT NULL,
    specialization      VARCHAR(100) NOT NULL,
    username            VARCHAR(50)  NOT NULL UNIQUE,
    password_hash       VARCHAR(64)  NOT NULL,   -- SHA-256 hex
    available           BOOLEAN      DEFAULT TRUE,
    assigned_patient_id VARCHAR(50)  DEFAULT NULL
);

-- 6. INVENTORY TABLE (from VITAL-CONNECT — BST/TreeMap equivalent)
CREATE TABLE IF NOT EXISTS inventory (
    item_id  INT          NOT NULL PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    quantity INT          NOT NULL DEFAULT 0,
    category VARCHAR(50)  DEFAULT 'General'
);

-- ---- INDEXES ----
CREATE INDEX idx_vital_patient   ON vital_sign_history(patient_id);
CREATE INDEX idx_er_patient      ON emergency_requests(patient_id);
CREATE INDEX idx_er_status       ON emergency_requests(status);
CREATE INDEX idx_doctor_username ON doctors(username);

-- ---- SEED DATA: BEDS ----
INSERT IGNORE INTO beds (bed_id, status) VALUES
('BED-01','FREE'),('BED-02','FREE'),('BED-03','FREE'),('BED-04','FREE'),('BED-05','FREE'),
('BED-06','FREE'),('BED-07','FREE'),('BED-08','FREE'),('BED-09','FREE'),('BED-10','FREE'),
('BED-11','FREE'),('BED-12','FREE'),('BED-13','FREE'),('BED-14','FREE'),('BED-15','FREE'),
('BED-16','FREE'),('BED-17','FREE'),('BED-18','FREE'),('BED-19','FREE'),('BED-20','FREE');

-- ---- SEED DATA: DOCTORS (passwords are SHA-256 of the plain text shown) ----
-- pramod123, aman123, mohit123, ananya123, diwakar123
INSERT IGNORE INTO doctors (id, name, specialization, username, password_hash, available) VALUES
(1, 'Dr. Pramod Kumar',   'Emergency Medicine', 'pramod',  SHA2('pramod123',  256), TRUE),
(2, 'Dr. Aman Singh',     'Cardiology',         'aman',    SHA2('aman123',    256), TRUE),
(3, 'Dr. Mohit Chandra',  'Pulmonology',        'mohit',   SHA2('mohit123',   256), TRUE),
(4, 'Dr. Ananya Sharma',  'Neurology',          'ananya',  SHA2('ananya123',  256), TRUE),
(5, 'Dr. Diwakar Pandey', 'General Surgery',    'diwakar', SHA2('diwakar123', 256), TRUE);

-- ---- SEED DATA: INVENTORY ----
INSERT IGNORE INTO inventory (item_id, name, quantity, category) VALUES
(101, 'Paracetamol 500mg',    200, 'Medicine'),
(102, 'Aspirin 75mg',         150, 'Medicine'),
(103, 'Oxygen Cylinder',       10, 'Equipment'),
(104, 'IV Saline 500ml',       80, 'Consumable'),
(105, 'Surgical Gloves (box)', 30, 'Consumable'),
(106, 'Morphine 10mg',         25, 'Medicine'),
(107, 'Adrenaline 1mg/ml',     15, 'Medicine'),
(108, 'Defibrillator Pads',     8, 'Equipment'),
(109, 'Bandage Roll',          100, 'Consumable'),
(110, 'Antiseptic Solution',   50, 'Medicine');
