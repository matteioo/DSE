package com.drive2stars.shared.messaging;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;

/**
 * RabbitMQ state broadcast published by each SIMULATOR instance.
 */
@RegisterForReflection
public class SimulatorVehicleStateMessage {

    public String vin;
    public int linePosition;
    public SimulationScenarioCommand.Scenario scenario;
    public double positionMeters;
    public double speedMetersPerSecond;
    public boolean emergencyBrakeActive;
    public Instant timestamp;

    public SimulatorVehicleStateMessage() {}

    public SimulatorVehicleStateMessage(String vin, int linePosition,
                                        SimulationScenarioCommand.Scenario scenario,
                                        double positionMeters, double speedMetersPerSecond,
                                        boolean emergencyBrakeActive, Instant timestamp) {
        this.vin = vin;
        this.linePosition = linePosition;
        this.scenario = scenario;
        this.positionMeters = positionMeters;
        this.speedMetersPerSecond = speedMetersPerSecond;
        this.emergencyBrakeActive = emergencyBrakeActive;
        this.timestamp = timestamp;
    }
}
