package com.drive2stars.simulator.service;

import com.drive2stars.shared.messaging.BrakeMessage;
import com.drive2stars.shared.messaging.SimulationScenarioCommand;
import com.drive2stars.shared.messaging.SimulatorVehicleStateMessage;
import com.drive2stars.simulator.endpoint.SonarSensorReadingDto;
import com.drive2stars.simulator.endpoint.VehicleGpsDto;
import com.drive2stars.simulator.endpoint.VehicleStateDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class VehicleSimulationService {

  private static final BigDecimal BASE_LATITUDE = new BigDecimal("48.2082000");
  private static final BigDecimal BASE_LONGITUDE = new BigDecimal("16.3738000");
  private static final BigDecimal METERS_PER_LONGITUDE_DEGREE = new BigDecimal("74400");

  private final String vin;
  private final int linePosition;
  private final double initialGapMeters;
  private final double scenarioBaseSpeedMetersPerSecond;
  private final double scenarioSpeedDeltaMetersPerSecond;
  private final double scenario3LeadSpeedMetersPerSecond;
  private final double scenario3BoostDistanceMeters;
  private final double resumeDistanceMeters;
  private final Duration peerStateMaxAge;
  private final Clock clock;

  private final Map<String, VehicleSnapshot> peerSnapshots = new ConcurrentHashMap<>();

  private SimulationScenarioCommand.Scenario scenario = SimulationScenarioCommand.Scenario.IDLE;
  private double positionMeters;
  private double speedMetersPerSecond;
  private boolean emergencyBrakeActive;
  private boolean scenario3LeadBoosted;
  private Instant lastTick;

  @Inject
  public VehicleSimulationService(
      @ConfigProperty(name = "simulator.vin") String vin,
      @ConfigProperty(name = "simulator.position") int linePosition,
      @ConfigProperty(name = "simulator.initial.gap.meters") double initialGapMeters,
      @ConfigProperty(name = "simulator.scenario.base.speed.mps") double scenarioBaseSpeedMetersPerSecond,
      @ConfigProperty(name = "simulator.scenario.speed.delta.mps") double scenarioSpeedDeltaMetersPerSecond,
      @ConfigProperty(name = "simulator.scenario3.lead.speed.mps") double scenario3LeadSpeedMetersPerSecond,
      @ConfigProperty(name = "simulator.scenario3.boost.distance.meters") double scenario3BoostDistanceMeters,
      @ConfigProperty(name = "simulator.resume.distance.meters") double resumeDistanceMeters,
      @ConfigProperty(name = "simulator.peer.state.max.age.seconds") long peerStateMaxAgeSeconds) {
    this(vin, linePosition, initialGapMeters, scenarioBaseSpeedMetersPerSecond,
        scenarioSpeedDeltaMetersPerSecond, scenario3LeadSpeedMetersPerSecond,
        scenario3BoostDistanceMeters, resumeDistanceMeters, Duration.ofSeconds(peerStateMaxAgeSeconds),
        Clock.systemUTC());
  }

  public VehicleSimulationService(String vin, int linePosition, double initialGapMeters,
      double scenarioBaseSpeedMetersPerSecond, double scenarioSpeedDeltaMetersPerSecond,
      double scenario3LeadSpeedMetersPerSecond, double scenario3BoostDistanceMeters,
      double resumeDistanceMeters, Duration peerStateMaxAge, Clock clock) {
    if (linePosition < 1) {
      throw new IllegalArgumentException("SIMULATOR_POSITION must be >= 1");
    }
    this.vin = vin;
    this.linePosition = linePosition;
    this.initialGapMeters = initialGapMeters;
    this.scenarioBaseSpeedMetersPerSecond = scenarioBaseSpeedMetersPerSecond;
    this.scenarioSpeedDeltaMetersPerSecond = scenarioSpeedDeltaMetersPerSecond;
    this.scenario3LeadSpeedMetersPerSecond = scenario3LeadSpeedMetersPerSecond;
    this.scenario3BoostDistanceMeters = scenario3BoostDistanceMeters;
    this.resumeDistanceMeters = resumeDistanceMeters;
    this.peerStateMaxAge = peerStateMaxAge;
    this.clock = clock;
    resetToIdle();
  }

  public synchronized List<VehicleStateDto> getVehicles() {
    advance();
    return snapshots().stream()
        .map(this::toState)
        .toList();
  }

  public synchronized VehicleGpsDto getGps(String requestedVin) {
    advance();
    if (!vin.equals(requestedVin)) {
      throw new NotFoundException("Unknown VIN for this simulator: " + requestedVin);
    }
    VehicleStateDto state = toState(currentSnapshot());
    return new VehicleGpsDto(state.vin, state.latitude, state.longitude);
  }

  public synchronized List<SonarSensorReadingDto> getSonarReadings(String requestedVin) {
    advance();
    if (!vin.equals(requestedVin)) {
      throw new NotFoundException("Unknown VIN for this simulator: " + requestedVin);
    }
    return sonarReadingsFor(currentSnapshot(), snapshots(), now());
  }

  public synchronized List<SonarSensorReadingDto> getAllSonarReadings() {
    advance();
    Instant measuredAt = now();
    List<VehicleSnapshot> snapshots = snapshots();
    return snapshots.stream()
        .flatMap(snapshot -> sonarReadingsFor(snapshot, snapshots, measuredAt).stream())
        .toList();
  }

  public synchronized List<SonarSensorReadingDto> getOwnSonarReadings() {
    advance();
    return sonarReadingsFor(currentSnapshot(), snapshots(), now());
  }

  public synchronized SimulatorVehicleStateMessage currentStateMessage() {
    advance();
    return toStateMessage(currentSnapshot());
  }

  public synchronized void applyScenarioCommand(SimulationScenarioCommand command) {
    if (command == null || command.scenario == null) {
      return;
    }
    advance();
    if (command.scenario == SimulationScenarioCommand.Scenario.RESET) {
      resetToIdle();
      return;
    }
    if (command.scenario == SimulationScenarioCommand.Scenario.IDLE) {
      scenario = SimulationScenarioCommand.Scenario.IDLE;
      emergencyBrakeActive = false;
      scenario3LeadBoosted = false;
      speedMetersPerSecond = 0.0;
      lastTick = now();
      return;
    }
    scenario = command.scenario;
    emergencyBrakeActive = false;
    scenario3LeadBoosted = false;
    speedMetersPerSecond = desiredScenarioSpeed();
    lastTick = now();
  }

  public synchronized void applyBrakeMessage(BrakeMessage message) {
    if (message == null || message.vin == null || !vin.equals(message.vin)) {
      return;
    }
    advance();
    emergencyBrakeActive = message.active;
    if (message.active) {
      speedMetersPerSecond = 0.0;
    } else {
      speedMetersPerSecond = desiredScenarioSpeed();
    }
  }

  public void updatePeerState(SimulatorVehicleStateMessage message) {
    if (message == null || message.vin == null || vin.equals(message.vin)) {
      return;
    }
    peerSnapshots.put(message.vin, new VehicleSnapshot(
        message.vin,
        message.linePosition,
        message.positionMeters,
        message.speedMetersPerSecond,
        message.scenario,
        message.emergencyBrakeActive,
        message.timestamp == null ? now() : message.timestamp));
  }

  private void resetToIdle() {
    scenario = SimulationScenarioCommand.Scenario.IDLE;
    positionMeters = initialPositionMeters();
    speedMetersPerSecond = 0.0;
    emergencyBrakeActive = false;
    scenario3LeadBoosted = false;
    peerSnapshots.clear();
    lastTick = now();
  }

  private void advance() {
    Instant currentTime = now();
    if (lastTick == null) {
      lastTick = currentTime;
      return;
    }

    double elapsedSeconds = Duration.between(lastTick, currentTime).toMillis() / 1000.0;
    if (elapsedSeconds <= 0) {
      return;
    }

    if (emergencyBrakeActive && shouldResumeAfterBrake()) {
      emergencyBrakeActive = false;
    }

    speedMetersPerSecond = emergencyBrakeActive ? 0.0 : desiredScenarioSpeed();
    positionMeters += speedMetersPerSecond * elapsedSeconds;
    lastTick = currentTime;
  }

  private boolean shouldResumeAfterBrake() {
    if (scenario != SimulationScenarioCommand.Scenario.SCENARIO_2) {
      return false;
    }
    return nearestAheadDistanceMeters() > resumeDistanceMeters;
  }

  private double desiredScenarioSpeed() {
    return switch (scenario) {
      case IDLE, RESET -> 0.0;
      case SCENARIO_1 -> scenarioBaseSpeedMetersPerSecond;
      case SCENARIO_2 -> lineSpeed();
      case SCENARIO_3 -> scenario3Speed();
    };
  }

  private double scenario3Speed() {
    if (linePosition == 1) {
      double nearestBehind = nearestBehindDistanceMeters();
      if (scenario3LeadBoosted || nearestBehind <= scenario3BoostDistanceMeters) {
        scenario3LeadBoosted = true;
        return scenario3LeadSpeedMetersPerSecond;
      }
    }
    return lineSpeed();
  }

  private double lineSpeed() {
    return scenarioBaseSpeedMetersPerSecond
        + Math.max(0, linePosition - 1) * scenarioSpeedDeltaMetersPerSecond;
  }

  private double nearestAheadDistanceMeters() {
    return snapshots().stream()
        .filter(snapshot -> snapshot.positionMeters() > positionMeters)
        .mapToDouble(snapshot -> snapshot.positionMeters() - positionMeters)
        .min()
        .orElse(Double.POSITIVE_INFINITY);
  }

  private double nearestBehindDistanceMeters() {
    return snapshots().stream()
        .filter(snapshot -> snapshot.positionMeters() < positionMeters)
        .mapToDouble(snapshot -> positionMeters - snapshot.positionMeters())
        .min()
        .orElse(Double.POSITIVE_INFINITY);
  }

  private List<SonarSensorReadingDto> sonarReadingsFor(VehicleSnapshot source,
      List<VehicleSnapshot> snapshots, Instant measuredAt) {
    List<VehicleSnapshot> sorted = snapshots.stream()
        .sorted(Comparator.comparingDouble(VehicleSnapshot::positionMeters))
        .toList();
    int index = findSnapshotIndex(sorted, source.vin());
    if (index < 0) {
      return List.of();
    }

    List<SonarSensorReadingDto> readings = new ArrayList<>();
    if (index + 1 < sorted.size()) {
      readings.add(toSonarReading(source, sorted.get(index + 1), "FRONT", measuredAt));
    }
    if (index > 0) {
      readings.add(toSonarReading(source, sorted.get(index - 1), "BACK", measuredAt));
    }
    return readings;
  }

  private List<VehicleSnapshot> snapshots() {
    Instant currentTime = now();
    List<VehicleSnapshot> snapshots = new ArrayList<>();
    for (VehicleSnapshot peer : peerSnapshots.values()) {
      extrapolatePeerSnapshot(peer, currentTime).ifPresent(snapshots::add);
    }
    snapshots.add(currentSnapshot());
    return snapshots;
  }

  private java.util.Optional<VehicleSnapshot> extrapolatePeerSnapshot(VehicleSnapshot peer,
      Instant currentTime) {
    if (peer.timestamp() == null) {
      return java.util.Optional.empty();
    }
    Duration age = Duration.between(peer.timestamp(), currentTime);
    if (age.isNegative()) {
      age = Duration.ZERO;
    }
    if (peerStateMaxAge != null && age.compareTo(peerStateMaxAge) > 0) {
      peerSnapshots.remove(peer.vin(), peer);
      return java.util.Optional.empty();
    }

    double elapsedSeconds = age.toMillis() / 1000.0;
    return java.util.Optional.of(new VehicleSnapshot(
        peer.vin(),
        peer.linePosition(),
        peer.positionMeters() + peer.speedMetersPerSecond() * elapsedSeconds,
        peer.speedMetersPerSecond(),
        peer.scenario(),
        peer.emergencyBrakeActive(),
        currentTime));
  }

  private VehicleSnapshot currentSnapshot() {
    return new VehicleSnapshot(vin, linePosition, positionMeters, speedMetersPerSecond,
        scenario, emergencyBrakeActive, now());
  }

  private VehicleStateDto toState(VehicleSnapshot snapshot) {
    BigDecimal longitudeOffset = BigDecimal.valueOf(snapshot.positionMeters())
        .divide(METERS_PER_LONGITUDE_DEGREE, 7, RoundingMode.HALF_UP);
    return new VehicleStateDto(
        snapshot.vin(),
        BASE_LATITUDE,
        BASE_LONGITUDE.add(longitudeOffset).setScale(7, RoundingMode.HALF_UP),
        BigDecimal.valueOf(snapshot.speedMetersPerSecond()).setScale(2, RoundingMode.HALF_UP));
  }

  private SimulatorVehicleStateMessage toStateMessage(VehicleSnapshot snapshot) {
    return new SimulatorVehicleStateMessage(
        snapshot.vin(),
        snapshot.linePosition(),
        snapshot.scenario(),
        snapshot.positionMeters(),
        snapshot.speedMetersPerSecond(),
        snapshot.emergencyBrakeActive(),
        snapshot.timestamp());
  }

  private int findSnapshotIndex(List<VehicleSnapshot> snapshots, String requestedVin) {
    for (int i = 0; i < snapshots.size(); i++) {
      if (snapshots.get(i).vin().equals(requestedVin)) {
        return i;
      }
    }
    return -1;
  }

  private SonarSensorReadingDto toSonarReading(VehicleSnapshot source, VehicleSnapshot target,
      String direction, Instant measuredAt) {
    BigDecimal trueDistance = BigDecimal.valueOf(Math.abs(target.positionMeters() - source.positionMeters()))
        .setScale(2, RoundingMode.HALF_UP);
    return new SonarSensorReadingDto(
        source.vin(),
        direction,
        offsetDistance(trueDistance, "0.30"),
        offsetDistance(trueDistance, "-0.10"),
        offsetDistance(trueDistance, "0.05"),
        measuredAt);
  }

  private BigDecimal offsetDistance(BigDecimal distance, String offset) {
    BigDecimal adjusted = distance.add(new BigDecimal(offset));
    if (adjusted.signum() < 0) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    return adjusted.setScale(2, RoundingMode.HALF_UP);
  }

  private double initialPositionMeters() {
    return -(linePosition - 1) * initialGapMeters;
  }

  private Instant now() {
    return Instant.now(clock);
  }

  private record VehicleSnapshot(String vin, int linePosition, double positionMeters,
      double speedMetersPerSecond, SimulationScenarioCommand.Scenario scenario,
      boolean emergencyBrakeActive, Instant timestamp) {}
}
