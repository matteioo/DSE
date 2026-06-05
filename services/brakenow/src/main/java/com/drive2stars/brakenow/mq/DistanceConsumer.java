package com.drive2stars.brakenow.mq;

import com.drive2stars.brakenow.service.BrakeService;
import com.drive2stars.shared.messaging.DistanceMessage;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Consumes DistanceMessage from SONAR (exchange d2s.vehicle.distance).
 * Passes each message to BrakeService for condition evaluation.
 */
@ApplicationScoped
public class DistanceConsumer {

    private static final Logger LOG = Logger.getLogger(DistanceConsumer.class);

    @Inject
    BrakeService brakeService;

    @Incoming("sonar-distance")
    @Blocking
    public void process(byte[] raw) {
        DistanceMessage msg;
        try {
            msg = new JsonObject(new String(raw, StandardCharsets.UTF_8))
                    .mapTo(DistanceMessage.class);
        } catch (DecodeException e) {
            LOG.errorf("Dropping malformed DistanceMessage: %s", e.getMessage());
            return;
        }
        if (msg.vin == null) {
            LOG.error("Dropping DistanceMessage with null VIN");
            return;
        }
        LOG.debugf("Distance received: vin=%s dist=%.1fm rate=%.2fm/s",
                msg.vin, msg.distanceMeters, msg.changeRateMps);
        brakeService.processDistance(msg);
    }
}
