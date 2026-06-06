package com.drive2stars.spider.adapter.orchestrator;

import java.time.Instant;

public class OrchestratorBrakeStateDto {
    public String vin;
    public boolean emergencyBrakeActive;
    public boolean preEmergencyBrakeActive;
    public Instant updatedAt;
}
