package com.drive2stars.sonar.service;

import com.drive2stars.sonar.endpoint.SimulatorSonarClient;
import com.drive2stars.sonar.endpoint.SonarReadingDto;
import com.drive2stars.sonar.endpoint.SonarSensorReadingDto;
import com.drive2stars.sonar.mq.SonarPublisher;
import com.drive2stars.shared.messaging.DistanceMessage;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SonarPollingService {

  private static final Logger LOG = Logger.getLogger(SonarPollingService.class);
  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

  private final Map<String, PreviousReading> previousReadings = new ConcurrentHashMap<>();
  private final Map<String, SonarReadingDto> latestReadings = new ConcurrentHashMap<>();

  @Inject
  @RestClient
  SimulatorSonarClient simulatorSonarClient;

  @Inject
  SonarPublisher sonarPublisher;

  @ConfigProperty(name = "sonar.mode")
  String mode;

  @ConfigProperty(name = "sonar.vin")
  String vin;

  @Scheduled(every = "{sonar.poll.interval}")
  void publishCurrentReadings() {
    if (!isVehicleMode()) {
      return;
    }
    publishVehicleReadings(vin);
  }

  public void pollOnce() {
    publishCurrentReadings();
  }

  public List<SonarReadingDto> readingsForVin(String requestedVin) {
    if (isVehicleMode()) {
      return readFreshVehicleReadings(requestedVin);
    }
    return latestReadings.values().stream()
        .filter(reading -> requestedVin.equals(reading.vin))
        .toList();
  }

  private List<SonarReadingDto> readFreshVehicleReadings(String requestedVin) {
    try {
      List<SonarSensorReadingDto> sensorReadings = simulatorSonarClient.getVehicleReadings(requestedVin);
      return fuseAndCache(sensorReadings);
    } catch (NotFoundException e) {
      return List.of();
    } catch (WebApplicationException e) {
      if (e.getResponse() != null && e.getResponse().getStatus() == 404) {
        return List.of();
      }
      LOG.errorf("Could not read fresh SONAR data from simulator: HTTP %d",
          e.getResponse() == null ? -1 : e.getResponse().getStatus());
      return List.of();
    } catch (RuntimeException e) {
      LOG.errorf(e, "Could not read fresh SONAR data from simulator");
      return List.of();
    }
  }

  public void processBackendDistanceMessage(DistanceMessage message) {
    if (!isBackendMode()) {
      return;
    }
    SonarReadingDto reading = new SonarReadingDto(
        message.vin,
        message.direction.name(),
        toScaledBigDecimal(message.distanceMeters),
        toScaledBigDecimal(message.changeRateMps),
        message.timestamp);
    latestReadings.put(key(reading.vin, reading.direction), reading);
  }

  public static List<SonarReadingDto> fuseReadings(List<SonarSensorReadingDto> sensorReadings,
      Map<String, PreviousReading> previousReadings) {
    List<SonarReadingDto> fusedReadings = new ArrayList<>();
    if (sensorReadings == null) {
      return fusedReadings;
    }

    for (SonarSensorReadingDto sensorReading : sensorReadings) {
      BigDecimal distanceMeters = fuseDistance(sensorReading);
      if (distanceMeters == null) {
        continue;
      }
      Instant measuredAt = sensorReading.measuredAt == null ? Instant.now() : sensorReading.measuredAt;
      String key = key(sensorReading.vin, sensorReading.direction);
      PreviousReading previousReading = previousReadings.put(key,
          new PreviousReading(distanceMeters, measuredAt));
      BigDecimal distanceChangeMetersPerSecond =
          calculateDistanceChange(distanceMeters, measuredAt, previousReading);
      fusedReadings.add(new SonarReadingDto(
          sensorReading.vin,
          sensorReading.direction,
          distanceMeters,
          distanceChangeMetersPerSecond,
          measuredAt));
    }
    return fusedReadings;
  }

  public static BigDecimal fuseDistance(SonarSensorReadingDto reading) {
    List<BigDecimal> distances = Stream.of(
        reading.radarDistanceMeters,
        reading.lidarDistanceMeters,
        reading.ultrasonicDistanceMeters)
        .filter(distance -> distance != null && distance.signum() >= 0)
        .toList();
    if (distances.isEmpty()) {
      return null;
    }

    BigDecimal sum = distances.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    return sum.divide(BigDecimal.valueOf(distances.size()), 2, RoundingMode.HALF_UP);
  }

  public static BigDecimal calculateDistanceChange(BigDecimal currentDistance, Instant measuredAt,
      PreviousReading previousReading) {
    if (previousReading == null) {
      return ZERO;
    }

    long elapsedMillis = Duration.between(previousReading.measuredAt(), measuredAt).toMillis();
    if (elapsedMillis <= 0) {
      return ZERO;
    }

    BigDecimal elapsedSeconds = BigDecimal.valueOf(elapsedMillis)
        .divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
    return previousReading.distanceMeters()
        .subtract(currentDistance)
        .divide(elapsedSeconds, 2, RoundingMode.HALF_UP);
  }

  public List<SonarReadingDto> getAllLatestReadings() {
    return List.copyOf(latestReadings.values());
  }

  private static String key(String vin, String direction) {
    return vin + "|" + direction;
  }

  private static BigDecimal toScaledBigDecimal(double value) {
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
  }

  private void publishVehicleReadings(String requestedVin) {
    try {
      List<SonarSensorReadingDto> sensorReadings = simulatorSonarClient.getVehicleReadings(requestedVin);
      for (SonarReadingDto reading : fuseAndCache(sensorReadings)) {
        sonarPublisher.publish(reading);
      }
    } catch (WebApplicationException e) {
      LOG.errorf("Could not read SONAR data from simulator: HTTP %d",
          e.getResponse() == null ? -1 : e.getResponse().getStatus());
    } catch (RuntimeException e) {
      LOG.errorf(e, "Could not read SONAR data from simulator");
    }
  }

  private List<SonarReadingDto> fuseAndCache(List<SonarSensorReadingDto> sensorReadings) {
    List<SonarReadingDto> readings = fuseReadings(sensorReadings, previousReadings);
    for (SonarReadingDto reading : readings) {
      latestReadings.put(key(reading.vin, reading.direction), reading);
    }
    return readings;
  }

  private boolean isVehicleMode() {
    return "vehicle".equalsIgnoreCase(mode);
  }

  private boolean isBackendMode() {
    return "backend".equalsIgnoreCase(mode);
  }

  public record PreviousReading(BigDecimal distanceMeters, Instant measuredAt) {}
}
