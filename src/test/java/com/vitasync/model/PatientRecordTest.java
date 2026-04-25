package com.vitasync.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PatientRecordTest {

    @Test
    void updateVitalsSetsCurrentValues() {
        PatientRecord record = new PatientRecord("P001", "Alice", 30);
        record.updateVitals(80, 97);
        assertEquals(80, record.getCurrentHeartRate());
        assertEquals(97, record.getCurrentSpO2());
    }

    @Test
    void updateVitalsAppendsToHistory() {
        PatientRecord record = new PatientRecord("P001", "Alice", 30);
        record.updateVitals(75, 98);
        record.updateVitals(80, 97);
        assertEquals(2, record.getHistory().size());
    }

    @Test
    void historyIsUnmodifiable() {
        PatientRecord record = new PatientRecord("P001", "Alice", 30);
        record.updateVitals(75, 98);
        assertThrows(UnsupportedOperationException.class,
                () -> record.getHistory().clear());
    }

    @Test
    void equalityBasedOnPatientId() {
        PatientRecord a = new PatientRecord("P001", "Alice", 30);
        PatientRecord b = new PatientRecord("P001", "Bob",   25);
        assertEquals(a, b);
    }

    @Test
    void differentIdsNotEqual() {
        PatientRecord a = new PatientRecord("P001", "Alice", 30);
        PatientRecord b = new PatientRecord("P002", "Alice", 30);
        assertNotEquals(a, b);
    }

    @Test
    void lastUpdatedChangesAfterUpdate() throws InterruptedException {
        PatientRecord record = new PatientRecord("P001", "Alice", 30);
        var before = record.getLastUpdated();
        Thread.sleep(10);
        record.updateVitals(75, 98);
        assertTrue(record.getLastUpdated().isAfter(before));
    }
}
