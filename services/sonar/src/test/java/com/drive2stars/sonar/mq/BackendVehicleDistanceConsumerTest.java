package com.drive2stars.sonar.mq;

import com.drive2stars.shared.messaging.DistanceMessage;
import com.drive2stars.sonar.service.SonarPollingService;
import io.vertx.core.json.JsonObject;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BackendVehicleDistanceConsumerTest {

  @Test
  void backendConsumerCachesValidDistanceMessage() {
    RecordingSonarPollingService sonarPollingService = new RecordingSonarPollingService();
    BackendVehicleDistanceConsumer consumer = consumer("backend", sonarPollingService);

    consumer.process(JsonObject.mapFrom(message()).encode().getBytes(StandardCharsets.UTF_8));

    assertEquals(1, sonarPollingService.processed);
  }

  @Test
  void consumerDropsMalformedAndIncompleteMessages() {
    RecordingSonarPollingService sonarPollingService = new RecordingSonarPollingService();
    BackendVehicleDistanceConsumer consumer = consumer("backend", sonarPollingService);

    consumer.process("not-json".getBytes(StandardCharsets.UTF_8));
    consumer.process(JsonObject.mapFrom(new DistanceMessage()).encode()
        .getBytes(StandardCharsets.UTF_8));

    assertEquals(0, sonarPollingService.processed);
  }

  @Test
  void vehicleModeIgnoresDistanceMessages() {
    RecordingSonarPollingService sonarPollingService = new RecordingSonarPollingService();
    BackendVehicleDistanceConsumer consumer = consumer("vehicle", sonarPollingService);

    consumer.process(JsonObject.mapFrom(message()).encode().getBytes(StandardCharsets.UTF_8));

    assertEquals(0, sonarPollingService.processed);
  }

  private static BackendVehicleDistanceConsumer consumer(String mode,
      RecordingSonarPollingService sonarPollingService) {
    BackendVehicleDistanceConsumer consumer = new BackendVehicleDistanceConsumer();
    consumer.mode = mode;
    consumer.sonarPollingService = sonarPollingService;
    return consumer;
  }

  private static DistanceMessage message() {
    return new DistanceMessage("VIN-2", 50.30, 1.25,
        DistanceMessage.Direction.FRONT, Instant.parse("2026-06-05T10:00:00Z"));
  }

  private static class RecordingSonarPollingService extends SonarPollingService {
    int processed;

    @Override
    public void processBackendDistanceMessage(DistanceMessage message) {
      processed++;
    }
  }
}
