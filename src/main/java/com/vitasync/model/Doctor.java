package com.vitasync.model;

/**
 * Represents a hospital doctor with login credentials and specialization.
 * Used for both authentication and patient assignment.
 */
public class Doctor {

    private final int    id;
    private       String name;
    private       String specialization;
    private       String username;
    private       String passwordHash; // BCrypt or SHA-256 hash stored in DB
    private       boolean available;
    private       String assignedPatientId; // null if not assigned

    public Doctor(int id, String name, String specialization, String username, String passwordHash) {
        this.id             = id;
        this.name           = name;
        this.specialization = specialization;
        this.username       = username;
        this.passwordHash   = passwordHash;
        this.available      = true;
        this.assignedPatientId = null;
    }

    // ---- Getters ----
    public int     getId()               { return id; }
    public String  getName()             { return name; }
    public String  getSpecialization()   { return specialization; }
    public String  getUsername()         { return username; }
    public String  getPasswordHash()     { return passwordHash; }
    public boolean isAvailable()         { return available; }
    public String  getAssignedPatientId(){ return assignedPatientId; }

    // ---- Setters ----
    public void setName(String name)                       { this.name = name; }
    public void setSpecialization(String specialization)   { this.specialization = specialization; }
    public void setAvailable(boolean available)            { this.available = available; }
    public void setAssignedPatientId(String patientId)     { this.assignedPatientId = patientId; }

    /** Assign this doctor to a patient — marks as unavailable. */
    public void assignToPatient(String patientId) {
        this.assignedPatientId = patientId;
        this.available = false;
    }

    /** Release this doctor from their current patient — marks as available. */
    public void release() {
        this.assignedPatientId = null;
        this.available = true;
    }

    @Override
    public String toString() {
        return String.format("Doctor{id=%d, name='%s', spec='%s', available=%b}",
                id, name, specialization, available);
    }
}
