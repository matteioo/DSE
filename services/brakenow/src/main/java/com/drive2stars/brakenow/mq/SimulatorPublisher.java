package com.drive2stars.brakenow.mq;

import com.drive2stars.shared.messaging.SimulatorBrakeMessage;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SimulatorPublisher {
  private static final Logger LOG = Logger.getLogger(SimulatorPublisher.class);

  private final Emitter<JsonObject> emitter;

  public SimulatorPublisher(@Channel("simulator-out") Emitter<JsonObject> emitter) {
    this.emitter = emitter;
  }

  public void publish(SimulatorBrakeMessage message) {
    JsonObject payload = JsonObject.mapFrom(message);
    emitter.send(Message.of(payload)
            .addMetadata(OutgoingRabbitMQMetadata.builder()
                    .withContentType("application/json")
                    .withRoutingKey("brake." + message.vin)
                    .build()));
    LOG.infof("Published BrakeMessage to SIMULATOR: vin=%s active=%b preBreak=%b",
            message.vin, message.brakeActive, message.preBreak);
  }

}
