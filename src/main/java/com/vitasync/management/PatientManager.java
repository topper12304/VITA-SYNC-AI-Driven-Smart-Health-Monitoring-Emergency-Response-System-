package com.vitasync.management;

import com.vitasync.exceptions.DatabaseSyncException;
import com.vitasync.exceptions.ResourceAllocationException;
import com.vitasync.model.Bed;
import com.vitasync.model.PatientRecord;
import com.vitasync.records.RecordManager;
import com.vitasync.simulator.VitalsSimulator;
import com.vitasync.triage.TriageEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinates adding and removing patients across all subsystems:
 * RecordManager, VitalsSimulator, TriageEngine, BedManager.
 */
public class PatientManager {

    private static final Logger log = LoggerFactory.getLogger(PatientManager.class);

    private final RecordManager  recordManager;
    private final VitalsSimulator simulator;
    private final TriageEngine   triageEngine;
    private final BedManager     bedManager;

    public PatientManager(RecordManager recordManager,
                          VitalsSimulator simulator,
                          TriageEngine triageEngine,
                          BedManager bedManager) {
        this.recordManager = recordManager;
        this.simulator     = simulator;
        this.triageEngine  = triageEngine;
        this.bedManager    = bedManager;
    }

    /**
     * Admits a new patient:
     * 1. Creates PatientRecord
     * 2. Assigns a bed
     * 3. Starts vital sign monitoring
     */
    public Bed admitPatient(String patientId, String name, int age)
            throws DatabaseSyncException, ResourceAllocationException {

        if (recordManager.getPatientRecord(patientId) != null) {
            throw new IllegalArgumentException("Patient " + patientId + " already exists.");
        }

        PatientRecord record = new PatientRecord(patientId, name, age);
        recordManager.createPatientRecord(record);

        Bed bed = bedManager.assignBed(patientId);

        simulator.startMonitoring(patientId);

        log.info("Admitted patient {} to {}", patientId, bed.getBedId());
        return bed;
    }

    /**
     * Discharges a patient:
     * 1. Stops monitoring
     * 2. Removes from triage
     * 3. Releases bed
     */
    public void dischargePatient(String patientId) {
        simulator.stopMonitoring(patientId);
        triageEngine.removePatient(patientId);
        bedManager.releaseBed(patientId);
        log.info("Discharged patient {}", patientId);
    }
}
