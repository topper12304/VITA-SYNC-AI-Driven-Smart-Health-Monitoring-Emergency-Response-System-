package com.vitasync.simulator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class VitalsSimulatorTest {

    private VitalsSimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new VitalsSimulator((id, hr, spo2) -> {});
    }

    @AfterEach
    void tearDown() {
        simulator.close();
    }

    @Test
    void startMonitoringAddsPatient() {
        simulator.startMonitoring("P001");
        assertTrue(simulator.isMonitoring("P001"));
    }

    @Test
    void stopMonitoringRemovesPatient() {
        simulator.startMonitoring("P001");
        simulator.stopMonitoring("P001");
        assertFalse(simulator.isMonitoring("P001"));
    }

    @Test
    void duplicateStartIsNoOp() {
        simulator.startMonitoring("P001");
        simulator.startMonitoring("P001"); // should not throw
        assertEquals(1, simulator.getMonitoredPatients().size());
    }

    @Test
    void listenerReceivesUpdatesWithinTimeout() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        VitalsSimulator sim = new VitalsSimulator((id, hr, spo2) -> latch.countDown());
        sim.startMonitoring("P001");
        boolean received = latch.await(3, TimeUnit.SECONDS);
        sim.close();
        assertTrue(received, "Listener should receive at least one update within 3 seconds");
    }

    @Test
    void multiplePatientsConcurrently() throws InterruptedException {
        AtomicInteger count = new AtomicInteger(0);
        VitalsSimulator sim = new VitalsSimulator((id, hr, spo2) -> count.incrementAndGet());
        sim.startMonitoring("P001");
        sim.startMonitoring("P002");
        sim.startMonitoring("P003");
        Thread.sleep(2000);
        sim.close();
        assertTrue(count.get() >= 3, "Should receive updates from multiple patients");
    }

    @Test
    void closeStopsAllMonitoring() {
        simulator.startMonitoring("P001");
        simulator.startMonitoring("P002");
        simulator.close();
        // After close, no tasks should remain
        assertTrue(simulator.getMonitoredPatients().isEmpty());
    }
}
