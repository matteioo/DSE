package com.drive2stars.sonar.service;

import com.drive2stars.sonar.endpoint.SimulatorSonarClient;
import com.drive2stars.sonar.endpoint.SonarReadingDto;
import com.drive2stars.sonar.endpoint.SonarSensorReadingDto;
import com.drive2stars.sonar.mq.SonarPublisher;
import com.drive2stars.shared.messaging.DistanceMessage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class SonarPollingServiceTest {

  @Test
  void vehicleModePollsConfiguredVinAndPublishesFusedReadings() {
    RecordingSimulatorSonarClient simulatorSonarClient = new RecordingSimulatorSonarClient();
    RecordingSonarPublisher sonarPublisher = new RecordingSonarPublisher();
    SonarPollingService sonarPollingService = service("vehicle", "D2S-DEMO-VIN-002",
        simulatorSonarClient, sonarPublisher);

    sonarPollingService.pollOnce();

    assertEquals("D2S-DEMO-VIN-002", simulatorSonarClient.requestedVin);
    assertFalse(simulatorSonarClient.allReadingsRequested);
    assertEquals(1, sonarPublisher.published.size());
    assertEquals("D2S-DEMO-VIN-002", sonarPublisher.published.getFirst().vin);
    assertEquals("FRONT", sonarPublisher.published.getFirst().direction);
    assertEquals(new BigDecimal("100.00"), sonarPublisher.published.getFirst().distanceMeters);
  }

  @Test
  void backendModeDoesNotPollSimulatorOnSchedule() {
    RecordingSimulatorSonarClient simulatorSonarClient = new RecordingSimulatorSonarClient();
    RecordingSonarPublisher sonarPublisher = new RecordingSonarPublisher();
    SonarPollingService sonarPollingService = service("backend", "ignored-vin",
        simulatorSonarClient, sonarPublisher);

    sonarPollingService.pollOnce();

    assertNull(simulatorSonarClient.requestedVin);
    assertFalse(simulatorSonarClient.allReadingsRequested);
    assertEquals(0, sonarPublisher.published.size());
  }

  @Test
  void vehicleRestStyleReadGetsFreshSimulatorDataAndCachesWithoutPublishing() {
    RecordingSimulatorSonarClient simulatorSonarClient = new RecordingSimulatorSonarClient();
    RecordingSonarPublisher sonarPublisher = new RecordingSonarPublisher();
    SonarPollingService sonarPollingService = service("vehicle", "D2S-DEMO-VIN-002",
        simulatorSonarClient, sonarPublisher);

    List<SonarReadingDto> readings = sonarPollingService.readingsForVin("D2S-DEMO-VIN-002");

    assertEquals("D2S-DEMO-VIN-002", simulatorSonarClient.requestedVin);
    assertEquals(1, readings.size());
    assertEquals(new BigDecimal("100.00"), readings.getFirst().distanceMeters);
    assertEquals(0, sonarPublisher.published.size());
  }

  @Test
  void backendDistanceMessageCachesLatestWithoutPublishing() {
    RecordingSimulatorSonarClient simulatorSonarClient = new RecordingSimulatorSonarClient();
    RecordingSonarPublisher sonarPublisher = new RecordingSonarPublisher();
    SonarPollingService sonarPollingService = service("backend", "ignored-vin",
        simulatorSonarClient, sonarPublisher);

    sonarPollingService.processBackendDistanceMessage(new DistanceMessage(
        "D2S-DEMO-VIN-002", 100.0, 2.5, DistanceMessage.Direction.FRONT,
        Instant.parse("2026-06-05T10:00:00Z")));

    assertNull(simulatorSonarClient.requestedVin);
    assertEquals(0, sonarPublisher.published.size());
    List<SonarReadingDto> cached = sonarPollingService.readingsForVin("D2S-DEMO-VIN-002");
    assertEquals(1, cached.size());
    assertEquals("FRONT", cached.getFirst().direction);
    assertEquals(new BigDecimal("100.00"), cached.getFirst().distanceMeters);
    assertEquals(new BigDecimal("2.50"), cached.getFirst().distanceChangeMetersPerSecond);
  }

  @Test
  void distanceChangeIsPositiveWhenGapClosesAndNegativeWhenGapOpens() {
    HashMap<String, SonarPollingService.PreviousReading> previousReadings = new HashMap<>();
    Instant t0 = Instant.parse("2026-06-03T10:00:00Z");
    Instant t5 = Instant.parse("2026-06-03T10:00:05Z");
    Instant t10 = Instant.parse("2026-06-03T10:00:10Z");

    SonarPollingService.fuseReadings(List.of(reading("VIN-2", "FRONT", "50.00", t0)),
        previousReadings);
    SonarReadingDto closing = SonarPollingService.fuseReadings(
        List.of(reading("VIN-2", "FRONT", "45.00", t5)), previousReadings).getFirst();
    SonarReadingDto opening = SonarPollingService.fuseReadings(
        List.of(reading("VIN-2", "FRONT", "55.00", t10)), previousReadings).getFirst();

    assertEquals(new BigDecimal("1.00"), closing.distanceChangeMetersPerSecond);
    assertEquals(new BigDecimal("-2.00"), opening.distanceChangeMetersPerSecond);
  }

  private static SonarPollingService service(String mode, String vin,
      RecordingSimulatorSonarClient simulatorSonarClient, RecordingSonarPublisher sonarPublisher) {
    SonarPollingService service = new SonarPollingService();
    service.mode = mode;
    service.vin = vin;
    service.simulatorSonarClient = simulatorSonarClient;
    service.sonarPublisher = sonarPublisher;
    return service;
  }

  private static SonarSensorReadingDto reading(String vin, String direction, String distance,
      Instant measuredAt) {
    BigDecimal distanceMeters = new BigDecimal(distance);
    return new SonarSensorReadingDto(vin, direction, distanceMeters, distanceMeters,
        distanceMeters, measuredAt);
  }

  private static class RecordingSimulatorSonarClient implements SimulatorSonarClient {
    String requestedVin;
    boolean allReadingsRequested;

    @Override
    public List<SonarSensorReadingDto> getVehicleReadings(String vin) {
      requestedVin = vin;
      return List.of(reading(vin, "FRONT", "100.00", Instant.now()));
    }

    @Override
    public List<SonarSensorReadingDto> getAllReadings() {
      allReadingsRequested = true;
      return List.of(
          reading("D2S-DEMO-VIN-001", "BACK", "100.00", Instant.now()),
          reading("D2S-DEMO-VIN-002", "FRONT", "100.00", Instant.now()));
    }
  }

  private static class RecordingSonarPublisher extends SonarPublisher {
    List<SonarReadingDto> published = new ArrayList<>();

    @Override
    public void publish(SonarReadingDto reading) {
      published.add(reading);
    }
  }
}
