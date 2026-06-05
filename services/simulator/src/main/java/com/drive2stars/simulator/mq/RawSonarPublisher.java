package com.drive2stars.simulator.mq;

import com.drive2stars.simulator.endpoint.SonarSensorReadingDto;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

@ApplicationScoped
public class RawSonarPublisher {

  private static final Logger LOG = Logger.getLogger(RawSonarPublisher.class);

  @Inject
  @Channel("raw-sonar")
  Emitter<JsonObject> emitter;

  public void publish(SonarSensorReadingDto reading) {
    JsonObject payload = JsonObject.mapFrom(reading);
    emitter.send(Message.of(payload)
        .addMetadata(OutgoingRabbitMQMetadata.builder()
            .withContentType("application/json")
            .build()));
    LOG.infof("Published raw SONAR for VIN %s to %s", reading.vin, reading.direction);
  }
}
