package com.drive2stars.brakenow.mq;

import com.drive2stars.brakenow.service.BrakeService;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Consumes SonarReadingDto messages published by SONAR on exchange vehicle.sonar.
 *
 * Filters to own VIN + FRONT direction, then adapts the message into the format
 * BrakeService expects:
 *   distanceMeters          — unchanged
 *   distanceChangeMps       — SONAR's distanceChangeMetersPerSecond is positive when closing in;
 *                             BrakeService expects the same sign convention (positive = closing).
 */
@ApplicationScoped
public class SonarConsumer {

    private static final Logger LOG = Logger.getLogger(SonarConsumer.class);

    @Inject
    BrakeService brakeService;

    @ConfigProperty(name = "brakenow.vin")
    String ownVin;

    @Incoming("sonar-readings")
    @Blocking
    public void process(byte[] raw) {
        JsonObject json;
        try {
            json = new JsonObject(new String(raw, StandardCharsets.UTF_8));
        } catch (DecodeException e) {
            LOG.errorf("Dropping malformed SONAR message: %s", e.getMessage());
            return;
        }

        String vin = json.getString("vin");
        String direction = json.getString("direction");

        // Only handle readings for own VIN
        if (!ownVin.equals(vin)) return;

        double distanceM  = toDouble(json, "distanceMeters");
        //positive = closing in
        double closingMps = toDouble(json, "changeRateMps");

        LOG.debugf("SONAR reading: vin=%s dir=%s dist=%.1fm closing=%.2fm/s",
                vin, direction, distanceM, closingMps);

        brakeService.processDistance(vin, distanceM, closingMps);
    }

    private double toDouble(JsonObject json, String field) {
        Object val = json.getValue(field);
        if (val instanceof Number n) return n.doubleValue();
        if (val instanceof String s) { try { return Double.parseDouble(s); } catch (NumberFormatException _) {} }
        return 0.0;
    }
}
