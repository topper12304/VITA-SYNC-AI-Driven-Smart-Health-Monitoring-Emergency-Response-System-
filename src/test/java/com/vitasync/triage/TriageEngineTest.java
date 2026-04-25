package com.vitasync.triage;

import com.vitasync.model.PatientPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TriageEngineTest {

    private TriageEngine engine;

    @BeforeEach
    void setUp() { engine = new TriageEngine(); }

    @Test
    void optimalVitalsGiveHighScore() {
        // HR=75, SpO2=100 → normalizedHR=1.0, normalizedSpO2=1.0 → score=1.0
        double score = engine.calculateHealthStabilityScore(75, 100);
        assertEquals(1.0, score, 0.0001);
    }

    @Test
    void criticalLowHRGivesLowScore() {
        // HR=35 (below 40 threshold) → normalizedHR = 1 - 40/75 ≈ 0.4667
        double score = engine.calculateHealthStabilityScore(35, 100);
        assertTrue(score < 0.8, "Critical HR should produce low stability score");
    }

    @Test
    void criticalLowSpO2GivesLowScore() {
        double score = engine.calculateHealthStabilityScore(75, 85);
        assertTrue(score < 0.93, "Low SpO2 should reduce stability score");
    }

    @Test
    void scoreClampedAtZeroForExtremeHR() {
        // HR=0 → normalizedHR = 1 - 75/75 = 0.0
        double score = engine.calculateHealthStabilityScore(0, 100);
        assertEquals(0.5, score, 0.0001); // (0 + 1.0) / 2
    }

    @Test
    void scoreNeverNegative() {
        double score = engine.calculateHealthStabilityScore(300, 0);
        assertTrue(score >= 0.0);
    }

    @Test
    void mostCriticalPatientIsFirstInQueue() {
        engine.updatePatientPriority("P001", 75, 100);  // healthy
        engine.updatePatientPriority("P002", 35, 85);   // critical
        engine.updatePatientPriority("P003", 60, 95);   // moderate

        List<PatientPriority> queue = engine.getAllPrioritizedPatients();
        assertEquals("P002", queue.get(0).getPatientId(), "Most critical should be first");
    }

    @Test
    void updateReplacesOldPriorityEntry() {
        engine.updatePatientPriority("P001", 35, 85);   // critical
        engine.updatePatientPriority("P001", 75, 100);  // now healthy

        List<PatientPriority> queue = engine.getAllPrioritizedPatients();
        assertEquals(1, queue.size(), "Should only have one entry per patient");
        assertEquals(1.0, queue.get(0).getHealthStabilityScore(), 0.0001);
    }

    @Test
    void removePatientClearsFromQueue() {
        engine.updatePatientPriority("P001", 75, 98);
        engine.removePatient("P001");
        assertEquals(0, engine.getQueueSize());
    }

    @Test
    void getNextCriticalPatientPollsQueue() {
        engine.updatePatientPriority("P001", 75, 98);
        PatientPriority next = engine.getNextCriticalPatient();
        assertNotNull(next);
        assertEquals("P001", next.getPatientId());
        assertEquals(0, engine.getQueueSize());
    }

    @Test
    void emptyQueueReturnsNullOnPoll() {
        assertNull(engine.getNextCriticalPatient());
    }

    @Test
    void multiplePatientsSortedCorrectly() {
        engine.updatePatientPriority("PA", 75, 100); // score 1.0
        engine.updatePatientPriority("PB", 40, 90);  // borderline
        engine.updatePatientPriority("PC", 35, 85);  // most critical

        List<PatientPriority> queue = engine.getAllPrioritizedPatients();
        assertTrue(queue.get(0).getHealthStabilityScore()
                <= queue.get(1).getHealthStabilityScore());
        assertTrue(queue.get(1).getHealthStabilityScore()
                <= queue.get(2).getHealthStabilityScore());
    }
}
