package com.vitasync.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single emergency request raised for a patient.
 * Stored in the emergency_requests table.
 * Status lifecycle: PENDING → DISPATCHED → RESOLVED
 */
public class EmergencyRequest {

    public enum Priority { LOW, MODERATE, HIGH, CRITICAL }
    public enum Status   { PENDING, DISPATCHED, RESOLVED }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final int           requestId;
    private final String        patientId;
    private       Priority      priority;
    private       String        description;
    private       Status        status;
    private       String        assignedDoctor;
    private final LocalDateTime createdAt;

    public EmergencyRequest(int requestId, String patientId, Priority priority,
                            String description, Status status,
                            String assignedDoctor, LocalDateTime createdAt) {
        this.requestId      = requestId;
        this.patientId      = patientId;
        this.priority       = priority;
        this.description    = description;
        this.status         = status;
        this.assignedDoctor = assignedDoctor;
        this.createdAt      = createdAt;
    }

    // ---- Getters ----
    public int           getRequestId()     { return requestId; }
    public String        getPatientId()     { return patientId; }
    public Priority      getPriority()      { return priority; }
    public String        getDescription()   { return description; }
    public Status        getStatus()        { return status; }
    public String        getAssignedDoctor(){ return assignedDoctor; }
    public LocalDateTime getCreatedAt()     { return createdAt; }
    public String        getCreatedAtStr()  { return createdAt != null ? createdAt.format(FMT) : "—"; }

    // ---- Setters ----
    public void setStatus(Status status)               { this.status = status; }
    public void setAssignedDoctor(String doctor)       { this.assignedDoctor = doctor; }
    public void setPriority(Priority priority)         { this.priority = priority; }

    @Override
    public String toString() {
        return String.format("ER#%d [%s] Patient=%s Priority=%s Status=%s",
                requestId, getCreatedAtStr(), patientId, priority, status);
    }
}
