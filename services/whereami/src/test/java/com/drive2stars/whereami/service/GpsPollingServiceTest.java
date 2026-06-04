package com.drive2stars.whereami.service;

import com.drive2stars.shared.messaging.GpsMessage;
import com.drive2stars.whereami.endpoint.GpsReadingDto;
import com.drive2stars.whereami.endpoint.SimulatorGpsClient;
import com.drive2stars.whereami.mq.GpsPublisher;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GpsPollingServiceTest {

  @Test
  void fetchAndPublishUsesProvidedVinAndPublishesGps() {
    RecordingSimulatorGpsClient simulatorGpsClient = new RecordingSimulatorGpsClient();
    RecordingGpsPublisher gpsPublisher = new RecordingGpsPublisher();

    GpsPollingService.fetchAndPublish(simulatorGpsClient, gpsPublisher, "D2S-DEMO-VIN-002");

    assertEquals("D2S-DEMO-VIN-002", simulatorGpsClient.requestedVin);
    assertEquals("D2S-DEMO-VIN-002", gpsPublisher.published.vin);
    assertEquals(new BigDecimal("48.2082000"), gpsPublisher.published.latitude);
    assertEquals(new BigDecimal("16.3721871"), gpsPublisher.published.longitude);
    assertNotNull(gpsPublisher.published.timestamp);
  }

  private static class RecordingSimulatorGpsClient implements SimulatorGpsClient {
    String requestedVin;

    @Override
    public GpsReadingDto getGps(String vin) {
      requestedVin = vin;
      return new GpsReadingDto(vin, new BigDecimal("48.2082000"), new BigDecimal("16.3721871"));
    }
  }

  private static class RecordingGpsPublisher extends GpsPublisher {
    GpsMessage published;

    @Override
    public void publish(GpsMessage message) {
      published = message;
    }
  }
}
