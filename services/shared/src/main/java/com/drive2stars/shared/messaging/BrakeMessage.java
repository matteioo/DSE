package com.drive2stars.shared.messaging;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;

/**
 * RabbitMQ message published by BRAKENOW on exchange {@code d2s.vehicle.brake}.
 * Consumed by ORCHESTRATOR and SIMULATOR.
 * <br>
 * conditionTriggered: 1-3 = BRAKENOW local conditions, 4 = ORCHESTRATOR plausibility check.
 * active=false signals the brake has been cleared (distance > 100-200m).
 */
@RegisterForReflection
public class BrakeMessage {

    public String vin;
    public boolean active;
    /** 1–3 for BRAKENOW-evaluated conditions, 4 for ORCHESTRATOR plausibility check. */
    public int conditionTriggered;
    /** "BRAKENOW" or "ORCHESTRATOR" */
    public Source source;
    public Instant timestamp;

    public BrakeMessage() {}

    public BrakeMessage(String vin, boolean active, int conditionTriggered, Source source, Instant timestamp) {
        this.vin = vin;
        this.active = active;
        this.conditionTriggered = conditionTriggered;
        this.source = source;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "BrakeMessage{vin='" + vin + "', active=" + active + ", condition=" + conditionTriggered + ", source='" + source.toString() + "', ts=" + timestamp + '}';
    }

    public enum Source {
        ORCHESTRATOR,
        BRAKENOW
    }
}
