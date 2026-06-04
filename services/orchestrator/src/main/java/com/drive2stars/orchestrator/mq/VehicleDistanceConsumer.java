package com.drive2stars.orchestrator.mq;

import com.drive2stars.orchestrator.service.OrchestratorService;
import com.drive2stars.shared.messaging.DistanceMessage;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class VehicleDistanceConsumer {

  private static final Logger LOG = Logger.getLogger(VehicleDistanceConsumer.class);

  private final OrchestratorService orchestratorService;

  public VehicleDistanceConsumer(OrchestratorService orchestratorService) {
    this.orchestratorService = orchestratorService;
  }

  @Incoming("vehicle-distance")
  @Blocking
  @Transactional
  public void process(byte[] raw) {
    DistanceMessage msg;
    try {
      msg = new JsonObject(new String(raw, StandardCharsets.UTF_8)).mapTo(DistanceMessage.class);
    } catch (DecodeException e) {
      LOG.errorf("Dropping malformed distance message: %s", e.getMessage());
      return;
    }
    if (msg.vin == null || msg.direction == null || msg.timestamp == null) {
      LOG.errorf("Dropping incomplete distance message: %s", msg);
      return;
    }
    LOG.infof("Received vehicle distance message for VIN %s (%s): %sm, %sm/s", msg.vin, msg.timestamp, msg.distanceMeters, msg.changeRateMps);

    orchestratorService.process(msg);
  }
}
