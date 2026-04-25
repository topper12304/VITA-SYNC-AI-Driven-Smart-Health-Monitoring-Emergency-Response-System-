package com.vitasync.exceptions;

/**
 * Thrown when a patient's vital signs reach critical thresholds:
 * - Heart Rate < 40 bpm or > 140 bpm
 * - SpO2 < 90%
 */
public class CriticalVitalException extends Exception {

    private final String patientId;
    private final String vitalType;
    private final int value;
    private final String threshold;

    public CriticalVitalException(String patientId, String vitalType, int value, String threshold) {
        super(String.format("Critical vital detected for patient %s: %s = %d (%s)",
                patientId, vitalType, value, threshold));
        this.patientId = patientId;
        this.vitalType = vitalType;
        this.value = value;
        this.threshold = threshold;
    }

    public String getPatientId() { return patientId; }
    public String getVitalType() { return vitalType; }
    public int getValue() { return value; }
    public String getThreshold() { return threshold; }
}
