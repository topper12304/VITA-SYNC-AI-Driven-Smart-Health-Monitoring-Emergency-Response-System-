package com.vitasync.records;

import com.vitasync.exceptions.DatabaseSyncException;
import com.vitasync.model.PatientRecord;
import com.vitasync.model.VitalSignReading;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RecordManager using H2 in-memory database.
 * Schema mirrors schema.sql but uses H2-compatible syntax.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RecordManagerIntegrationTest {

    private static HikariDataSource dataSource;
    private static RecordManager manager;

    @BeforeAll
    static void setUpDb() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:vitasync_test;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(5);
        dataSource = new HikariDataSource(config);

        // Create schema
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS patients (
                    patient_id    VARCHAR(50)  NOT NULL PRIMARY KEY,
                    name          VARCHAR(100) NOT NULL,
                    age           INT          NOT NULL,
                    current_heart_rate INT,
                    current_spo2  INT,
                    last_updated  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
                )""");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS vital_sign_history (
                    id          BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    patient_id  VARCHAR(50) NOT NULL,
                    heart_rate  INT NOT NULL,
                    spo2        INT NOT NULL,
                    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
                )""");
        }
        manager = new RecordManager(dataSource);
    }

    @AfterAll
    static void tearDown() {
        if (dataSource != null) dataSource.close();
    }

    @Test
    @Order(1)
    void createPatientPersistsToDb() throws DatabaseSyncException {
        PatientRecord record = new PatientRecord("P001", "Alice", 30);
        record.updateVitals(75, 98);
        manager.createPatientRecord(record);

        PatientRecord fetched = manager.getPatientRecord("P001");
        assertNotNull(fetched);
        assertEquals("Alice", fetched.getName());
    }

    @Test
    @Order(2)
    void updatePatientSyncsToDb() throws DatabaseSyncException {
        PatientRecord record = manager.getPatientRecord("P001");
        record.updateVitals(90, 95);
        manager.updatePatientRecord(record);

        PatientRecord updated = manager.getPatientRecord("P001");
        assertEquals(90, updated.getCurrentHeartRate());
        assertEquals(95, updated.getCurrentSpO2());
    }

    @Test
    @Order(3)
    void recordVitalReadingInsertsHistory() throws DatabaseSyncException {
        VitalSignReading reading = new VitalSignReading(80, 97);
        assertDoesNotThrow(() -> manager.recordVitalReading("P001", reading));
    }

    @Test
    @Order(4)
    void loadAllFromDatabasePopulatesHashMap() throws DatabaseSyncException {
        // Add another patient directly
        PatientRecord p2 = new PatientRecord("P002", "Bob", 45);
        p2.updateVitals(65, 99);
        manager.createPatientRecord(p2);

        RecordManager freshManager = new RecordManager(dataSource);
        freshManager.loadAllFromDatabase();
        assertTrue(freshManager.getAllActiveRecords().size() >= 2);
    }

    @Test
    @Order(5)
    void inMemoryOnlyModeDoesNotThrow() throws DatabaseSyncException {
        RecordManager memOnly = RecordManager.createInMemoryOnly();
        PatientRecord rec = new PatientRecord("P999", "Test", 20);
        rec.updateVitals(70, 96);
        assertDoesNotThrow(() -> memOnly.createPatientRecord(rec));
        assertDoesNotThrow(() -> memOnly.updatePatientRecord(rec));
        assertNotNull(memOnly.getPatientRecord("P999"));
        memOnly.close();
    }

    @Test
    @Order(6)
    void getAllActiveRecordsReturnsAll() {
        assertFalse(manager.getAllActiveRecords().isEmpty());
    }
}
