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
    /** Negative = receding, positive = approaching. */
    public double changeRateMps;
    public Direction direction;
    public Instant timestamp;

    public DistanceMessage(String vin, double distanceMeters, double changeRateMps, Direction direction, Instant timestamp) {
        this.vin = vin;
        this.distanceMeters = distanceMeters;
        this.changeRateMps = changeRateMps;
        this.direction = direction;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "DistanceMessage{vin='" + vin + "', dist=" + distanceMeters + "m, rate=" + changeRateMps + "m/s, direction=" + direction + ", ts=" + timestamp + '}';
    }

    public enum Direction {
        FRONT,
        BACK
    }
}
