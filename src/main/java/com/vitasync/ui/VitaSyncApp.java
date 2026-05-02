package com.vitasync.ui;

import com.vitasync.exceptions.CriticalVitalException;
import com.vitasync.exceptions.DatabaseSyncException;
import com.vitasync.exceptions.ResourceAllocationException;
import com.vitasync.management.BedManager;
import com.vitasync.management.DischargeSummaryService;
import com.vitasync.management.DoctorManager;
import com.vitasync.management.EmergencyRequestManager;
import com.vitasync.management.InventoryManager;
import com.vitasync.management.PatientManager;
import com.vitasync.model.Doctor;
import com.vitasync.model.PatientRecord;
import com.vitasync.records.RecordManager;
import com.vitasync.simulator.VitalSignListener;
import com.vitasync.simulator.VitalSignValidator;
import com.vitasync.simulator.VitalsSimulator;
import com.vitasync.triage.CriticalVitalHandler;
import com.vitasync.triage.TriageEngine;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JavaFX Application entry point for VITA-SYNC.
 * Flow: Login Screen → (on success) → Main Dashboard
 */
public class VitaSyncApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(VitaSyncApp.class);

    private static final int      TOTAL_BEDS       = 20;
    private static final String[] DEFAULT_PATIENTS = {
        "P001","P002","P003","P004","P005",
        "P006","P007","P008","P009","P010"
    };

    // Static dependencies injected from Main.java
    private static VitalsSimulator  staticSimulator;
    private static RecordManager    staticRecordManager;
    private static TriageEngine     staticTriageEngine;
    private static AlertStore       staticAlertStore;
    private static BedManager       staticBedManager;
    private static PatientManager   staticPatientManager;
    private static DoctorManager    staticDoctorManager;
    private static InventoryManager staticInventoryManager;
    private static EmergencyRequestManager staticErManager;
    private static DischargeSummaryService staticSummaryService;

    private VitalsSimulator simulator;
    private RecordManager   recordManager;

    public static void setDependencies(RecordManager rm, TriageEngine te, AlertStore as,
                                       BedManager bm, PatientManager pm, VitalsSimulator sim,
                                       DoctorManager dm, InventoryManager im,
                                       EmergencyRequestManager erm, DischargeSummaryService dss) {
        staticRecordManager    = rm;
        staticTriageEngine     = te;
        staticAlertStore       = as;
        staticBedManager       = bm;
        staticPatientManager   = pm;
        staticSimulator        = sim;
        staticDoctorManager    = dm;
        staticInventoryManager = im;
        staticErManager        = erm;
        staticSummaryService   = dss;
    }

    @Override
    public void start(Stage primaryStage) {
        log.info("Initializing VITA-SYNC Dashboard...");

        // ---- CASE 1: Coordinated Launch (via Main.java) ----
        if (staticRecordManager != null) {
            log.info("Coordinated launch — showing login screen.");
            this.recordManager = staticRecordManager;
            this.simulator     = staticSimulator;

            // Admit default patients
            AtomicInteger ageBase = new AtomicInteger(22);
            for (String pid : DEFAULT_PATIENTS) {
                try { staticPatientManager.admitPatient(pid, "Patient-" + pid, ageBase.getAndAdd(3)); }
                catch (ResourceAllocationException | DatabaseSyncException e) {
                    log.warn("Could not admit {}: {}", pid, e.getMessage());
                    if (staticRecordManager.getPatientRecord(pid) == null) {
                        PatientRecord rec = new PatientRecord(pid, "Patient-" + pid, ageBase.get());
                        try { staticRecordManager.createPatientRecord(rec); }
                        catch (DatabaseSyncException ex) { /* ignore */ }
                    }
                    staticSimulator.startMonitoring(pid);
                }
            }

            // Show Login → then Dashboard
            showLogin(primaryStage, staticDoctorManager,
                      staticRecordManager, staticTriageEngine, staticAlertStore,
                      staticBedManager, staticPatientManager,
                      staticDoctorManager, staticInventoryManager,
                      staticErManager, staticSummaryService, null);
            return;
        }

        // ---- CASE 2: Standalone Launch ----
        log.info("Standalone launch — initializing local components.");

        TriageEngine         triageEngine    = new TriageEngine();
        AlertStore           alertStore      = new AlertStore();
        BedManager           bedManager      = new BedManager(TOTAL_BEDS);
        CriticalVitalHandler criticalHandler = new CriticalVitalHandler(triageEngine, alertStore);

        try {
            recordManager = RecordManager.createFromProperties();
            log.info("DB connected.");
        } catch (Exception e) {
            log.warn("DB unavailable, using in-memory mode.");
            recordManager = RecordManager.createInMemoryOnly();
        }

        // DoctorManager and InventoryManager
        DoctorManager    doctorManager;
        InventoryManager inventoryManager;
        EmergencyRequestManager erManager;
        DischargeSummaryService summaryService;
        if (recordManager.getDataSource() != null) {
            doctorManager    = new DoctorManager(recordManager.getDataSource());
            inventoryManager = new InventoryManager(recordManager.getDataSource());
            erManager        = new EmergencyRequestManager(recordManager.getDataSource());
            summaryService   = new DischargeSummaryService(recordManager.getDataSource());
        } else {
            doctorManager    = new DoctorManager();
            inventoryManager = new InventoryManager();
            erManager        = new EmergencyRequestManager();
            summaryService   = new DischargeSummaryService();
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
            PatientRecord rec = rm.getPatientRecord(patientId);
            if (rec != null) {
                rec.updateVitals(heartRate, spO2);
                try { rm.updatePatientRecord(rec); }
                catch (DatabaseSyncException ex) { log.warn("DB update failed: {}", ex.getMessage()); }
            }
            try { VitalSignValidator.validateAndThrow(patientId, heartRate, spO2); }
            catch (CriticalVitalException ex) { criticalHandler.handleCriticalVital(ex, heartRate, spO2); }
            triageEngine.updatePatientPriority(patientId, heartRate, spO2);
        };

        simulator = new VitalsSimulator(listener);
        simulator.setCriticalVitalHandler(criticalHandler);

        PatientManager patientManager = new PatientManager(recordManager, simulator, triageEngine, bedManager);
        for (String pid : DEFAULT_PATIENTS) {
            try { patientManager.admitPatient(pid, "Patient-" + pid, ageBase.getAndAdd(3)); }
            catch (ResourceAllocationException | DatabaseSyncException e) {
                log.warn("Could not admit {}: {}", pid, e.getMessage());
                simulator.startMonitoring(pid);
            }
        }

        showLogin(primaryStage, doctorManager,
                  recordManager, triageEngine, alertStore,
                  bedManager, patientManager, doctorManager, inventoryManager,
                  erManager, summaryService, listener);
    }

    private void showLogin(Stage stage, DoctorManager dm,
                           RecordManager rm, TriageEngine te, AlertStore as,
                           BedManager bm, PatientManager pm,
                           DoctorManager doctorManager, InventoryManager inventoryManager,
                           EmergencyRequestManager erManager, DischargeSummaryService summaryService,
                           VitalSignListener listener) {
        new LoginScreen(dm, (loggedInDoctor) -> {
            log.info("Login successful: {}", loggedInDoctor.getName());
            DashboardController dashboard = new DashboardController(
                    rm, te, as, bm, pm, doctorManager, inventoryManager,
                    erManager, summaryService, listener, loggedInDoctor);
            dashboard.show(stage);
        }).show(stage);
    }

    @Override
    public void stop() {
        log.info("Stopping VITA-SYNC...");
        if (simulator       != null) simulator.close();
        if (staticSimulator != null) staticSimulator.close();
        if (recordManager   != null) recordManager.close();
        if (staticRecordManager != null) staticRecordManager.close();
        Platform.exit();
    }
}
