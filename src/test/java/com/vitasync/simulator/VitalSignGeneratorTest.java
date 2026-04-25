package com.vitasync.simulator;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VitalSignGeneratorTest {

    @RepeatedTest(50)
    void generatedHeartRateInRange() {
        VitalSignGenerator gen = new VitalSignGenerator("P001", (id, hr, spo2) -> {});
        int hr = gen.generateHeartRate();
        assertTrue(hr >= 35 && hr <= 145, "HR out of range: " + hr);
    }

    @RepeatedTest(50)
    void generatedSpO2InRange() {
        VitalSignGenerator gen = new VitalSignGenerator("P001", (id, hr, spo2) -> {});
        int spo2 = gen.generateSpO2();
        assertTrue(spo2 >= 85 && spo2 <= 100, "SpO2 out of range: " + spo2);
    }

    @Test
    void runNotifiesListener() {
        List<String> received = new ArrayList<>();
        VitalSignGenerator gen = new VitalSignGenerator("P001",
                (id, hr, spo2) -> received.add(id + ":" + hr + ":" + spo2));
        gen.run();
        assertEquals(1, received.size());
        assertTrue(received.get(0).startsWith("P001:"));
    }

    @Test
    void patientIdIsCorrect() {
        VitalSignGenerator gen = new VitalSignGenerator("P999", (id, hr, spo2) -> {});
        assertEquals("P999", gen.getPatientId());
    }
}
