package com.drive2stars.orchestrator.service;

import com.drive2stars.shared.messaging.BrakeMessage;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class BrakeStateService {

    public static class VehicleBrakeState {
        public final String vin;
        public final boolean emergencyBrakeActive;
        public final boolean preEmergencyBrakeActive;
        public final Instant updatedAt;

        VehicleBrakeState(String vin, boolean emergencyBrakeActive, boolean preEmergencyBrakeActive, Instant updatedAt) {
            this.vin = vin;
            this.emergencyBrakeActive = emergencyBrakeActive;
            this.preEmergencyBrakeActive = preEmergencyBrakeActive;
            this.updatedAt = updatedAt;
        }
    }

    private final ConcurrentHashMap<String, VehicleBrakeState> states = new ConcurrentHashMap<>();

    public void update(BrakeMessage msg) {
        if (msg == null || msg.vin == null) return;
        states.put(msg.vin, new VehicleBrakeState(
                msg.vin,
                msg.active,
                msg.preEmergencyBrake && !msg.active,
                msg.timestamp != null ? msg.timestamp : Instant.now()));
    }

    public Collection<VehicleBrakeState> getAllStates() {
        return states.values();
    }

    public VehicleBrakeState getState(String vin) {
        return states.get(vin);
    }
}
