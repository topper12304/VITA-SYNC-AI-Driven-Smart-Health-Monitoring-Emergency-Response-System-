package com.vitasync.triage;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for TriageEngine health stability score.
 * Uses jqwik to generate hundreds of random inputs and verify invariants.
 */
class TriageEngineProperties {

    private final TriageEngine engine = new TriageEngine();

    @Property
    void scoreAlwaysBetweenZeroAndOne(
            @ForAll @IntRange(min = 0, max = 300) int hr,
            @ForAll @IntRange(min = 0, max = 100) int spo2) {
        double score = engine.calculateHealthStabilityScore(hr, spo2);
        assertTrue(score >= 0.0 && score <= 1.0,
                "Score out of [0,1] for HR=" + hr + " SpO2=" + spo2 + ": " + score);
    }

    @Property
    void optimalVitalsAlwaysGiveHighestScore(
            @ForAll @IntRange(min = 0, max = 300) int hr,
            @ForAll @IntRange(min = 0, max = 100) int spo2) {
        double optimal = engine.calculateHealthStabilityScore(75, 100);
        double other   = engine.calculateHealthStabilityScore(hr, spo2);
        assertTrue(optimal >= other - 0.0001,
                "Optimal score should be >= any other score");
    }

    @Property
    void higherSpO2NeverWorsensScore(
            @ForAll @IntRange(min = 0, max = 300) int hr,
            @ForAll @IntRange(min = 0, max = 99)  int spo2) {
        double lower  = engine.calculateHealthStabilityScore(hr, spo2);
        double higher = engine.calculateHealthStabilityScore(hr, spo2 + 1);
        assertTrue(higher >= lower - 0.0001,
                "Higher SpO2 should not decrease score");
    }

    @Property
    void updateAndQueueSizeConsistent(
            @ForAll @IntRange(min = 1, max = 20) int patientCount) {
        TriageEngine localEngine = new TriageEngine();
        for (int i = 1; i <= patientCount; i++) {
            localEngine.updatePatientPriority("P" + i, 75, 98);
        }
        assertEquals(patientCount, localEngine.getQueueSize());
    }

    @Property
    void duplicateUpdateKeepsQueueSizeOne(
            @ForAll @IntRange(min = 0, max = 300) int hr,
            @ForAll @IntRange(min = 0, max = 100) int spo2) {
        TriageEngine localEngine = new TriageEngine();
        localEngine.updatePatientPriority("P001", hr, spo2);
        localEngine.updatePatientPriority("P001", hr, spo2);
        assertEquals(1, localEngine.getQueueSize(),
                "Duplicate updates should not grow queue");
    }
}
