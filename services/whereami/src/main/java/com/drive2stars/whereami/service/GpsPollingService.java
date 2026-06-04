package com.drive2stars.whereami.service;

import com.drive2stars.shared.messaging.GpsMessage;
import com.drive2stars.whereami.endpoint.GpsReadingDto;
import com.drive2stars.whereami.endpoint.SimulatorGpsClient;
import com.drive2stars.whereami.mq.GpsPublisher;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import java.time.Instant;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class GpsPollingService {

  private static final Logger LOG = Logger.getLogger(GpsPollingService.class);

  private final SimulatorGpsClient simulatorGpsClient;
  private final GpsPublisher gpsPublisher;
  private final String vin;

  public GpsPollingService(@RestClient SimulatorGpsClient simulatorGpsClient,
                           GpsPublisher gpsPublisher,
                           @ConfigProperty(name = "whereami.vin") String vin) {
    this.simulatorGpsClient = simulatorGpsClient;
    this.gpsPublisher = gpsPublisher;
    this.vin = vin;
  }

  @Scheduled(every = "{whereami.poll.interval}")
  void publishCurrentPosition() {
    fetchAndPublish(simulatorGpsClient, gpsPublisher, vin);
  }

  public void pollOnce() {
    publishCurrentPosition();
  }

  static void fetchAndPublish(SimulatorGpsClient simulatorGpsClient, GpsPublisher gpsPublisher, String vin) {
    try {
      GpsReadingDto reading = simulatorGpsClient.getGps(vin);
      GpsMessage message = new GpsMessage(reading.vin, reading.latitude, reading.longitude, Instant.now());
      gpsPublisher.publish(message);
    } catch (WebApplicationException e) {
      LOG.errorf("Could not read GPS for VIN %s from simulator: HTTP %d", vin, e.getResponse().getStatus());
    } catch (RuntimeException e) {
      LOG.errorf(e, "Could not read GPS for VIN %s from simulator", vin);
    }
  }
}
