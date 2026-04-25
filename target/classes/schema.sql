-- VITA-SYNC Database Schema

CREATE DATABASE IF NOT EXISTS vitasync;
USE vitasync;

CREATE TABLE IF NOT EXISTS patients (
    patient_id    VARCHAR(50)  NOT NULL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    age           INT          NOT NULL,
    current_heart_rate INT,
    current_spo2  INT,
    last_updated  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vital_sign_history (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    patient_id    VARCHAR(50)  NOT NULL,
    heart_rate    INT          NOT NULL,
    spo2          INT          NOT NULL,
    recorded_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vital_history_patient ON vital_sign_history(patient_id);
