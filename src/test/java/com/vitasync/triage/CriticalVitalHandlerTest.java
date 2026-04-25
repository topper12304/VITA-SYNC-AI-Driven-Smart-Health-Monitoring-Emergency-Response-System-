package com.vitasync.triage;

import com.vitasync.exceptions.CriticalVitalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

class CriticalVitalHandlerTest {

    private TriageEngine triageEngine;
    private CriticalVitalHandler handler;

    @BeforeEach
    void setUp() {
        triageEngine = Mockito.mock(TriageEngine.class);
        handler = new CriticalVitalHandler(triageEngine);
    }

    @Test
    void handleCriticalVitalCallsTriageUpdate() {
        CriticalVitalException ex = new CriticalVitalException("P001", "Heart Rate", 35, "below 40 bpm");
        handler.handleCriticalVital(ex, 35, 98);
        verify(triageEngine, times(1)).updatePatientPriority("P001", 35, 98);
    }

    @Test
    void logCriticalEventDoesNotThrow() {
        CriticalVitalException ex = new CriticalVitalException("P002", "SpO2", 88, "below 90%");
        // Should not throw — just logs
        handler.logCriticalEvent(ex);
    }

    @Test
    void updateTriagePriorityDelegatesToEngine() {
        handler.updateTriagePriority("P003", 145, 92);
        verify(triageEngine).updatePatientPriority("P003", 145, 92);
    }

    @Test
    void handleCriticalVitalWithNullAlertStoreDoesNotThrow() {
        // handler created without AlertStore (null) — should not NPE
        CriticalVitalException ex = new CriticalVitalException("P004", "Heart Rate", 150, "above 140 bpm");
        handler.handleCriticalVital(ex, 150, 95);
        verify(triageEngine).updatePatientPriority("P004", 150, 95);
    }
}
