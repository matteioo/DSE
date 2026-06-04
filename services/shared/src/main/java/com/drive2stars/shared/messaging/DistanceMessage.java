package com.drive2stars.shared.messaging;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;

/**
 * RabbitMQ message published by SONAR (vehicle instances) on exchange {@code d2s.vehicle.distance}.
 * Consumed by BRAKENOW (own VIN only) and ORCHESTRATOR (all VINs).
 */
@RegisterForReflection
public class DistanceMessage {

    public String vin;
    public double distanceMeters;
    /** Negative = approaching, positive = receding. */
    public double changeRateMps;
    public Instant timestamp;

    public DistanceMessage() {}

    public DistanceMessage(String vin, double distanceMeters, double changeRateMps, Instant timestamp) {
        this.vin = vin;
        this.distanceMeters = distanceMeters;
        this.changeRateMps = changeRateMps;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "DistanceMessage{vin='" + vin + "', dist=" + distanceMeters + "m, rate=" + changeRateMps + "m/s, ts=" + timestamp + '}';
    }
}
