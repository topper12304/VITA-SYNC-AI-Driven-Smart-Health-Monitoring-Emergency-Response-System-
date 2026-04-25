package com.vitasync.simulator;

import com.vitasync.exceptions.CriticalVitalException;

/**
 * Validates vital sign readings against defined thresholds.
 *
 * Critical thresholds (trigger CriticalVitalException):
 *   - Heart Rate < 40 bpm  (bradycardia)
 *   - Heart Rate > 140 bpm (tachycardia)
 *   - SpO2 < 90%           (hypoxemia)
 *
 * Valid ranges (reject with IllegalArgumentException):
 *   - Heart Rate: 0–300 bpm
 *   - SpO2: 0–100%
 */
public class VitalSignValidator {

    private static final int HR_CRITICAL_LOW  = 40;
    private static final int HR_CRITICAL_HIGH = 140;
    private static final int SPO2_CRITICAL_LOW = 90;

    private static final int HR_VALID_MIN  = 0;
    private static final int HR_VALID_MAX  = 300;
    private static final int SPO2_VALID_MIN = 0;
    private static final int SPO2_VALID_MAX = 100;

    private VitalSignValidator() {}

    /**
     * Validates that the vital signs are within the acceptable physical range.
     * Throws IllegalArgumentException for out-of-range values.
     */
    public static void validateRange(int heartRate, int spO2) {
        if (heartRate < HR_VALID_MIN || heartRate > HR_VALID_MAX) {
            throw new IllegalArgumentException(
                    String.format("Heart rate %d is outside valid range [%d, %d]",
                            heartRate, HR_VALID_MIN, HR_VALID_MAX));
        }
        if (spO2 < SPO2_VALID_MIN || spO2 > SPO2_VALID_MAX) {
            throw new IllegalArgumentException(
                    String.format("SpO2 %d is outside valid range [%d, %d]",
                            spO2, SPO2_VALID_MIN, SPO2_VALID_MAX));
        }
    }

    /**
     * Validates vital signs and throws CriticalVitalException if critical thresholds are breached.
     * Also validates the physical range first.
     */
    public static void validateAndThrow(String patientId, int heartRate, int spO2)
            throws CriticalVitalException {
        validateRange(heartRate, spO2);

        if (heartRate < HR_CRITICAL_LOW) {
            throw new CriticalVitalException(patientId, "Heart Rate", heartRate,
                    "below " + HR_CRITICAL_LOW + " bpm");
        }
        if (heartRate > HR_CRITICAL_HIGH) {
            throw new CriticalVitalException(patientId, "Heart Rate", heartRate,
                    "above " + HR_CRITICAL_HIGH + " bpm");
        }
        if (spO2 < SPO2_CRITICAL_LOW) {
            throw new CriticalVitalException(patientId, "SpO2", spO2,
                    "below " + SPO2_CRITICAL_LOW + "%");
        }
    }

    /**
     * Returns true if the vitals are in the critical zone (without throwing).
     */
    public static boolean isCritical(int heartRate, int spO2) {
        return heartRate < HR_CRITICAL_LOW || heartRate > HR_CRITICAL_HIGH || spO2 < SPO2_CRITICAL_LOW;
    }
}
