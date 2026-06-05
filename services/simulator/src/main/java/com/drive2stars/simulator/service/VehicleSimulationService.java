package com.drive2stars.simulator.service;

import com.drive2stars.simulator.endpoint.VehicleGpsDto;
import com.drive2stars.simulator.endpoint.VehicleStateDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
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

  private VehicleGpsDto toGps(VehicleSeed vehicle) {
    VehicleStateDto state = toState(vehicle);
    return new VehicleGpsDto(state.vin, state.latitude, state.longitude);
  }

  private VehicleStateDto toState(VehicleSeed vehicle) {
    BigDecimal traveledMeters = vehicle.startOffsetMeters()
        .add(vehicle.speedMetersPerSecond().multiply(BigDecimal.valueOf(elapsedSeconds())));
    BigDecimal longitudeOffset = traveledMeters.divide(METERS_PER_LONGITUDE_DEGREE, 7, RoundingMode.HALF_UP);
    return new VehicleStateDto(
        vehicle.vin(),
        BASE_LATITUDE,
        BASE_LONGITUDE.add(longitudeOffset).setScale(7, RoundingMode.HALF_UP),
        vehicle.speedMetersPerSecond());
  }

  private long elapsedSeconds() {
    return java.time.Duration.between(startedAt, Instant.now(Clock.systemUTC())).toSeconds();
  }

  private record VehicleSeed(String vin, BigDecimal startOffsetMeters, BigDecimal speedMetersPerSecond) {}
}
