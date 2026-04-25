package com.vitasync.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a patient's medical record including current vitals and historical readings.
 */
public class PatientRecord {

    private final String patientId;
    private String name;
    private int age;
    private volatile int currentHeartRate;
    private volatile int currentSpO2;
    private final List<VitalSignReading> history;
    private volatile LocalDateTime lastUpdated;

    public PatientRecord(String patientId, String name, int age) {
        this.patientId = patientId;
        this.name = name;
        this.age = age;
        this.history = new ArrayList<>();
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Updates the current vitals and appends a reading to history.
     */
    public synchronized void updateVitals(int heartRate, int spO2) {
        this.currentHeartRate = heartRate;
        this.currentSpO2 = spO2;
        this.lastUpdated = LocalDateTime.now();
        this.history.add(new VitalSignReading(heartRate, spO2, this.lastUpdated));
    }

    public String getPatientId() { return patientId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public int getCurrentHeartRate() { return currentHeartRate; }
    public int getCurrentSpO2() { return currentSpO2; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }

    public List<VitalSignReading> getHistory() {
        return Collections.unmodifiableList(history);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PatientRecord)) return false;
        PatientRecord that = (PatientRecord) o;
        return Objects.equals(patientId, that.patientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(patientId);
    }

    @Override
    public String toString() {
        return String.format("PatientRecord{id='%s', name='%s', age=%d, HR=%d, SpO2=%d}",
                patientId, name, age, currentHeartRate, currentSpO2);
    }
}
