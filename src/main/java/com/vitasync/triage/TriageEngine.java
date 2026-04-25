package com.vitasync.triage;

import com.vitasync.model.PatientPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

/**
 * PHASE 3 UPDATED: Prioritizes patients based on health stability and 
 * predicts risk levels using NEWS2 (National Early Warning Score) logic.
 */
public class TriageEngine {

    private static final Logger log = LoggerFactory.getLogger(TriageEngine.class);

    private static final double OPTIMAL_HR = 75.0;

    // PriorityBlockingQueue is thread-safe and orders by PatientPriority.compareTo (lowest score first)
    private final PriorityBlockingQueue<PatientPriority> priorityQueue = new PriorityBlockingQueue<>();

    // Tracks the latest score per patient so we can remove stale entries on update
    private final ConcurrentHashMap<String, PatientPriority> latestPriority = new ConcurrentHashMap<>();

    /**
     * Calculates the health stability score for the given vitals.
     * Score range: [0.0, 1.0] — lower means more critical.
     */
    public double calculateHealthStabilityScore(int heartRate, int spO2) {
        double normalizedHR = 1.0 - Math.abs(heartRate - OPTIMAL_HR) / OPTIMAL_HR;
        normalizedHR = Math.max(0.0, normalizedHR); // clamp to [0, 1]
        double normalizedSpO2 = spO2 / 100.0;
        return (normalizedHR + normalizedSpO2) / 2.0;
    }

    /**
     * PHASE 3 NEW: Predicts Risk Level based on clinical NEWS2 scoring.
     * Use this to show "LOW", "MEDIUM", or "HIGH" risk on the Dashboard.
     */
    public String predictRiskStatus(int heartRate, int spO2) {
        int riskScore = 0;

        // SpO2 Scoring logic (Clinical NEWS2 thresholds)
        if (spO2 <= 91) riskScore += 3;
        else if (spO2 <= 93) riskScore += 2;
        else if (spO2 <= 95) riskScore += 1;

        // Heart Rate Scoring logic
        if (heartRate <= 40 || heartRate >= 131) riskScore += 3;
        else if (heartRate >= 111 && heartRate <= 130) riskScore += 2;
        else if (heartRate >= 91 && heartRate <= 110) riskScore += 1;
        else if (heartRate >= 41 && heartRate <= 50) riskScore += 1;

        // Interpretation
        if (riskScore >= 5) return "HIGH RISK (Emergency)";
        if (riskScore >= 3) return "MODERATE RISK (Watch)";
        return "STABLE (Low Risk)";
    }

    /**
     * Updates (or inserts) a patient's priority based on new vital readings.
     * Removes the old entry from the queue before inserting the updated one.
     */
    public void updatePatientPriority(String patientId, int heartRate, int spO2) {
        double score = calculateHealthStabilityScore(heartRate, spO2);
        PatientPriority newPriority = new PatientPriority(patientId, score);

        // Remove stale entry if present
        PatientPriority old = latestPriority.put(patientId, newPriority);
        if (old != null) {
            priorityQueue.remove(old);
        }
        priorityQueue.offer(newPriority);
        log.debug("Updated priority for {}: score={:.4f}, Risk={}", 
                  patientId, score, predictRiskStatus(heartRate, spO2));
    }

    /**
     * Retrieves and removes the most critical patient (lowest score) from the queue.
     * Returns null if the queue is empty.
     */
    public PatientPriority getNextCriticalPatient() {
        PatientPriority next = priorityQueue.poll();
        if (next != null) {
            latestPriority.remove(next.getPatientId());
        }
        return next;
    }

    /**
     * Returns a snapshot of all patients sorted by priority (most critical first).
     */
    public List<PatientPriority> getAllPrioritizedPatients() {
        return priorityQueue.stream()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Removes a patient from the triage queue entirely.
     */
    public void removePatient(String patientId) {
        PatientPriority entry = latestPriority.remove(patientId);
        if (entry != null) {
            priorityQueue.remove(entry);
        }
    }

    public int getQueueSize() {
        return priorityQueue.size();
    }
}