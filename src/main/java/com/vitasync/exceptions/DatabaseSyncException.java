package com.vitasync.exceptions;

/**
 * Thrown when a database synchronization operation fails for a patient record.
 */
public class DatabaseSyncException extends Exception {

    private final String patientId;
    private final String operation;

    public DatabaseSyncException(String patientId, String operation, Throwable cause) {
        super(String.format("Database sync failed for patient '%s' during '%s'", patientId, operation), cause);
        this.patientId = patientId;
        this.operation = operation;
    }

    public String getPatientId() { return patientId; }
    public String getOperation() { return operation; }
}
