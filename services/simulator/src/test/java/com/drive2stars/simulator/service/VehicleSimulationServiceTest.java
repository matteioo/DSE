package com.drive2stars.simulator.service;

import com.drive2stars.shared.messaging.BrakeMessage;
import com.drive2stars.shared.messaging.SimulationScenarioCommand;
import com.drive2stars.shared.messaging.SimulatorVehicleStateMessage;
import com.drive2stars.simulator.endpoint.SonarSensorReadingDto;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleSimulationServiceTest {

  @Test
  void idleKeepsVehiclesStandingAtDifferentLinePositions() {
    MutableClock clock = clock();
    VehicleSimulationService lead = simulator("VIN-1", 1, clock);
    VehicleSimulationService second = simulator("VIN-2", 2, clock);
    VehicleSimulationService third = simulator("VIN-3", 3, clock);

    clock.advance(Duration.ofSeconds(10));

    assertEquals(0.0, lead.currentStateMessage().positionMeters);
    assertEquals(-80.0, second.currentStateMessage().positionMeters);
    assertEquals(-160.0, third.currentStateMessage().positionMeters);
    assertEquals(0.0, lead.currentStateMessage().speedMetersPerSecond);
    assertEquals(0.0, second.currentStateMessage().speedMetersPerSecond);
    assertEquals(0.0, third.currentStateMessage().speedMetersPerSecond);
  }

  @Test
  void scenario1UsesSameSpeedForEveryLinePosition() {
    MutableClock clock = clock();
    VehicleSimulationService lead = simulator("VIN-1", 1, clock);
    VehicleSimulationService follower = simulator("VIN-2", 2, clock);

    lead.applyScenarioCommand(command(SimulationScenarioCommand.Scenario.SCENARIO_1));
    follower.applyScenarioCommand(command(SimulationScenarioCommand.Scenario.SCENARIO_1));
    clock.advance(Duration.ofSeconds(5));

    assertEquals(20.0, lead.currentStateMessage().speedMetersPerSecond);
    assertEquals(20.0, follower.currentStateMessage().speedMetersPerSecond);
    assertEquals(80.0, lead.currentStateMessage().positionMeters - follower.currentStateMessage().positionMeters);
  }

  @Test
  void scenarioCommandStartsWithoutRepositioning() {
    MutableClock clock = clock();
    VehicleSimulationService follower = simulator("VIN-2", 2, clock);
    follower.applyScenarioCommand(command(SimulationScenarioCommand.Scenario.SCENARIO_1));
    clock.advance(Duration.ofSeconds(5));
    double movedPosition = follower.currentStateMessage().positionMeters;

    follower.applyScenarioCommand(command(SimulationScenarioCommand.Scenario.SCENARIO_2));

    assertEquals(movedPosition, follower.currentStateMessage().positionMeters);
    assertEquals(25.0, follower.currentStateMessage().speedMetersPerSecond);
  }

  @Test
  void idleStopsWithoutRepositioningAndResetRepositions() {
    MutableClock clock = clock();
    VehicleSimulationService follower = simulator("VIN-2", 2, clock);
    follower.applyScenarioCommand(command(SimulationScenarioCommand.Scenario.SCENARIO_1));
    clock.advance(Duration.ofSeconds(5));
    double movedPosition = follower.currentStateMessage().positionMeters;

    follower.applyScenarioCommand(command(SimulationScenarioCommand.Scenario.IDLE));
    assertEquals(movedPosition, follower.currentStateMessage().positionMeters);
    assertEquals(0.0, follower.currentStateMessage().speedMetersPerSecond);

    follower.applyScenarioCommand(command(SimulationScenarioCommand.Scenario.RESET));
    assertEquals(-80.0, follower.currentStateMessage().positionMeters);
    assertEquals(0.0, follower.currentStateMessage().speedMetersPerSecond);
  }

  @Test
  void scenario2MakesCarsFurtherBackFaster() {
    MutableClock clock = clock();
    VehicleSimulationService lead = simulator("VIN-1", 1, clock);
    VehicleSimulationService second = simulator("VIN-2", 2, clock);
    VehicleSimulationService third = simulator("VIN-3", 3, clock);

    lead.applyScenarioCommand(command(SimulationScenarioCommand.Scenario.SCENARIO_2));
    second.applyScenarioCommand(command(SimulationScenarioCommand.Scenario.SCENARIO_2));
    third.applyScenarioCommand(command(SimulationScenarioCommand.Scenario.SCENARIO_2));
    clock.advance(Duration.ofSeconds(10));

    assertEquals(20.0, lead.currentStateMessage().speedMetersPerSecond);
    assertEquals(25.0, second.currentStateMessage().speedMetersPerSecond);
    assertEquals(30.0, third.currentStateMessage().speedMetersPerSecond);
    assertTrue(lead.currentStateMessage().positionMeters - second.currentStateMessage().positionMeters < 80.0);
    assertTrue(second.currentStateMessage().positionMeters - third.currentStateMessage().positionMeters < 80.0);
  }

  @Test
  void scenario3LeadAcceleratesWhenFollowerGetsClose() {
    MutableClock clock = clock();
    VehicleSimulationService lead = simulator("VIN-1", 1, clock);
    lead.applyScenarioCommand(command(SimulationScenarioCommand.Scenario.SCENARIO_3));
    lead.updatePeerState(state("VIN-2", 2, -25.0, 0.0,
        SimulationScenarioCommand.Scenario.SCENARIO_3));

    clock.advance(Duration.ofSeconds(1));

    assertEquals(35.0, lead.currentStateMessage().speedMetersPerSecond);
  }

  @Test
  void ownBrakeMessageStopsVehicleAndOtherBrakeMessageIsIgnored() {
    MutableClock clock = clock();
    VehicleSimulationService follower = simulator("VIN-2", 2, clock);
    follower.applyScenarioCommand(command(SimulationScenarioCommand.Scenario.SCENARIO_2));

    follower.applyBrakeMessage(brake("VIN-1", true));
    assertEquals(25.0, follower.currentStateMessage().speedMetersPerSecond);

    follower.applyBrakeMessage(brake("VIN-2", true));
    assertEquals(0.0, follower.currentStateMessage().speedMetersPerSecond);
  }

  @Test
  void scenario2ResumesAfterBrakeWhenFrontVehicleIsFarEnoughAway() {
    MutableClock clock = clock();
    VehicleSimulationService follower = simulator("VIN-2", 2, clock);
    follower.applyScenarioCommand(command(SimulationScenarioCommand.Scenario.SCENARIO_2));
    follower.applyBrakeMessage(brake("VIN-2", true));
    follower.updatePeerState(state("VIN-1", 1, 100.0, 20.0,
        SimulationScenarioCommand.Scenario.SCENARIO_2));

    clock.advance(Duration.ofSeconds(1));

    assertEquals(25.0, follower.currentStateMessage().speedMetersPerSecond);
  }

  @Test
  void scenario2KeepsBrakeActiveWhenFrontVehicleStateExpires() {
    MutableClock clock = clock();
    VehicleSimulationService follower = simulator("VIN-2", 2, clock);
    follower.applyScenarioCommand(command(SimulationScenarioCommand.Scenario.SCENARIO_2));
    follower.applyBrakeMessage(brake("VIN-2", true));
    follower.updatePeerState(state("VIN-1", 1, 100.0, 20.0,
        SimulationScenarioCommand.Scenario.SCENARIO_2));

    clock.advance(Duration.ofSeconds(4));

    assertEquals(0.0, follower.currentStateMessage().speedMetersPerSecond);
  }

  @Test
  void sonarReportsNearestVehicleAheadAndBehind() {
    MutableClock clock = clock();
    VehicleSimulationService second = simulator("VIN-2", 2, clock);
    second.updatePeerState(state("VIN-1", 1, 0.0, 0.0,
        SimulationScenarioCommand.Scenario.IDLE));
    second.updatePeerState(state("VIN-3", 3, -160.0, 0.0,
        SimulationScenarioCommand.Scenario.IDLE));

    List<SonarSensorReadingDto> readings = second.getOwnSonarReadings();

    assertEquals(2, readings.size());
    assertTrue(readings.stream().anyMatch(r -> "FRONT".equals(r.direction)));
    assertTrue(readings.stream().anyMatch(r -> "BACK".equals(r.direction)));
  }

  @Test
  void peerStateIsExtrapolatedToCurrentTimeForSonar() {
    MutableClock clock = clock();
    VehicleSimulationService follower = simulator("VIN-2", 2, clock);
    follower.applyScenarioCommand(command(SimulationScenarioCommand.Scenario.SCENARIO_1));
    follower.updatePeerState(state("VIN-1", 1, 0.0, 20.0,
        SimulationScenarioCommand.Scenario.SCENARIO_1));

    clock.advance(Duration.ofSeconds(1));
    List<SonarSensorReadingDto> readings = follower.getOwnSonarReadings();

    assertEquals(1, readings.size());
    assertEquals("FRONT", readings.getFirst().direction);
    assertEquals(new BigDecimal("80.30"), readings.getFirst().radarDistanceMeters);
  }

  @Test
  void stalePeerStateIsIgnored() {
    MutableClock clock = clock();
    VehicleSimulationService follower = simulator("VIN-2", 2, clock);
    follower.updatePeerState(state("VIN-1", 1, 0.0, 0.0,
        SimulationScenarioCommand.Scenario.IDLE));

    clock.advance(Duration.ofSeconds(4));

    assertEquals(0, follower.getOwnSonarReadings().size());
  }

  @Test
  void resetClearsPeerStateCache() {
    MutableClock clock = clock();
    VehicleSimulationService follower = simulator("VIN-2", 2, clock);
    follower.updatePeerState(state("VIN-1", 1, 0.0, 0.0,
        SimulationScenarioCommand.Scenario.IDLE));

    follower.applyScenarioCommand(command(SimulationScenarioCommand.Scenario.RESET));

    assertEquals(0, follower.getOwnSonarReadings().size());
  }

  private static VehicleSimulationService simulator(String vin, int position, Clock clock) {
    return new VehicleSimulationService(vin, position, 80.0, 20.0, 5.0, 35.0,
        30.0, 150.0, Duration.ofSeconds(3), clock);
  }

  private static SimulationScenarioCommand command(SimulationScenarioCommand.Scenario scenario) {
    return new SimulationScenarioCommand(scenario, Instant.parse("2026-06-05T10:00:00Z"));
  }

  private static BrakeMessage brake(String vin, boolean active) {
    return new BrakeMessage(vin, active, 1, BrakeMessage.Source.ORCHESTRATOR,
        Instant.parse("2026-06-05T10:00:00Z"));
  }

  private static SimulatorVehicleStateMessage state(String vin, int linePosition,
      double positionMeters, double speedMetersPerSecond,
      SimulationScenarioCommand.Scenario scenario) {
    return new SimulatorVehicleStateMessage(vin, linePosition, scenario, positionMeters,
        speedMetersPerSecond, false, Instant.parse("2026-06-05T10:00:00Z"));
  }

  private static MutableClock clock() {
    return new MutableClock(Instant.parse("2026-06-05T10:00:00Z"));
  }

  private static class MutableClock extends Clock {
    private Instant instant;

    MutableClock(Instant instant) {
      this.instant = instant;
    }

    void advance(Duration duration) {
      instant = instant.plus(duration);
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
