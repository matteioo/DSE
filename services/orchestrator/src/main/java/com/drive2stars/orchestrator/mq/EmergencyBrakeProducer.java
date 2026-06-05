package com.drive2stars.orchestrator.mq;

import com.drive2stars.shared.messaging.BrakeMessage;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;

@ApplicationScoped
public class EmergencyBrakeProducer {

    private final Emitter<BrakeMessage> brakeEmitter;

    public EmergencyBrakeProducer(@Channel("brake-out") Emitter<BrakeMessage> brakeEmitter) {
        this.brakeEmitter = brakeEmitter;
    }

    public void sendBrakeMessage(BrakeMessage message) {
        OutgoingRabbitMQMetadata metadata = new OutgoingRabbitMQMetadata.Builder()
                .withRoutingKey("brake." + message.vin)
                .build();
        brakeEmitter.send(Message.of(message).addMetadata(metadata));
    }
}
