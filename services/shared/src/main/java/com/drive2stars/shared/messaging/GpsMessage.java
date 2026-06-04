package com.drive2stars.shared.messaging;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * RabbitMQ message published by WHEREAMI on exchange {@code d2s.vehicle.gps}.
 * Consumed by UTRACKED and ORCHESTRATOR.
 */
@RegisterForReflection
public class GpsMessage {

    public String vin;
    public BigDecimal latitude;
    public BigDecimal longitude;
    public Instant timestamp;

    public GpsMessage() {}

    public GpsMessage(String vin, BigDecimal latitude, BigDecimal longitude, Instant timestamp) {
        this.vin = vin;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "GpsMessage{vin='" + vin + "', lat=" + latitude + ", lon=" + longitude + ", ts=" + timestamp + '}';
    }
}