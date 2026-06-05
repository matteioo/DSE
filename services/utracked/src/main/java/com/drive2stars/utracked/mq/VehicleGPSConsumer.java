package com.drive2stars.utracked.mq;

import com.drive2stars.shared.messaging.GpsMessage;
import com.drive2stars.utracked.persistence.VehiclePositionEntity;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class VehicleGPSConsumer {

  private static final Logger LOG = Logger.getLogger(VehicleGPSConsumer.class);

  @Incoming("vehicle-position")
  @Blocking
  @Transactional
  public void process(byte[] raw) {
    GpsMessage msg;
    try {
      msg = new JsonObject(new String(raw, StandardCharsets.UTF_8)).mapTo(GpsMessage.class);
    } catch (DecodeException e) {
      LOG.errorf("Dropping malformed GPS message: %s", e.getMessage());
      return;
    }
    if (msg.vin == null || msg.latitude == null || msg.longitude == null) {
      LOG.errorf("Dropping incomplete GPS message: %s", msg);
      return;
    }
    LOG.infof("Received GPS for VIN %s: lat=%s, lon=%s", msg.vin, msg.latitude, msg.longitude);

    VehiclePositionEntity entity = new VehiclePositionEntity();
    entity.vin = msg.vin;
    entity.latitude = msg.latitude;
    entity.longitude = msg.longitude;
    entity.persist();
  }
}
