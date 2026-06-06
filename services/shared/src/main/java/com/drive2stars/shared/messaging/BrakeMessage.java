package com.drive2stars.shared.messaging;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;

/**
 * RabbitMQ message published by BRAKENOW on exchange {@code d2s.vehicle.brake}.
 * Consumed by ORCHESTRATOR and SIMULATOR.
 * conditionTriggered: 1-3 = BRAKENOW local conditions, 4 = ORCHESTRATOR.
 * active=false signals the brake has been cleared (distance > 150m).
 */
@RegisterForReflection
public class BrakeMessage {

    public String vin;
    public boolean active;
    public boolean preEmergencyBrake;
    /** 1–3 = distance/closing conditions, 4 = ORCHESTRATOR plausibility check (GPS/SONAR deviation). */
    public int conditionTriggered;
    /** "BRAKENOW" or "ORCHESTRATOR" */
    public Source source;
    public Instant timestamp;

    public BrakeMessage() {}

    public BrakeMessage(String vin, boolean active, boolean preEmergencyBrake, int conditionTriggered, Source source, Instant timestamp) {
        this.vin = vin;
        this.active = active;
        this.preEmergencyBrake = preEmergencyBrake;
        this.conditionTriggered = conditionTriggered;
        this.source = source;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "BrakeMessage{vin='" + vin + "', active=" + active + ", preEmergency=" + preEmergencyBrake + ", condition=" + conditionTriggered + ", source='" + source + "', ts=" + timestamp + '}';
    }

    public enum Source {
        ORCHESTRATOR,
        BRAKENOW
    }
}
