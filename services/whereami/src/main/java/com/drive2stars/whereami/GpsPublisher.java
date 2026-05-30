package com.drive2stars.whereami;

import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

@ApplicationScoped
public class GpsPublisher {

  private static final Logger LOG = Logger.getLogger(GpsPublisher.class);

  @Inject
  @Channel("vehicle-gps")
  Emitter<JsonObject> emitter;

  public void publish(GpsReadingDto reading) {
    JsonObject payload = JsonObject.mapFrom(reading);
    emitter.send(Message.of(payload)
        .addMetadata(OutgoingRabbitMQMetadata.builder()
            .withContentType("application/json")
            .build()));
    LOG.infof("Published GPS for VIN %s: %s, %s", reading.vin, reading.latitude, reading.longitude);
  }
}