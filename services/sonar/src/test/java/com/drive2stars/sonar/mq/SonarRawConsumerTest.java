package com.drive2stars.sonar.mq;

import com.drive2stars.sonar.endpoint.SonarSensorReadingDto;
import com.drive2stars.sonar.service.SonarPollingService;
import io.vertx.core.json.JsonObject;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SonarRawConsumerTest {

  @Test
  void backendConsumerProcessesValidRawMessage() {
    RecordingSonarPollingService sonarPollingService = new RecordingSonarPollingService();
    SonarRawConsumer consumer = consumer("backend", sonarPollingService);

    consumer.process(JsonObject.mapFrom(reading()).encode().getBytes(StandardCharsets.UTF_8));

    assertEquals(1, sonarPollingService.processed);
  }

  @Test
  void consumerDropsMalformedAndIncompleteMessages() {
    RecordingSonarPollingService sonarPollingService = new RecordingSonarPollingService();
    SonarRawConsumer consumer = consumer("backend", sonarPollingService);

    consumer.process("not-json".getBytes(StandardCharsets.UTF_8));
    consumer.process(JsonObject.mapFrom(new SonarSensorReadingDto()).encode()
        .getBytes(StandardCharsets.UTF_8));

    assertEquals(0, sonarPollingService.processed);
  }

  @Test
  void vehicleModeIgnoresRawMessages() {
    RecordingSonarPollingService sonarPollingService = new RecordingSonarPollingService();
    SonarRawConsumer consumer = consumer("vehicle", sonarPollingService);

    consumer.process(JsonObject.mapFrom(reading()).encode().getBytes(StandardCharsets.UTF_8));

    assertEquals(0, sonarPollingService.processed);
  }

  private static SonarRawConsumer consumer(String mode,
      RecordingSonarPollingService sonarPollingService) {
    SonarRawConsumer consumer = new SonarRawConsumer();
    consumer.mode = mode;
    consumer.sonarPollingService = sonarPollingService;
    return consumer;
  }

  private static SonarSensorReadingDto reading() {
    return new SonarSensorReadingDto("VIN-2", "FRONT", new BigDecimal("50.30"),
        new BigDecimal("49.90"), new BigDecimal("50.05"), Instant.parse("2026-06-05T10:00:00Z"));
  }

  private static class RecordingSonarPollingService extends SonarPollingService {
    int processed;

    @Override
    public void processBackendRawReading(SonarSensorReadingDto sensorReading) {
      processed++;
    }
  }
}
