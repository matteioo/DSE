package com.drive2stars.shared.messaging;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;

/**
 * RabbitMQ command consumed by SIMULATOR instances on exchange {@code d2s.simulator.scenario}.
 */
@RegisterForReflection
public class SimulationScenarioCommand {

    public Scenario scenario;
    public Instant timestamp;

    public SimulationScenarioCommand() {}

    public SimulationScenarioCommand(Scenario scenario, Instant timestamp) {
        this.scenario = scenario;
        this.timestamp = timestamp;
    }

    public enum Scenario {
        IDLE,
        RESET,
        SCENARIO_1,
        SCENARIO_2,
        SCENARIO_3
    }
}
