package com.drive2stars.brakenow.endpoint;

import com.drive2stars.brakenow.service.BrakeState;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;

@RegisterForReflection
public class BrakeStatusDto {

    public String vin;
    public boolean emergencyBrakeActive;
    public boolean preEmergencyBrake;
    public int conditionTriggered;
    public Instant updatedAt;

    public static BrakeStatusDto from(BrakeState s) {
        BrakeStatusDto dto = new BrakeStatusDto();
        dto.vin = s.vin;
        dto.emergencyBrakeActive = s.emergencyBrakeActive;
        dto.preEmergencyBrake = s.preEmergencyBrake;
        dto.conditionTriggered = s.conditionTriggered;
        dto.updatedAt = s.updatedAt;
        return dto;
    }
}
