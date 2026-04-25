package com.vitasync.api;

import com.vitasync.exceptions.CriticalVitalException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Professional Alert Store to manage and provide critical health notifications.
 * This is used by both the API Server and the UI.
 */
public class AlertStore {
    private static final int MAX_ALERTS = 100;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Thread-safe list for alerts
    private final List<String> alerts = new CopyOnWriteArrayList<>();
    
    private static AlertStore instance;

    private AlertStore() {}

    public static synchronized AlertStore getInstance() {
        if (instance == null) {
            instance = new AlertStore();
        }
        return instance;
    }

    /**
     * Adds a new alert triggered by a CriticalVitalException.
     */
    public void addAlert(CriticalVitalException e) {
        String time = LocalDateTime.now().format(FMT);
        String message = String.format("[%s] ALERT: Patient %s has critical %s: %d (Threshold: %s)", 
                                       time, e.getPatientId(), e.getVitalType(), e.getValue(), e.getThreshold());
        
        alerts.add(0, message); // Add to top (newest first)
        
        // Keep only the last 100 alerts
        if (alerts.size() > MAX_ALERTS) {
            alerts.remove(alerts.size() - 1);
        }
    }

    public List<String> getAllAlerts() {
        return Collections.unmodifiableList(alerts);
    }
}