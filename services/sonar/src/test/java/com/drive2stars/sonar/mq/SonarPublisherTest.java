package com.drive2stars.sonar.mq;

import com.drive2stars.sonar.endpoint.SonarReadingDto;
import com.drive2stars.shared.messaging.DistanceMessage;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SonarPublisherTest {

  @Test
  void mapsSonarReadingToDistanceMessageContract() {
    Instant measuredAt = Instant.parse("2026-06-05T12:00:00Z");
    SonarReadingDto reading = new SonarReadingDto(
        "VIN-1",
        "VIN-2",
        "front",
        new BigDecimal("12.34"),
        new BigDecimal("5.67"),
        measuredAt);

    DistanceMessage message = new SonarPublisher().toDistanceMessage(reading);

    assertEquals("VIN-1", message.vin);
    assertEquals(12.34, message.distanceMeters);
    assertEquals(5.67, message.changeRateMps);
    assertEquals(DistanceMessage.Direction.FRONT, message.direction);
    assertEquals(measuredAt, message.timestamp);
  }

  @Test
  void rejectsUnsupportedDirection() {
    SonarReadingDto reading = new SonarReadingDto(
        "VIN-1",
        "VIN-2",
        "LEFT",
        new BigDecimal("12.34"),
        new BigDecimal("5.67"),
        Instant.parse("2026-06-05T12:00:00Z"));

    assertNull(new SonarPublisher().toDistanceMessage(reading));
  }
}
