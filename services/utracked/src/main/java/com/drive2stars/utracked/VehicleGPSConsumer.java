package com.drive2stars.utracked;

import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class VehicleGPSConsumer {

  private static final Logger LOG = Logger.getLogger(VehicleGPSConsumer.class);

  @Incoming("vehicle-position")
  @Blocking
  @Transactional
  public void process(JsonObject payload) {
    VehicleGPSDto msg = payload.mapTo(VehicleGPSDto.class);
    LOG.infof("Received GPS for VIN %s: %s, %s", msg.vin, msg.latitude, msg.longitude);

    VehiclePositionEntity entity = new VehiclePositionEntity();
    entity.vin = msg.vin;
    entity.latitude = msg.latitude;
    entity.longitude = msg.longitude;
    entity.persist();
  }
}