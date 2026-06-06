package com.drive2stars.simulator.mq;

import com.drive2stars.shared.messaging.SimulatorBrakeMessage;
import com.drive2stars.simulator.service.VehicleSimulationService;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class BrakeNowConsumer {

  private static final Logger LOG = Logger.getLogger(BrakeNowConsumer.class);

  @Inject
  VehicleSimulationService vehicleSimulationService;

  @Incoming("brakenow-brake-in")
  @Blocking
  public void process(byte[] raw) {
    SimulatorBrakeMessage message;
    try {
      message = new JsonObject(new String(raw, StandardCharsets.UTF_8))
          .mapTo(SimulatorBrakeMessage.class);
    } catch (DecodeException e) {
      LOG.errorf("Dropping malformed brakenow brake message: %s", e.getMessage());
      return;
    }

    if (message == null || message.vin == null || message.timestamp == null) {
      LOG.errorf("Dropping incomplete brakenow brake message: %s", message);
      return;
    }

    LOG.infof("Received brakenow brake message for VIN %s: brakeActive=%b preBreak=%b",
        message.vin, message.brakeActive, message.preBreak);
    vehicleSimulationService.applyBrakeMessage(message);
  }
}
