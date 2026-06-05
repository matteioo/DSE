package com.drive2stars.simulator.mq;

import com.drive2stars.shared.messaging.SimulationScenarioCommand;
import com.drive2stars.simulator.service.SimulatorStatePublishingService;
import com.drive2stars.simulator.service.VehicleSimulationService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulationScenarioConsumerTest {

  @Test
  void scenarioCommandPublishesStateImmediately() {
    MutableClock clock = new MutableClock(Instant.parse("2026-06-05T10:00:00Z"));
    VehicleSimulationService simulationService = new VehicleSimulationService("VIN-1", 1, 80.0,
        20.0, 5.0, 35.0, 30.0, 150.0, Duration.ofSeconds(3), clock);
    RecordingStatePublishingService publishingService = new RecordingStatePublishingService();
    SimulationScenarioConsumer consumer = new SimulationScenarioConsumer();
    consumer.vehicleSimulationService = simulationService;
    consumer.simulatorStatePublishingService = publishingService;

    consumer.process("SCENARIO_1".getBytes(StandardCharsets.UTF_8));

    assertEquals(1, publishingService.publishCalls);
    assertEquals(SimulationScenarioCommand.Scenario.SCENARIO_1,
        simulationService.currentStateMessage().scenario);
  }

  private static class RecordingStatePublishingService extends SimulatorStatePublishingService {
    int publishCalls;

    @Override
    public void publishOnce() {
      publishCalls++;
    }
  }

  private static class MutableClock extends Clock {
    private Instant instant;

    MutableClock(Instant instant) {
      this.instant = instant;
    }

    @Override
    public ZoneId getZone() {
      return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant;
    }
  }
}
