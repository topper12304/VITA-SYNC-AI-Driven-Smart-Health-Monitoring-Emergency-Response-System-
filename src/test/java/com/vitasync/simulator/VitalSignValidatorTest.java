package com.vitasync.simulator;

import com.vitasync.exceptions.CriticalVitalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class VitalSignValidatorTest {

    // ── validateRange ────────────────────────────────────────────────────────

    @Test
    void normalVitalsPassRange() {
        assertDoesNotThrow(() -> VitalSignValidator.validateRange(75, 98));
    }

    @ParameterizedTest
    @CsvSource({"-1,98", "301,98", "75,-1", "75,101"})
    void outOfRangeThrowsIllegalArgument(int hr, int spo2) {
        assertThrows(IllegalArgumentException.class,
                () -> VitalSignValidator.validateRange(hr, spo2));
    }

    // ── validateAndThrow ─────────────────────────────────────────────────────

    @Test
    void normalVitalsNoCriticalException() {
        assertDoesNotThrow(() -> VitalSignValidator.validateAndThrow("P001", 75, 98));
    }

    @Test
    void lowHRThrowsCritical() {
        CriticalVitalException ex = assertThrows(CriticalVitalException.class,
                () -> VitalSignValidator.validateAndThrow("P001", 39, 98));
        assertEquals("P001", ex.getPatientId());
        assertEquals("Heart Rate", ex.getVitalType());
        assertEquals(39, ex.getValue());
    }

    @Test
    void highHRThrowsCritical() {
        CriticalVitalException ex = assertThrows(CriticalVitalException.class,
                () -> VitalSignValidator.validateAndThrow("P002", 141, 98));
        assertEquals("Heart Rate", ex.getVitalType());
        assertEquals(141, ex.getValue());
    }

    @Test
    void lowSpO2ThrowsCritical() {
        CriticalVitalException ex = assertThrows(CriticalVitalException.class,
                () -> VitalSignValidator.validateAndThrow("P003", 75, 89));
        assertEquals("SpO2", ex.getVitalType());
        assertEquals(89, ex.getValue());
    }

    @Test
    void boundaryHR40IsNotCritical() {
        assertDoesNotThrow(() -> VitalSignValidator.validateAndThrow("P001", 40, 98));
    }

    @Test
    void boundaryHR140IsNotCritical() {
        assertDoesNotThrow(() -> VitalSignValidator.validateAndThrow("P001", 140, 98));
    }

    @Test
    void boundarySpO290IsNotCritical() {
        assertDoesNotThrow(() -> VitalSignValidator.validateAndThrow("P001", 75, 90));
    }

    // ── isCritical ───────────────────────────────────────────────────────────

    @Test
    void isCriticalTrueForLowHR() {
        assertTrue(VitalSignValidator.isCritical(39, 98));
    }

    @Test
    void isCriticalTrueForHighHR() {
        assertTrue(VitalSignValidator.isCritical(141, 98));
    }

    @Test
    void isCriticalTrueForLowSpO2() {
        assertTrue(VitalSignValidator.isCritical(75, 89));
    }

    @Test
    void isCriticalFalseForNormalVitals() {
        assertFalse(VitalSignValidator.isCritical(75, 98));
    }
}
