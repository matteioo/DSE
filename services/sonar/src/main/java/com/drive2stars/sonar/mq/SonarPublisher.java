package com.drive2stars.sonar.mq;

import com.drive2stars.sonar.endpoint.SonarReadingDto;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
    JsonObject payload = JsonObject.mapFrom(reading);
    emitter.send(Message.of(payload)
        .addMetadata(OutgoingRabbitMQMetadata.builder()
            .withContentType("application/json")
            .build()));
    LOG.infof("Published SONAR for VIN %s to %s/%s: distance=%s, change=%s",
        reading.vin, reading.direction, reading.targetVin, reading.distanceMeters,
        reading.distanceChangeMetersPerSecond);
  }
}
