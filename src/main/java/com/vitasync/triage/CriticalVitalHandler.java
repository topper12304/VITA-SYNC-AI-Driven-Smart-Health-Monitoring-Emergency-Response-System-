package com.vitasync.triage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vitasync.exceptions.CriticalVitalException;
import com.vitasync.ui.AlertStore;

/**
 * Integrated Handler: VITA-SYNC monitoring triggers VITAL-CONNECT emergency response.
 */
public class CriticalVitalHandler {

    private static final Logger log = LoggerFactory.getLogger(CriticalVitalHandler.class);

    private final TriageEngine triageEngine;
    private final AlertStore alertStore;
    
    // PHASE 4: Bridge to Vital Connect
    // private final EmergencyRequestManager requestManager; 

    public CriticalVitalHandler(TriageEngine triageEngine) {
        this(triageEngine, null);
    }

    public CriticalVitalHandler(TriageEngine triageEngine, AlertStore alertStore) {
        this.triageEngine = triageEngine;
        this.alertStore = alertStore;
        // this.requestManager = null; // Default constructor fix
    }

    /* // Integrated Constructor (Use this after merging projects)
    public CriticalVitalHandler(TriageEngine triageEngine, AlertStore alertStore, EmergencyRequestManager rm) {
        this.triageEngine = triageEngine;
        this.alertStore = alertStore;
        this.requestManager = rm;
    } 
    */

    public void handleCriticalVital(CriticalVitalException exception, int heartRate, int spO2) {
        // 1. Log the event locally
        logCriticalEvent(exception);
        
        // 2. Local Alert UI Storage
        if (alertStore != null) alertStore.addAlert(exception);
        
        // 3. Update VITA-SYNC Triage Queue (Internal Sorting)
        updateTriagePriority(exception.getPatientId(), heartRate, spO2);
        
        // 4. BRIDGE: Trigger VITAL-CONNECT Emergency Dispatch
        triggerVitalConnectEmergency(exception);
    }

    private void triggerVitalConnectEmergency(CriticalVitalException exception) {
        String patientId = exception.getPatientId();
        String detail = "AUTO-TRIGGER: " + exception.getVitalType() + " reached " + exception.getValue();
        
        log.info("📢 BRIDGING TO VITAL-CONNECT: Raising automatic emergency request for {}", patientId);
        
        /* // ACTUAL INTEGRATION CODE:
        if (requestManager != null) {
            requestManager.createRequest(patientId, Priority.CRITICAL, detail);
            log.info("✅ VITAL-CONNECT Ticket Created Successfully for {}", patientId);
        } 
        */
        
        // Console placeholder for now
        System.out.println(">>> [BRIDGE] Dispatching Emergency Team via VITAL-CONNECT for Patient: " + patientId);
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