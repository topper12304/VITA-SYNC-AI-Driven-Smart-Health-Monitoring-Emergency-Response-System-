package com.vitasync.management;

import com.vitasync.api.AlertStore;
import com.vitasync.exceptions.DatabaseSyncException;
import com.vitasync.model.PatientRecord;
import com.vitasync.model.VitalSignReading;
import com.vitasync.records.RecordManager;
import com.vitasync.simulator.VitalSignListener;
import com.vitasync.triage.TriageEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The central coordinator of the VITA-SYNC system.
 * It listens to the Simulator and updates the Database and Triage Engine.
 */
public class VitaSyncController implements VitalSignListener {
    private static final Logger log = LoggerFactory.getLogger(VitaSyncController.class);

    private final RecordManager recordManager;
    private final TriageEngine triageEngine;

    public VitaSyncController(RecordManager recordManager, TriageEngine triageEngine) {
        this.recordManager = recordManager;
        this.triageEngine = triageEngine;
    }

    /**
     * This method is called automatically every 1-2 seconds by the Simulator 
     * for every monitored patient.
     */
    @Override
    public void onVitalSignUpdate(String patientId, int heartRate, int spO2) {
        // 1. Get the patient's record from memory
        PatientRecord record = recordManager.getPatientRecord(patientId);
        
        if (record != null) {
            // 2. Update the record with new values
            record.updateVitals(heartRate, spO2);
            
            try {
                // 3. Sync to Database (MySQL)
                recordManager.updatePatientRecord(record);
                
                // 4. Save this specific reading to history table
                recordManager.recordVitalReading(patientId, new VitalSignReading(heartRate, spO2));
                
                // 5. Update Triage Engine (AI Scoring)
                // This re-calculates where the patient stands in the emergency queue
                triageEngine.updatePatientPriority(patientId, heartRate, spO2);
                
                log.debug("Controller: Updated patient {} - HR: {}, SpO2: {}", patientId, heartRate, spO2);
                
            } catch (DatabaseSyncException e) {
                log.error("Critical: Could not sync data for patient {} to Database!", patientId);
            }
        } else {
            log.warn("Controller: Received vitals for unknown patient ID: {}", patientId);
        }
    }
}