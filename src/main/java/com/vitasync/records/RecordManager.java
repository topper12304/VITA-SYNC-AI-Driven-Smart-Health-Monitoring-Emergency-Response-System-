package com.vitasync.records;

import com.vitasync.exceptions.DatabaseSyncException;
import com.vitasync.model.PatientRecord;
import com.vitasync.model.VitalSignReading;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages patient records with dual storage: ConcurrentHashMap (O(1) access) + MySQL (persistence).
 * All database operations use PreparedStatement to prevent SQL injection.
 * On database failure, HashMap changes are rolled back to maintain consistency.
 */
public class RecordManager implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(RecordManager.class);

    private final ConcurrentHashMap<String, PatientRecord> activeRecords = new ConcurrentHashMap<>();
    private final HikariDataSource dataSource;

    public RecordManager(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Creates a RecordManager with a HikariCP pool from the given JDBC URL.
     */
    public static RecordManager create(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(30_000);
        return new RecordManager(new HikariDataSource(config));
    }

    /**
     * Creates a RecordManager by reading db.properties from the classpath.
     */
    public static RecordManager createFromProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream is = RecordManager.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (is == null) throw new IOException("db.properties not found on classpath");
            props.load(is);
        }
        return create(
            props.getProperty("db.url"),
            props.getProperty("db.username"),
            props.getProperty("db.password")
        );
    }

    /**
     * Creates an in-memory-only RecordManager (no DB). Used when MySQL is unavailable.
     */
    public static RecordManager createInMemoryOnly() {
        return new RecordManager(null);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * O(1) lookup from the in-memory HashMap.
     */
    public PatientRecord getPatientRecord(String patientId) {
        return activeRecords.get(patientId);
    }

    /**
     * Creates a new patient record in both HashMap and database.
     * Rolls back HashMap on database failure.
     */
    public void createPatientRecord(PatientRecord record) throws DatabaseSyncException {
        activeRecords.put(record.getPatientId(), record);
        if (dataSource == null) { log.debug("In-memory mode: skipping DB insert for {}", record.getPatientId()); return; }
        try {
            insertPatientToDb(record);
            log.info("Created patient record: {}", record.getPatientId());
        } catch (SQLException e) {
            activeRecords.remove(record.getPatientId());
            throw new DatabaseSyncException(record.getPatientId(), "create", e);
        }
    }

    /**
     * Updates an existing patient record in both HashMap and database.
     * Rolls back HashMap on database failure.
     */
    public void updatePatientRecord(PatientRecord record) throws DatabaseSyncException {
        PatientRecord previous = activeRecords.get(record.getPatientId());
        activeRecords.put(record.getPatientId(), record);
        if (dataSource == null) return;
        try {
            syncToDatabase(record);
            log.debug("Updated patient record: {}", record.getPatientId());
        } catch (SQLException e) {
            rollbackHashMapChange(record.getPatientId(), previous);
            throw new DatabaseSyncException(record.getPatientId(), "update", e);
        }
    }

    /**
     * Persists a vital sign reading to the history table.
     */
    public void recordVitalReading(String patientId, VitalSignReading reading) throws DatabaseSyncException {
        if (dataSource == null) return;
        try {
            insertVitalHistory(patientId, reading);
        } catch (SQLException e) {
            throw new DatabaseSyncException(patientId, "recordVital", e);
        }
    }

    /**
     * Loads all patients from the database into the HashMap (e.g., on startup).
     */
    public void loadAllFromDatabase() throws DatabaseSyncException {
        String sql = "SELECT patient_id, name, age, current_heart_rate, current_spo2 FROM patients";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                PatientRecord record = new PatientRecord(
                        rs.getString("patient_id"),
                        rs.getString("name"),
                        rs.getInt("age")
                );
                record.updateVitals(rs.getInt("current_heart_rate"), rs.getInt("current_spo2"));
                activeRecords.put(record.getPatientId(), record);
            }
            log.info("Loaded {} patient records from database", activeRecords.size());
        } catch (SQLException e) {
            throw new DatabaseSyncException("ALL", "loadAll", e);
        }
    }

    public List<PatientRecord> getAllActiveRecords() {
        return new ArrayList<>(activeRecords.values());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void insertPatientToDb(PatientRecord record) throws SQLException {
        String sql = "INSERT INTO patients (patient_id, name, age, current_heart_rate, current_spo2) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, record.getPatientId());
            ps.setString(2, record.getName());
            ps.setInt(3, record.getAge());
            ps.setInt(4, record.getCurrentHeartRate());
            ps.setInt(5, record.getCurrentSpO2());
            ps.executeUpdate();
        }
    }

    void syncToDatabase(PatientRecord record) throws SQLException {
        String sql = "UPDATE patients SET name=?, age=?, current_heart_rate=?, current_spo2=?, " +
                     "last_updated=NOW() WHERE patient_id=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, record.getName());
            ps.setInt(2, record.getAge());
            ps.setInt(3, record.getCurrentHeartRate());
            ps.setInt(4, record.getCurrentSpO2());
            ps.setString(5, record.getPatientId());
            ps.executeUpdate();
        }
    }

    private void insertVitalHistory(String patientId, VitalSignReading reading) throws SQLException {
        String sql = "INSERT INTO vital_sign_history (patient_id, heart_rate, spo2, recorded_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, patientId);
            ps.setInt(2, reading.getHeartRate());
            ps.setInt(3, reading.getSpO2());
            ps.setTimestamp(4, Timestamp.valueOf(reading.getTimestamp()));
            ps.executeUpdate();
        }
    }

    private void rollbackHashMapChange(String patientId, PatientRecord previous) {
        if (previous == null) {
            activeRecords.remove(patientId);
        } else {
            activeRecords.put(patientId, previous);
        }
        log.warn("Rolled back HashMap change for patient: {}", patientId);
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
