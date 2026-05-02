package com.vitasync.management;

import com.vitasync.model.Doctor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Manages doctor records using a Hash Table (ConcurrentHashMap) for O(1) lookup
 * and MySQL for persistence. Supports login authentication.
 *
 * Data Structure: ConcurrentHashMap<username, Doctor> — mirrors the C Hash Table from VITAL-CONNECT.
 */
public class DoctorManager {

    private static final Logger log = LoggerFactory.getLogger(DoctorManager.class);

    // Hash Table: username → Doctor (O(1) lookup for login)
    private final ConcurrentHashMap<String, Doctor> doctorTable = new ConcurrentHashMap<>();
    // Secondary index: id → Doctor
    private final ConcurrentHashMap<Integer, Doctor> idIndex = new ConcurrentHashMap<>();

    private final HikariDataSource dataSource;

    public DoctorManager(HikariDataSource dataSource) {
        this.dataSource = dataSource;
        loadFromDatabase();
    }

    /** Constructor for in-memory-only mode (no DB). */
    public DoctorManager() {
        this.dataSource = null;
        seedDefaultDoctors();
    }

    // -------------------------------------------------------------------------
    // Authentication
    // -------------------------------------------------------------------------

    /**
     * Authenticates a doctor by username and plain-text password.
     * Returns the Doctor if credentials match, empty otherwise.
     */
    public Optional<Doctor> authenticate(String username, String plainPassword) {
        Doctor doctor = doctorTable.get(username.toLowerCase());
        if (doctor == null) return Optional.empty();
        String hash = sha256(plainPassword);
        if (hash.equals(doctor.getPasswordHash())) {
            log.info("Doctor '{}' authenticated successfully.", username);
            return Optional.of(doctor);
        }
        log.warn("Failed login attempt for username '{}'.", username);
        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    /**
     * Adds a new doctor to the hash table and database.
     */
    public void addDoctor(int id, String name, String specialization,
                          String username, String plainPassword) {
        String hash = sha256(plainPassword);
        Doctor doctor = new Doctor(id, name, specialization, username.toLowerCase(), hash);
        doctorTable.put(username.toLowerCase(), doctor);
        idIndex.put(id, doctor);
        persistToDb(doctor);
        log.info("Doctor added: {}", doctor);
    }

    /** Lookup by username (O(1) hash table). */
    public Optional<Doctor> findByUsername(String username) {
        return Optional.ofNullable(doctorTable.get(username.toLowerCase()));
    }

    /** Lookup by doctor ID. */
    public Optional<Doctor> findById(int id) {
        return Optional.ofNullable(idIndex.get(id));
    }

    /** Returns all doctors sorted by ID. */
    public List<Doctor> getAllDoctors() {
        return idIndex.values().stream()
                .sorted((a, b) -> Integer.compare(a.getId(), b.getId()))
                .toList();
    }

    /** Returns all available (unassigned) doctors. */
    public List<Doctor> getAvailableDoctors() {
        return doctorTable.values().stream()
                .filter(Doctor::isAvailable)
                .toList();
    }

    /**
     * Assigns the first available doctor to a patient.
     * Returns the assigned doctor, or empty if none available.
     */
    public Optional<Doctor> assignDoctorToPatient(String patientId) {
        Optional<Doctor> available = getAvailableDoctors().stream().findFirst();
        available.ifPresent(doc -> {
            doc.assignToPatient(patientId);
            updateAvailabilityInDb(doc);
            log.info("Doctor '{}' assigned to patient '{}'", doc.getName(), patientId);
        });
        return available;
    }

    /**
     * Releases a doctor from their current patient assignment.
     */
    public void releaseDoctor(int doctorId) {
        Doctor doc = idIndex.get(doctorId);
        if (doc != null) {
            doc.release();
            updateAvailabilityInDb(doc);
            log.info("Doctor '{}' released and now available.", doc.getName());
        }
    }

    // -------------------------------------------------------------------------
    // Database operations
    // -------------------------------------------------------------------------

    private void loadFromDatabase() {
        if (dataSource == null) { seedDefaultDoctors(); return; }
        String sql = "SELECT id, name, specialization, username, password_hash, available, assigned_patient_id FROM doctors";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Doctor d = new Doctor(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("specialization"),
                        rs.getString("username"),
                        rs.getString("password_hash")
                );
                d.setAvailable(rs.getBoolean("available"));
                d.setAssignedPatientId(rs.getString("assigned_patient_id"));
                doctorTable.put(d.getUsername(), d);
                idIndex.put(d.getId(), d);
            }
            log.info("Loaded {} doctors from database.", doctorTable.size());
            if (doctorTable.isEmpty()) seedDefaultDoctors();
        } catch (SQLException e) {
            log.warn("Could not load doctors from DB: {}. Using defaults.", e.getMessage());
            seedDefaultDoctors();
        }
    }

    private void persistToDb(Doctor doctor) {
        if (dataSource == null) return;
        String sql = "INSERT INTO doctors (id, name, specialization, username, password_hash, available, assigned_patient_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE name=VALUES(name), specialization=VALUES(specialization), " +
                     "password_hash=VALUES(password_hash), available=VALUES(available), assigned_patient_id=VALUES(assigned_patient_id)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctor.getId());
            ps.setString(2, doctor.getName());
            ps.setString(3, doctor.getSpecialization());
            ps.setString(4, doctor.getUsername());
            ps.setString(5, doctor.getPasswordHash());
            ps.setBoolean(6, doctor.isAvailable());
            ps.setString(7, doctor.getAssignedPatientId());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to persist doctor {}: {}", doctor.getName(), e.getMessage());
        }
    }

    private void updateAvailabilityInDb(Doctor doctor) {
        if (dataSource == null) return;
        String sql = "UPDATE doctors SET available=?, assigned_patient_id=? WHERE id=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, doctor.isAvailable());
            ps.setString(2, doctor.getAssignedPatientId());
            ps.setInt(3, doctor.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to update doctor availability: {}", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** SHA-256 hash of plain-text password. */
    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /** Seeds default doctors when DB is empty or unavailable. */
    private void seedDefaultDoctors() {
        if (!doctorTable.isEmpty()) return;
        addDoctor(1, "Dr. Pramod Kumar",   "Emergency Medicine",  "pramod",   "pramod123");
        addDoctor(2, "Dr. Aman Singh",     "Cardiology",          "aman",     "aman123");
        addDoctor(3, "Dr. Mohit Chandra",  "Pulmonology",         "mohit",    "mohit123");
        addDoctor(4, "Dr. Ananya Sharma",  "Neurology",           "ananya",   "ananya123");
        addDoctor(5, "Dr. Diwakar Pandey", "General Surgery",     "diwakar",  "diwakar123");
        log.info("Seeded {} default doctors.", doctorTable.size());
    }
}
