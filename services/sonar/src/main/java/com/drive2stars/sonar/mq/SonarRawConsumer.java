package com.drive2stars.sonar.mq;

import com.drive2stars.sonar.endpoint.SonarSensorReadingDto;
import com.drive2stars.sonar.service.SonarPollingService;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SonarRawConsumer {

  private static final Logger LOG = Logger.getLogger(SonarRawConsumer.class);

  @Inject
  SonarPollingService sonarPollingService;

  @ConfigProperty(name = "sonar.mode")
  String mode;

  @Incoming("raw-sonar")
  @Blocking
  public void process(byte[] raw) {
    if (!"backend".equalsIgnoreCase(mode)) {
      return;
    }

    SonarSensorReadingDto reading;
    try {
      reading = new JsonObject(new String(raw, StandardCharsets.UTF_8))
          .mapTo(SonarSensorReadingDto.class);
    } catch (DecodeException e) {
      LOG.errorf("Dropping malformed raw SONAR message: %s", e.getMessage());
      return;
    }

    if (!SonarPollingService.isCompleteRawReading(reading)) {
      LOG.errorf("Dropping incomplete raw SONAR message for VIN %s", reading == null ? null : reading.vin);
      return;
    }

    sonarPollingService.processBackendRawReading(reading);
  }
}
