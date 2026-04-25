package com.vitasync.ui;

import com.vitasync.exceptions.CriticalVitalException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Thread-safe store for critical vital alerts.
 * Backed by a JavaFX ObservableList so the UI table auto-updates.
 */
public class AlertStore {

    private static final int MAX_ALERTS = 100;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ObservableList<AlertEntry> alerts =
            FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

    public void addAlert(CriticalVitalException e) {
        String time = LocalDateTime.now().format(FMT);
        AlertEntry entry = new AlertEntry(
                e.getPatientId(), e.getVitalType(),
                String.valueOf(e.getValue()), e.getThreshold(), time);
        javafx.application.Platform.runLater(() -> {
            if (alerts.size() >= MAX_ALERTS) alerts.remove(0);
            alerts.add(0, entry);   // newest first
        });
    }

    public ObservableList<AlertEntry> getAlerts() {
        return alerts;
    }

    /** Immutable row model for the alerts TableView. */
    public static class AlertEntry {
        public final String patientId;
        public final String vitalType;
        public final String value;
        public final String threshold;
        public final String time;

        public AlertEntry(String patientId, String vitalType,
                          String value, String threshold, String time) {
            this.patientId = patientId;
            this.vitalType = vitalType;
            this.value     = value;
            this.threshold = threshold;
            this.time      = time;
        }
    }
}
