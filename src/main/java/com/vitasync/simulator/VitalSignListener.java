package com.vitasync.simulator;

/**
 * Callback interface for receiving vital sign updates from the VitalsSimulator.
 */
public interface VitalSignListener {

    /**
     * Called whenever new vital signs are generated for a patient.
     *
     * @param patientId the patient's unique identifier
     * @param heartRate heart rate in beats per minute
     * @param spO2      blood oxygen saturation percentage
     */
    void onVitalSignUpdate(String patientId, int heartRate, int spO2);
}
