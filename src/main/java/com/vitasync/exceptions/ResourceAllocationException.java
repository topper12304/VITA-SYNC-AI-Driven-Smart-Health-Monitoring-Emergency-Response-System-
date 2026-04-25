package com.vitasync.exceptions;

/**
 * Thrown when a hospital resource cannot be allocated (e.g., already in use or not found).
 */
public class ResourceAllocationException extends Exception {

    private final String resourceId;
    private final String reason;

    public ResourceAllocationException(String resourceId, String reason) {
        super(String.format("Failed to allocate resource '%s': %s", resourceId, reason));
        this.resourceId = resourceId;
        this.reason = reason;
    }

    public String getResourceId() { return resourceId; }
    public String getReason() { return reason; }
}
