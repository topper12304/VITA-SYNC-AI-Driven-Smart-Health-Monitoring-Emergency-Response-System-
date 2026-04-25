package com.vitasync.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a patient's triage priority entry.
 * Lower healthStabilityScore = more critical = higher priority in the queue.
 */
public class PatientPriority implements Comparable<PatientPriority> {

    private final String patientId;
    private final double healthStabilityScore;
    private final LocalDateTime timestamp;

    public PatientPriority(String patientId, double healthStabilityScore) {
        this.patientId = patientId;
        this.healthStabilityScore = healthStabilityScore;
        this.timestamp = LocalDateTime.now();
    }

    public PatientPriority(String patientId, double healthStabilityScore, LocalDateTime timestamp) {
        this.patientId = patientId;
        this.healthStabilityScore = healthStabilityScore;
        this.timestamp = timestamp;
    }

    /**
     * Lower score = more critical = comes first in the priority queue.
     */
    @Override
    public int compareTo(PatientPriority other) {
        int scoreCompare = Double.compare(this.healthStabilityScore, other.healthStabilityScore);
        if (scoreCompare != 0) return scoreCompare;
        // Tie-break by timestamp (earlier = higher priority)
        return this.timestamp.compareTo(other.timestamp);
    }

    public String getPatientId() { return patientId; }
    public double getHealthStabilityScore() { return healthStabilityScore; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PatientPriority)) return false;
        PatientPriority that = (PatientPriority) o;
        return Objects.equals(patientId, that.patientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(patientId);
    }

    @Override
    public String toString() {
        return String.format("PatientPriority{id='%s', score=%.4f}", patientId, healthStabilityScore);
    }
}
