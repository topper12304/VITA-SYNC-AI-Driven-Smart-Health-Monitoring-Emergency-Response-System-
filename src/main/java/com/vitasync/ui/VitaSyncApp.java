package com.vitasync.ui;

import com.vitasync.exceptions.DatabaseSyncException;
import com.vitasync.exceptions.ResourceAllocationException;
import com.vitasync.management.BedManager;
import com.vitasync.management.PatientManager;
import com.vitasync.model.PatientRecord;
import com.vitasync.records.RecordManager;
import com.vitasync.simulator.VitalSignListener;
import com.vitasync.simulator.VitalSignValidator;
import com.vitasync.simulator.VitalsSimulator;
import com.vitasync.triage.CriticalVitalHandler;
import com.vitasync.triage.TriageEngine;
import com.vitasync.exceptions.CriticalVitalException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JavaFX Application entry point for VITA-SYNC.
 */
public class VitaSyncApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(VitaSyncApp.class);

    private static final int    TOTAL_BEDS       = 20;
    private static final String[] DEFAULT_PATIENTS = {
        "P001","P002","P003","P004","P005",
        "P006","P007","P008","P009","P010"
    };

    private VitalsSimulator simulator;
    private RecordManager   recordManager;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Core components
        TriageEngine         triageEngine    = new TriageEngine();
        AlertStore           alertStore      = new AlertStore();
        BedManager           bedManager      = new BedManager(TOTAL_BEDS);
        CriticalVitalHandler criticalHandler = new CriticalVitalHandler(triageEngine, alertStore);

        // DB (fallback to in-memory)
        try {
            recordManager = RecordManager.createFromProperties();
            log.info("DB connected.");
        } catch (Exception e) {
            log.warn("DB unavailable, using in-memory mode.");
            recordManager = RecordManager.createInMemoryOnly();
        }

        AtomicInteger ageBase  = new AtomicInteger(22);
        ConcurrentHashMap<String, Boolean> registered = new ConcurrentHashMap<>();
        final RecordManager rm = recordManager;

        VitalSignListener listener = (patientId, heartRate, spO2) -> {
            if (registered.putIfAbsent(patientId, Boolean.TRUE) == null) {
                PatientRecord rec = new PatientRecord(patientId, "Patient-" + patientId, ageBase.getAndAdd(3));
                try { rm.createPatientRecord(rec); }
                catch (DatabaseSyncException ex) { log.warn("DB create failed: {}", ex.getMessage()); }
            }
            try { VitalSignValidator.validateAndThrow(patientId, heartRate, spO2); }
            catch (CriticalVitalException ex) { criticalHandler.handleCriticalVital(ex, heartRate, spO2); }

            PatientRecord rec = rm.getPatientRecord(patientId);
            if (rec != null) {
                rec.updateVitals(heartRate, spO2);
                try { rm.updatePatientRecord(rec); }
                catch (DatabaseSyncException ex) { log.warn("DB update failed: {}", ex.getMessage()); }
            }
            triageEngine.updatePatientPriority(patientId, heartRate, spO2);
        };

        simulator = new VitalsSimulator(listener);
        simulator.setCriticalVitalHandler(criticalHandler);

        // Admit default patients
        PatientManager patientManager = new PatientManager(recordManager, simulator, triageEngine, bedManager);
        for (String pid : DEFAULT_PATIENTS) {
            try { patientManager.admitPatient(pid, "Patient-" + pid, ageBase.getAndAdd(3)); }
            catch (ResourceAllocationException | DatabaseSyncException e) {
                log.warn("Could not admit {}: {}", pid, e.getMessage());
                // fallback: just start monitoring without bed
                simulator.startMonitoring(pid);
            }
        }

        // Build dashboard
        DashboardController dashboard = new DashboardController(
                recordManager, triageEngine, alertStore, bedManager, patientManager, listener);
        dashboard.show(primaryStage);
    }

    @Override
    public void stop() {
        if (simulator     != null) simulator.close();
        if (recordManager != null) recordManager.close();
        Platform.exit();
    }
}
