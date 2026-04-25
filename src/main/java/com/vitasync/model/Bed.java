package com.vitasync.model;

/**
 * Represents a hospital bed.
 * Status: FREE or OCCUPIED (with assigned patientId).
 */
public class Bed {

    public enum Status { FREE, OCCUPIED }

    private final String bedId;
    private volatile Status status;
    private volatile String assignedPatientId;

    public Bed(String bedId) {
        this.bedId  = bedId;
        this.status = Status.FREE;
    }

    public synchronized void assign(String patientId) {
        this.status            = Status.OCCUPIED;
        this.assignedPatientId = patientId;
    }

    public synchronized void release() {
        this.status            = Status.FREE;
        this.assignedPatientId = null;
    }

    public String  getBedId()             { return bedId; }
    public Status  getStatus()            { return status; }
    public String  getAssignedPatientId() { return assignedPatientId; }
    public boolean isFree()               { return status == Status.FREE; }

    @Override
    public String toString() {
        return String.format("Bed{id='%s', status=%s, patient=%s}",
                bedId, status, assignedPatientId);
    }
}
