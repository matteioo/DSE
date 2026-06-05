package com.drive2stars.simulator.mq;

import com.drive2stars.shared.messaging.SimulatorVehicleStateMessage;
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
public class SimulatorStateConsumer {

  private static final Logger LOG = Logger.getLogger(SimulatorStateConsumer.class);

  @Inject
  VehicleSimulationService vehicleSimulationService;

  @Incoming("simulator-state-in")
  @Blocking
  public void process(byte[] raw) {
    SimulatorVehicleStateMessage message;
    try {
      message = new JsonObject(new String(raw, StandardCharsets.UTF_8))
          .mapTo(SimulatorVehicleStateMessage.class);
    } catch (DecodeException e) {
      LOG.errorf("Dropping malformed simulator state message: %s", e.getMessage());
      return;
    }

    if (message == null || message.vin == null || message.timestamp == null) {
      LOG.errorf("Dropping incomplete simulator state message: %s", message);
      return;
    }
    vehicleSimulationService.updatePeerState(message);
  }
}
