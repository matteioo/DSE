package com.drive2stars.simulator.mq;

import com.drive2stars.shared.messaging.SimulatorVehicleStateMessage;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SimulatorStatePublisher {

  private static final Logger LOG = Logger.getLogger(SimulatorStatePublisher.class);

  @Inject
  @Channel("simulator-state-out")
  Emitter<JsonObject> emitter;

  public void publish(SimulatorVehicleStateMessage state) {
    JsonObject payload = JsonObject.mapFrom(state);
    emitter.send(Message.of(payload)
        .addMetadata(OutgoingRabbitMQMetadata.builder()
            .withContentType("application/json")
            .build()));
    LOG.debugf("Published simulator state for VIN %s at position %s", state.vin,
        state.positionMeters);
  }
}
