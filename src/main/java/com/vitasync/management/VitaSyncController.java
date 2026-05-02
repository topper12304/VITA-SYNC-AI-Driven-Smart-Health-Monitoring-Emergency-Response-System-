package com.vitasync.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vitasync.records.RecordManager;

public class VitaSyncController {
    private static final Logger log = LoggerFactory.getLogger(VitaSyncController.class);

    private final RecordManager recordManager;
    private final BedManager bedManager;
    private final DoctorManager doctorManager;

    public VitaSyncController(RecordManager recordManager) {
        this.recordManager = recordManager;
        
        // FIXED: BedManager needs a capacity (e.g., 20)
        this.bedManager = new BedManager(20);
        
        // DoctorManager uses in-memory mode (no DB) in this controller
        this.doctorManager = new DoctorManager();
        
        log.info("VitaSyncController initialized with integrated managers.");
    }

    public RecordManager getRecordManager() {
        return recordManager;
    }

    public BedManager getBedManager() {
        return bedManager;
    }

    public DoctorManager getDoctorManager() {
        return doctorManager;
    }
}