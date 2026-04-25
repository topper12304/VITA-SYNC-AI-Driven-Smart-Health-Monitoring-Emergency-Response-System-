package com.vitasync.triage;

import com.vitasync.ui.AlertStore;
import com.vitasync.exceptions.CriticalVitalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles CriticalVitalException by:
 * 1. Logging the event with full context.
 * 2. Storing the alert in AlertStore (for REST API / dashboard).
 * 3. Updating the patient's priority in TriageEngine.
 */
public class CriticalVitalHandler {

    private static final Logger log = LoggerFactory.getLogger(CriticalVitalHandler.class);

    private final TriageEngine triageEngine;
    private final AlertStore alertStore;

    public CriticalVitalHandler(TriageEngine triageEngine) {
        this(triageEngine, null);
    }

    public CriticalVitalHandler(TriageEngine triageEngine, AlertStore alertStore) {
        this.triageEngine = triageEngine;
        this.alertStore = alertStore;
    }

    public void handleCriticalVital(CriticalVitalException exception, int heartRate, int spO2) {
        logCriticalEvent(exception);
        if (alertStore != null) alertStore.addAlert(exception);
        updateTriagePriority(exception.getPatientId(), heartRate, spO2);
    }

    public void logCriticalEvent(CriticalVitalException exception) {
        log.warn("CRITICAL VITAL ALERT — PatientID={}, VitalType={}, Value={}, Threshold={}",
                exception.getPatientId(), exception.getVitalType(),
                exception.getValue(), exception.getThreshold());
    }

    public void updateTriagePriority(String patientId, int heartRate, int spO2) {
        triageEngine.updatePatientPriority(patientId, heartRate, spO2);
        log.info("Triage priority updated for critical patient: {}", patientId);
    }
}
