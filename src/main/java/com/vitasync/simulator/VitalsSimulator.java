package com.vitasync.simulator;

import com.vitasync.triage.CriticalVitalHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Manages concurrent vital sign generation for multiple patients using a ScheduledExecutorService.
 * Each patient gets an independent scheduled task that fires every 1–2 seconds.
 */
public class VitalsSimulator implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(VitalsSimulator.class);

    private static final int UPDATE_INTERVAL_MS = 1500; // 1.5 seconds (within 2s requirement)
    private static final int MAX_POOL_SIZE = 50;

    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> patientTasks = new ConcurrentHashMap<>();
    private final VitalSignListener listener;
    private CriticalVitalHandler criticalVitalHandler;

    public VitalsSimulator(VitalSignListener listener) {
        this.listener = listener;
        this.scheduler = Executors.newScheduledThreadPool(MAX_POOL_SIZE);
    }

    public void setCriticalVitalHandler(CriticalVitalHandler handler) {
        this.criticalVitalHandler = handler;
    }

    /**
     * Starts periodic vital sign generation for the given patient.
     * If already monitoring, this is a no-op.
     */
    public void startMonitoring(String patientId) {
        if (patientTasks.containsKey(patientId)) {
            log.warn("Already monitoring patient: {}", patientId);
            return;
        }
        VitalSignGenerator generator = new VitalSignGenerator(patientId, listener);
        if (criticalVitalHandler != null) {
            generator.setCriticalVitalHandler(criticalVitalHandler);
        }
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                generator,
                0,
                UPDATE_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
        patientTasks.put(patientId, future);
        log.info("Started monitoring patient: {}", patientId);
    }

    /**
     * Stops vital sign generation for the given patient.
     */
    public void stopMonitoring(String patientId) {
        ScheduledFuture<?> future = patientTasks.remove(patientId);
        if (future != null) {
            future.cancel(false);
            log.info("Stopped monitoring patient: {}", patientId);
        } else {
            log.warn("No active monitoring found for patient: {}", patientId);
        }
    }

    /**
     * Returns the set of currently monitored patient IDs.
     */
    public Set<String> getMonitoredPatients() {
        return patientTasks.keySet();
    }

    public boolean isMonitoring(String patientId) {
        return patientTasks.containsKey(patientId);
    }

    /**
     * Gracefully shuts down the scheduler and cancels all patient tasks.
     */
    @Override
    public void close() {
        patientTasks.values().forEach(f -> f.cancel(false));
        patientTasks.clear();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("VitalsSimulator shut down.");
    }
}
