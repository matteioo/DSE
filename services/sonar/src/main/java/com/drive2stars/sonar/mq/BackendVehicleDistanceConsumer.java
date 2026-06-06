package com.drive2stars.sonar.mq;

import com.drive2stars.shared.messaging.DistanceMessage;
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
public class BackendVehicleDistanceConsumer {

  private static final Logger LOG = Logger.getLogger(BackendVehicleDistanceConsumer.class);

  @Inject
  SonarPollingService sonarPollingService;

  @ConfigProperty(name = "sonar.mode")
  String mode;

  @Incoming("backend-vehicle-distance")
  @Blocking
  public void process(byte[] raw) {
    if (!"backend".equalsIgnoreCase(mode)) {
      return;
    }

    DistanceMessage message;
    try {
      message = new JsonObject(new String(raw, StandardCharsets.UTF_8))
          .mapTo(DistanceMessage.class);
    } catch (DecodeException e) {
      LOG.errorf("Dropping malformed vehicle distance message: %s", e.getMessage());
      return;
    }

    if (!isCompleteDistanceMessage(message)) {
      LOG.errorf("Dropping incomplete vehicle distance message: %s", message);
      return;
    }

    sonarPollingService.processBackendDistanceMessage(message);
  }

  static boolean isCompleteDistanceMessage(DistanceMessage message) {
    return message != null
        && message.vin != null
        && message.direction != null
        && message.timestamp != null
        && Double.isFinite(message.distanceMeters)
        && message.distanceMeters >= 0
        && Double.isFinite(message.changeRateMps);
  }
}
