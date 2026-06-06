package com.drive2stars.brakenow.mq;

import com.drive2stars.brakenow.service.BrakeService;
import com.drive2stars.shared.messaging.BrakeMessage;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class BrakeConsumer {

  private static final Logger LOG = Logger.getLogger(BrakeConsumer.class);

  @Inject
  BrakeService brakeService;

  @ConfigProperty(name = "brakenow.vin")
  String ownVin;

  @Incoming("brake-in")
  @Blocking
  public void process(byte[] raw) {
    BrakeMessage msg;
    try {
      msg = new JsonObject(new String(raw, StandardCharsets.UTF_8))
              .mapTo(BrakeMessage.class);
    } catch (DecodeException e) {
      LOG.errorf("Dropping malformed BrakeMessage: %s", e.getMessage());
      return;
    }

    if (msg == null || msg.vin == null || msg.source == null || msg.timestamp == null) {
      LOG.errorf("Dropping incomplete emergency brake message: %s", msg);
      return;
    }

    if (!ownVin.equals(msg.vin)) return;

    // Only relay ORCHESTRATOR commands skip own published messages
    if (msg.source == BrakeMessage.Source.BRAKENOW) return;

    LOG.infof(
            "Received emergency brake message from %s for VIN %s (%s): active=%b, conditionTriggered=%s",
            msg.source, msg.vin, msg.timestamp, msg.active, msg.conditionTriggered);

    brakeService.processBrake(msg.vin, msg.active, msg.timestamp);
  }
}
