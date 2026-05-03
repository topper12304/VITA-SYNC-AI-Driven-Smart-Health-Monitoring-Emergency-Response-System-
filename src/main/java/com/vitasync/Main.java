package com.vitasync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vitasync.exceptions.CriticalVitalException;
import com.vitasync.exceptions.DatabaseSyncException;
import com.vitasync.management.BedManager;
import com.vitasync.management.DischargeSummaryService;
import com.vitasync.management.DoctorManager;
import com.vitasync.management.EmergencyRequestManager;
import com.vitasync.management.InventoryManager;
import com.vitasync.management.PatientManager;
import com.vitasync.model.PatientRecord;
import com.vitasync.records.RecordManager;
import com.vitasync.simulator.VitalSignValidator;
import com.vitasync.simulator.VitalsSimulator;
import com.vitasync.triage.CriticalVitalHandler;
import com.vitasync.triage.TriageEngine;
import com.vitasync.ui.AlertStore;
import com.vitasync.ui.VitaSyncApp;

import javafx.application.Application;

/**
 * Entry point for the Integrated Smart Hospital System.
 * VITA-SYNC (real-time monitoring) + VITAL-CONNECT (emergency management).
 *
 * Launch flow:
 *   Main → initializes all components → injects into VitaSyncApp
 *        → VitaSyncApp shows Login Screen
 *        → on success → DashboardController (7 tabs)
 */
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Initializing VITA-SYNC + VITAL-CONNECT Integrated System...");

        try {
            // 1. Persistence layer
            RecordManager recordManager = RecordManager.createFromProperties();
            AlertStore    alertStore    = new AlertStore();
            BedManager    bedManager    = new BedManager(20);

            // 2. Doctor & Inventory managers (pass the same DataSource)
            DoctorManager            doctorManager    = new DoctorManager(recordManager.getDataSource());
            InventoryManager         inventoryManager = new InventoryManager(recordManager.getDataSource());
            EmergencyRequestManager  erManager        = new EmergencyRequestManager(recordManager.getDataSource());
            DischargeSummaryService  summaryService   = new DischargeSummaryService(recordManager.getDataSource());

            // 3. Core triage engine
            TriageEngine triageEngine = new TriageEngine();

            // 4. Critical vital handler (bridges monitoring → alerts + triage)
            CriticalVitalHandler criticalHandler = new CriticalVitalHandler(triageEngine, alertStore);

            // 5. Simulator listener — updates PatientRecord + DB + triage on every reading
            VitalsSimulator simulator = new VitalsSimulator((patientId, hr, spo2) -> {
                PatientRecord rec = recordManager.getPatientRecord(patientId);
                if (rec != null) {
                    rec.updateVitals(hr, spo2);
                    try { recordManager.updatePatientRecord(rec); }
                    catch (DatabaseSyncException ex) {
                        log.warn("DB update failed for {}: {}", patientId, ex.getMessage());
                    }
                }
                try { VitalSignValidator.validateAndThrow(patientId, hr, spo2); }
                catch (CriticalVitalException ex) {
                    criticalHandler.handleCriticalVital(ex, hr, spo2);
                    // Auto-assign a doctor ONLY if this patient doesn't already have one
                    boolean alreadyAssigned = doctorManager.getAllDoctors().stream()
                            .anyMatch(d -> patientId.equals(d.getAssignedPatientId()));
                    if (!alreadyAssigned) {
                        doctorManager.assignDoctorToPatient(patientId).ifPresent(doc ->
                            log.info("AUTO-ASSIGNED: Dr. {} to critical patient {}", doc.getName(), patientId)
                        );
                    }
                    // Create an ER request only if no PENDING request exists for this patient
                    boolean hasPending = erManager.getPendingRequests().stream()
                            .anyMatch(r -> patientId.equals(r.getPatientId()));
                    if (!hasPending) {
                        erManager.createRequest(patientId,
                                com.vitasync.model.EmergencyRequest.Priority.CRITICAL,
                                "AUTO: " + ex.getVitalType() + " = " + ex.getValue() + " (" + ex.getThreshold() + ")",
                                doctorManager.getAllDoctors().stream()
                                        .filter(d -> patientId.equals(d.getAssignedPatientId()))
                                        .findFirst()
                                        .map(d -> "Dr. " + d.getName())
                                        .orElse("Awaiting Assignment"));
                    }
                } catch (Exception e) {
                    log.error("Validation error for {}: {}", patientId, e.getMessage());
                }
                triageEngine.updatePatientPriority(patientId, hr, spo2);
            });
            simulator.setCriticalVitalHandler(criticalHandler);

            // 6. Patient manager
            PatientManager patientManager = new PatientManager(recordManager, simulator, triageEngine, bedManager);

            // 7. Inject all into UI
            VitaSyncApp.setDependencies(
                recordManager, triageEngine, alertStore,
                bedManager, patientManager, simulator,
                doctorManager, inventoryManager, erManager, summaryService
            );

            log.info("Launching VITA-SYNC UI (Login Screen)...");
            Application.launch(VitaSyncApp.class, args);

        } catch (Exception e) {
            log.error("CRITICAL FAILURE during system startup: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}
