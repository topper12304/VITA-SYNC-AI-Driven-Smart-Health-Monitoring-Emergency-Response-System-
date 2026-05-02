package com.vitasync.management;

import com.vitasync.model.EmergencyRequest;
import com.vitasync.model.EmergencyRequest.Priority;
import com.vitasync.model.EmergencyRequest.Status;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages emergency requests — the real bridge between VITA-SYNC monitoring
 * and VITAL-CONNECT emergency dispatch.
 *
 * Backed by the emergency_requests table in MySQL.
 * In-memory list (CopyOnWriteArrayList) for fast UI reads.
 */
public class EmergencyRequestManager {

    private static final Logger log = LoggerFactory.getLogger(EmergencyRequestManager.class);

    // Thread-safe in-memory list for UI binding
    private final CopyOnWriteArrayList<EmergencyRequest> requests = new CopyOnWriteArrayList<>();
    private final HikariDataSource dataSource;

    public EmergencyRequestManager(HikariDataSource dataSource) {
        this.dataSource = dataSource;
        loadFromDatabase();
    }

    /** In-memory-only mode. */
    public EmergencyRequestManager() {
        this.dataSource = null;
    }

    // -------------------------------------------------------------------------
    // Core Operations
    // -------------------------------------------------------------------------

    /**
     * Creates a new emergency request for a patient and persists it to DB.
     * Returns the created request.
     */
    public EmergencyRequest createRequest(String patientId, Priority priority,
                                          String description, String assignedDoctor) {
        int id = persistToDb(patientId, priority, description, assignedDoctor);
        EmergencyRequest req = new EmergencyRequest(
                id, patientId, priority, description,
                Status.PENDING, assignedDoctor, LocalDateTime.now());
        requests.add(0, req); // newest first
        log.info("Emergency request created: {}", req);
        return req;
    }

    /**
     * Updates the status of an existing request (PENDING → DISPATCHED → RESOLVED).
     */
    public void updateStatus(int requestId, Status newStatus, String assignedDoctor) {
        requests.stream()
                .filter(r -> r.getRequestId() == requestId)
                .findFirst()
                .ifPresent(r -> {
                    r.setStatus(newStatus);
                    if (assignedDoctor != null) r.setAssignedDoctor(assignedDoctor);
                    updateStatusInDb(requestId, newStatus, assignedDoctor);
                    log.info("ER#{} status updated to {}", requestId, newStatus);
                });
    }

    /** Returns all requests (newest first). */
    public List<EmergencyRequest> getAllRequests() {
        return new ArrayList<>(requests);
    }

    /** Returns only PENDING requests. */
    public List<EmergencyRequest> getPendingRequests() {
        return requests.stream().filter(r -> r.getStatus() == Status.PENDING).toList();
    }

    public int getPendingCount() {
        return (int) requests.stream().filter(r -> r.getStatus() == Status.PENDING).count();
    }

    // -------------------------------------------------------------------------
    // Database
    // -------------------------------------------------------------------------

    private int persistToDb(String patientId, Priority priority,
                             String description, String assignedDoctor) {
        if (dataSource == null) return requests.size() + 1;
        String sql = "INSERT INTO emergency_requests (patient_id, priority, description, status, assigned_doctor) " +
                     "VALUES (?, ?, ?, 'PENDING', ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, patientId);
            ps.setString(2, priority.name());
            ps.setString(3, description);
            ps.setString(4, assignedDoctor != null ? assignedDoctor : "Awaiting Assignment");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Failed to persist emergency request: {}", e.getMessage());
        }
        return -1;
    }

    private void updateStatusInDb(int requestId, Status status, String assignedDoctor) {
        if (dataSource == null) return;
        String sql = "UPDATE emergency_requests SET status=?, assigned_doctor=? WHERE request_id=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, assignedDoctor != null ? assignedDoctor : "Awaiting Assignment");
            ps.setInt(3, requestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to update ER status: {}", e.getMessage());
        }
    }

    private void loadFromDatabase() {
        if (dataSource == null) return;
        String sql = "SELECT request_id, patient_id, priority, description, status, assigned_doctor, created_at " +
                     "FROM emergency_requests ORDER BY created_at DESC LIMIT 200";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("created_at");
                EmergencyRequest req = new EmergencyRequest(
                        rs.getInt("request_id"),
                        rs.getString("patient_id"),
                        Priority.valueOf(rs.getString("priority")),
                        rs.getString("description"),
                        Status.valueOf(rs.getString("status")),
                        rs.getString("assigned_doctor"),
                        ts != null ? ts.toLocalDateTime() : LocalDateTime.now()
                );
                requests.add(req);
            }
            log.info("Loaded {} emergency requests from DB.", requests.size());
        } catch (SQLException e) {
            log.warn("Could not load emergency requests: {}", e.getMessage());
        }
    }
}
