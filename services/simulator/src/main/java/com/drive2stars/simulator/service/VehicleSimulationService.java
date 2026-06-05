package com.drive2stars.simulator.service;

import com.drive2stars.simulator.endpoint.VehicleGpsDto;
import com.drive2stars.simulator.endpoint.SonarSensorReadingDto;
import com.drive2stars.simulator.endpoint.VehicleStateDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class VehicleSimulationService {

  private static final BigDecimal BASE_LATITUDE = new BigDecimal("48.2082000");
  private static final BigDecimal BASE_LONGITUDE = new BigDecimal("16.3738000");
  private static final BigDecimal METERS_PER_LONGITUDE_DEGREE = new BigDecimal("74400");

  private final Instant startedAt = Instant.now(Clock.systemUTC());

  private final List<VehicleSeed> vehicles = List.of(
      new VehicleSeed("D2S-DEMO-VIN-001", new BigDecimal("0"), new BigDecimal("20")),
      new VehicleSeed("D2S-DEMO-VIN-002", new BigDecimal("-120"), new BigDecimal("24"))
  );

  public List<VehicleStateDto> getVehicles() {
    return vehicles.stream()
        .map(this::toState)
        .toList();
  }

  public VehicleGpsDto getGps(String vin) {
    return vehicles.stream()
        .filter(vehicle -> vehicle.vin().equals(vin))
        .findFirst()
        .map(this::toGps)
        .orElseThrow(() -> new NotFoundException("Unknown VIN: " + vin));
  }

  public List<SonarSensorReadingDto> getSonarReadings(String vin) {
    List<VehicleSnapshot> snapshots = snapshots();
    return getSonarReadings(snapshots, vin, Instant.now(Clock.systemUTC()));
  }

  public List<SonarSensorReadingDto> getAllSonarReadings() {
    List<VehicleSnapshot> snapshots = snapshots();
    Instant measuredAt = Instant.now(Clock.systemUTC());
    return snapshots.stream()
        .map(VehicleSnapshot::vin)
        .flatMap(vin -> getSonarReadings(snapshots, vin, measuredAt).stream())
        .toList();
  }

  private List<SonarSensorReadingDto> getSonarReadings(List<VehicleSnapshot> snapshots, String vin,
      Instant measuredAt) {
    int index = findSnapshotIndex(snapshots, vin);
    if (index < 0) {
      throw new NotFoundException("Unknown VIN: " + vin);
    }

    List<SonarSensorReadingDto> readings = new ArrayList<>();
    VehicleSnapshot source = snapshots.get(index);
    if (index + 1 < snapshots.size()) {
      readings.add(toSonarReading(source, snapshots.get(index + 1), "FRONT", measuredAt));
    }
    if (index > 0) {
      readings.add(toSonarReading(source, snapshots.get(index - 1), "BACK", measuredAt));
    }
    return readings;
  }

  private VehicleGpsDto toGps(VehicleSeed vehicle) {
    VehicleStateDto state = toState(vehicle);
    return new VehicleGpsDto(state.vin, state.latitude, state.longitude);
  }

  private VehicleStateDto toState(VehicleSeed vehicle) {
    VehicleSnapshot snapshot = toSnapshot(vehicle, elapsedSeconds());
    return new VehicleStateDto(
        snapshot.vin(),
        BASE_LATITUDE,
        snapshot.longitude(),
        snapshot.speedMetersPerSecond());
  }

  private List<VehicleSnapshot> snapshots() {
    long elapsedSeconds = elapsedSeconds();
    return vehicles.stream()
        .map(vehicle -> toSnapshot(vehicle, elapsedSeconds))
        .sorted(Comparator.comparing(VehicleSnapshot::positionMeters))
        .toList();
  }

  private VehicleSnapshot toSnapshot(VehicleSeed vehicle, long elapsedSeconds) {
    BigDecimal positionMeters = vehicle.startOffsetMeters()
        .add(vehicle.speedMetersPerSecond().multiply(BigDecimal.valueOf(elapsedSeconds)));
    BigDecimal longitudeOffset = positionMeters.divide(METERS_PER_LONGITUDE_DEGREE, 7, RoundingMode.HALF_UP);
    return new VehicleSnapshot(
        vehicle.vin(),
        positionMeters,
        BASE_LONGITUDE.add(longitudeOffset).setScale(7, RoundingMode.HALF_UP),
        vehicle.speedMetersPerSecond());
  }

  private int findSnapshotIndex(List<VehicleSnapshot> snapshots, String vin) {
    for (int i = 0; i < snapshots.size(); i++) {
      if (snapshots.get(i).vin().equals(vin)) {
        return i;
      }
    }
    return -1;
  }

  private SonarSensorReadingDto toSonarReading(VehicleSnapshot source, VehicleSnapshot target,
      String direction, Instant measuredAt) {
    BigDecimal trueDistance = target.positionMeters()
        .subtract(source.positionMeters())
        .abs()
        .setScale(2, RoundingMode.HALF_UP);
    return new SonarSensorReadingDto(
        source.vin(),
        target.vin(),
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

  private long elapsedSeconds() {
    return java.time.Duration.between(startedAt, Instant.now(Clock.systemUTC())).toSeconds();
  }

  private record VehicleSeed(String vin, BigDecimal startOffsetMeters, BigDecimal speedMetersPerSecond) {}

  private record VehicleSnapshot(String vin, BigDecimal positionMeters, BigDecimal longitude,
      BigDecimal speedMetersPerSecond) {}
}
