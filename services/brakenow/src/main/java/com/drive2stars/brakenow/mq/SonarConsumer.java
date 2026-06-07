package com.drive2stars.brakenow.mq;

import com.drive2stars.brakenow.service.BrakeService;
import com.drive2stars.shared.messaging.DistanceMessage;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;


@ApplicationScoped
public class SonarConsumer {

    private static final Logger LOG = Logger.getLogger(SonarConsumer.class);

    @Inject
    BrakeService brakeService;

    @ConfigProperty(name = "brakenow.vin")
    String ownVin;

    @Incoming("vehicle-sonar")
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

        if (msg.vin == null || msg.direction == null) {
            LOG.error("Dropping incomplete DistanceMessage");
            return;
        }

        if (!ownVin.equals(msg.vin)) return;

        LOG.infof("SONAR reading: vin=%s dist=%.1fm closing=%.2fm/s",
                msg.vin, msg.distanceMeters, msg.changeRateMps);

        brakeService.processDistance(msg.vin, msg.distanceMeters, msg.changeRateMps, msg.direction);
    }
}
