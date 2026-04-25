package com.vitasync.management;

import com.vitasync.exceptions.ResourceAllocationException;
import com.vitasync.model.Bed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages hospital bed allocation.
 * Initializes a fixed number of beds (default 20).
 * Thread-safe: all operations synchronized at bed level.
 */
public class BedManager {

    private static final Logger log = LoggerFactory.getLogger(BedManager.class);

    private final ConcurrentHashMap<String, Bed> beds = new ConcurrentHashMap<>();
    // patientId → bedId reverse lookup
    private final ConcurrentHashMap<String, String> patientBedMap = new ConcurrentHashMap<>();

    public BedManager(int totalBeds) {
        for (int i = 1; i <= totalBeds; i++) {
            String bedId = String.format("BED-%02d", i);
            beds.put(bedId, new Bed(bedId));
        }
        log.info("BedManager initialized with {} beds.", totalBeds);
    }

    /**
     * Assigns the first available free bed to the given patient.
     * Throws ResourceAllocationException if no beds are free.
     */
    public synchronized Bed assignBed(String patientId) throws ResourceAllocationException {
        if (patientBedMap.containsKey(patientId)) {
            String existingBed = patientBedMap.get(patientId);
            log.warn("Patient {} already assigned to {}", patientId, existingBed);
            return beds.get(existingBed);
        }

        Optional<Bed> freeBed = beds.values().stream()
                .filter(Bed::isFree)
                .findFirst();

        if (freeBed.isEmpty()) {
            throw new ResourceAllocationException("BED", "No free beds available for patient " + patientId);
        }

        Bed bed = freeBed.get();
        bed.assign(patientId);
        patientBedMap.put(patientId, bed.getBedId());
        log.info("Assigned {} to patient {}", bed.getBedId(), patientId);
        return bed;
    }

    /**
     * Releases the bed assigned to the given patient.
     */
    public synchronized void releaseBed(String patientId) {
        String bedId = patientBedMap.remove(patientId);
        if (bedId != null) {
            beds.get(bedId).release();
            log.info("Released {} from patient {}", bedId, patientId);
        }
    }

    public List<Bed> getAllBeds() {
        return new ArrayList<>(beds.values())
                .stream()
                .sorted((a, b) -> a.getBedId().compareTo(b.getBedId()))
                .toList();
    }

    public int getFreeBedCount() {
        return (int) beds.values().stream().filter(Bed::isFree).count();
    }

    public int getTotalBeds() { return beds.size(); }

    public String getBedForPatient(String patientId) {
        return patientBedMap.getOrDefault(patientId, "—");
    }
}
