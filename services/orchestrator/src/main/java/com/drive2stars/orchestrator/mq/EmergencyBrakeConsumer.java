package com.drive2stars.orchestrator.mq;

import com.drive2stars.orchestrator.service.EventService;
import com.drive2stars.shared.messaging.BrakeMessage;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class EmergencyBrakeConsumer {

    private static final Logger LOG = Logger.getLogger(EmergencyBrakeConsumer.class);

    private final EventService eventService;

    public EmergencyBrakeConsumer(EventService eventService) {
        this.eventService = eventService;
    }

    @Incoming("brake-in")
    @Blocking
    public void process(byte[] raw) {
        BrakeMessage msg;
        try {
            msg = new JsonObject(new String(raw, StandardCharsets.UTF_8)).mapTo(BrakeMessage.class);
        } catch (DecodeException e) {
            LOG.errorf("Dropping malformed emergency brake message: %s", e.getMessage());
            return;
        }
        if (msg == null || msg.vin == null || msg.source == null || msg.timestamp == null) {
            LOG.errorf("Dropping incomplete emergency brake message: %s", msg);
            return;
        }
        LOG.infof(
                "Received emergency brake message from %s for VIN %s (%s): active=%s, conditionTriggered=%s",
                msg.source, msg.vin, msg.timestamp, msg.active, msg.conditionTriggered);

        // Skip own messages (already persisted when triggering)
        if (msg.source == BrakeMessage.Source.ORCHESTRATOR) {
            LOG.debugf("Skipping emergency brake message from own producer for VIN %s", msg.vin);
            return;
        }

        eventService.recordBrake(msg);
    }
}
