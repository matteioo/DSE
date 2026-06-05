package com.drive2stars.sonar.mq;

import com.drive2stars.sonar.endpoint.SonarReadingDto;
import com.drive2stars.shared.messaging.DistanceMessage;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Locale;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SonarPublisher {

  private static final Logger LOG = Logger.getLogger(SonarPublisher.class);

  @Inject
  @Channel("vehicle-sonar")
  Emitter<JsonObject> emitter;

  public void publish(SonarReadingDto reading) {
    DistanceMessage distanceMessage = toDistanceMessage(reading);
    if (distanceMessage == null) {
      return;
    }

    JsonObject payload = JsonObject.mapFrom(distanceMessage);
    emitter.send(Message.of(payload)
        .addMetadata(OutgoingRabbitMQMetadata.builder()
            .withContentType("application/json")
            .build()));
    LOG.infof("Published SONAR for VIN %s to %s: distance=%s, change=%s",
        reading.vin, reading.direction, reading.distanceMeters,
        reading.distanceChangeMetersPerSecond);
  }

  DistanceMessage toDistanceMessage(SonarReadingDto reading) {
    if (reading == null
        || reading.vin == null
        || reading.direction == null
        || reading.distanceMeters == null
        || reading.distanceChangeMetersPerSecond == null
        || reading.measuredAt == null) {
      LOG.errorf("Dropping incomplete SONAR reading: %s", reading);
      return null;
    }

    DistanceMessage.Direction direction;
    try {
      direction = DistanceMessage.Direction.valueOf(reading.direction.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      LOG.errorf("Dropping SONAR reading with unsupported direction %s for VIN %s",
          reading.direction, reading.vin);
      return null;
    }

    return new DistanceMessage(
        reading.vin,
        reading.distanceMeters.doubleValue(),
        reading.distanceChangeMetersPerSecond.doubleValue(),
        direction,
        reading.measuredAt);
  }
}
