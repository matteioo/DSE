package com.drive2stars.simulator.service;

import com.drive2stars.simulator.endpoint.SonarSensorReadingDto;
import com.drive2stars.simulator.mq.RawSonarPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SonarRawPublishingServiceTest {

  @Test
  void publishesOneRawMessagePerReading() {
    RecordingRawSonarPublisher publisher = new RecordingRawSonarPublisher();
    Instant measuredAt = Instant.parse("2026-06-05T10:00:00Z");
    List<SonarSensorReadingDto> readings = List.of(
        reading("VIN-1", "VIN-2", "FRONT", measuredAt),
        reading("VIN-2", "VIN-1", "BACK", measuredAt));

    int count = SonarRawPublishingService.publishRawReadings(readings, publisher);

    assertEquals(2, count);
    assertEquals(2, publisher.published.size());
    SonarSensorReadingDto first = publisher.published.getFirst();
    assertEquals("VIN-1", first.vin);
    assertEquals("VIN-2", first.targetVin);
    assertEquals("FRONT", first.direction);
    assertNotNull(first.radarDistanceMeters);
    assertNotNull(first.lidarDistanceMeters);
    assertNotNull(first.ultrasonicDistanceMeters);
    assertEquals(measuredAt, first.measuredAt);
  }

  private static SonarSensorReadingDto reading(String vin, String targetVin, String direction,
      Instant measuredAt) {
    return new SonarSensorReadingDto(vin, targetVin, direction, new BigDecimal("10.30"),
        new BigDecimal("9.90"), new BigDecimal("10.05"), measuredAt);
  }

  private static class RecordingRawSonarPublisher extends RawSonarPublisher {
    List<SonarSensorReadingDto> published = new ArrayList<>();

    @Override
    public void publish(SonarSensorReadingDto reading) {
      published.add(reading);
    }
  }
}
