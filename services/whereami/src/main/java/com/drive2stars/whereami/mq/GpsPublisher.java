package com.drive2stars.whereami.mq;

import com.drive2stars.shared.messaging.GpsMessage;
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

  public void publish(GpsMessage message) {
    JsonObject payload = JsonObject.mapFrom(message);
    emitter.send(Message.of(payload)
        .addMetadata(OutgoingRabbitMQMetadata.builder()
            .withContentType("application/json")
            .build()));
    LOG.infof("Published GPS for VIN %s: %s, %s", message.vin, message.latitude, message.longitude);
  }
}