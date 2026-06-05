package com.drive2stars.brakenow.mq;

import com.drive2stars.shared.messaging.BrakeMessage;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

@ApplicationScoped
public class BrakePublisher {

    private static final Logger LOG = Logger.getLogger(BrakePublisher.class);

    private final Emitter<JsonObject> emitter;

    public BrakePublisher(@Channel("brake-status") Emitter<JsonObject> emitter) {
        this.emitter = emitter;
    }

    public void publish(BrakeMessage message) {
        JsonObject payload = JsonObject.mapFrom(message);
        emitter.send(Message.of(payload)
            .addMetadata(OutgoingRabbitMQMetadata.builder()
                .withContentType("application/json")
                .build()));
        LOG.infof("Published BrakeMessage: vin=%s active=%b condition=%d source=%s",
            message.vin, message.active, message.conditionTriggered, message.source);
    }
}
