package com.drive2stars.brakenow.service;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;

@RegisterForReflection
public class BrakeState {

    public final String  vin;
    public final boolean emergencyBrakeActive;
    public final boolean preEmergencyBrake;
    public final int     conditionTriggered;
    public final Instant updatedAt;

    public BrakeState(String vin, boolean emergencyBrakeActive, boolean preEmergencyBrake,
                      int conditionTriggered, Instant updatedAt) {
        this.vin                  = vin;
        this.emergencyBrakeActive = emergencyBrakeActive;
        this.preEmergencyBrake    = preEmergencyBrake;
        this.conditionTriggered   = conditionTriggered;
        this.updatedAt            = updatedAt;
    }
}
