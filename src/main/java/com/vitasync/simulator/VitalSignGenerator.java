package com.vitasync.simulator;

import com.vitasync.exceptions.CriticalVitalException;
import com.vitasync.triage.CriticalVitalHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Runnable that generates a single vital sign reading for a patient and notifies the listener.
 * Heart rate range: 35–145 bpm (includes critical zones below 40 and above 140).
 * SpO2 range: 85–100% (includes critical zone below 90%).
 *
 * If a CriticalVitalHandler is provided, critical events are logged and triage priority updated.
 */
public class VitalSignGenerator implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(VitalSignGenerator.class);

    private static final int HR_MIN = 35;
    private static final int HR_MAX = 145;
    private static final int SPO2_MIN = 85;
    private static final int SPO2_MAX = 100;

    private final String patientId;
    private final VitalSignListener listener;
    private final Random random;
    private CriticalVitalHandler criticalVitalHandler;

    public VitalSignGenerator(String patientId, VitalSignListener listener) {
        this.patientId = patientId;
        this.listener = listener;
        this.random = new Random();
    }

    public void setCriticalVitalHandler(CriticalVitalHandler handler) {
        this.criticalVitalHandler = handler;
    }

    @Override
    public void run() {
        int heartRate = generateHeartRate();
        int spO2 = generateSpO2();
        log.debug("Generated vitals for {}: HR={}, SpO2={}", patientId, heartRate, spO2);
        try {
            VitalSignValidator.validateAndThrow(patientId, heartRate, spO2);
        } catch (CriticalVitalException e) {
            log.warn("CRITICAL VITAL — {}", e.getMessage());
            if (criticalVitalHandler != null) {
                criticalVitalHandler.handleCriticalVital(e, heartRate, spO2);
            }
            // Fall through: still deliver the reading so records can be updated
        } catch (Exception e) {
            log.error("Error validating vitals for patient {}: {}", patientId, e.getMessage(), e);
            return;
        }
        listener.onVitalSignUpdate(patientId, heartRate, spO2);
    }

    int generateHeartRate() {
        return HR_MIN + random.nextInt(HR_MAX - HR_MIN + 1);
    }

    int generateSpO2() {
        return SPO2_MIN + random.nextInt(SPO2_MAX - SPO2_MIN + 1);
    }

    public String getPatientId() { return patientId; }
}
