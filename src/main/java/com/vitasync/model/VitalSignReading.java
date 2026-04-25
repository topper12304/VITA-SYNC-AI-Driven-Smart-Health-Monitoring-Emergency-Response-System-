package com.vitasync.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a single vital sign reading for a patient at a specific point in time.
 */
public class VitalSignReading {

    private final int heartRate;   // beats per minute
    private final int spO2;        // blood oxygen saturation percentage
    private final LocalDateTime timestamp;

    public VitalSignReading(int heartRate, int spO2, LocalDateTime timestamp) {
        this.heartRate = heartRate;
        this.spO2 = spO2;
        this.timestamp = timestamp;
    }

    public VitalSignReading(int heartRate, int spO2) {
        this(heartRate, spO2, LocalDateTime.now());
    }

    public int getHeartRate() { return heartRate; }
    public int getSpO2() { return spO2; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VitalSignReading)) return false;
        VitalSignReading that = (VitalSignReading) o;
        return heartRate == that.heartRate &&
               spO2 == that.spO2 &&
               Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(heartRate, spO2, timestamp);
    }

    @Override
    public String toString() {
        return String.format("VitalSignReading{HR=%d bpm, SpO2=%d%%, time=%s}", heartRate, spO2, timestamp);
    }
}
