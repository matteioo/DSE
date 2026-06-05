package com.drive2stars.simulator.service;

import com.drive2stars.simulator.endpoint.SonarSensorReadingDto;
import com.drive2stars.simulator.mq.RawSonarPublisher;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class SonarRawPublishingService {

  @Inject
  VehicleSimulationService vehicleSimulationService;

  @Inject
  RawSonarPublisher rawSonarPublisher;

  @Scheduled(every = "{simulator.sonar.publish.interval}")
  void publishCurrentRawReadings() {
    publishRawReadings(vehicleSimulationService.getOwnSonarReadings(), rawSonarPublisher);
  }

  public int publishOnce() {
    return publishRawReadings(vehicleSimulationService.getOwnSonarReadings(), rawSonarPublisher);
  }

  public static int publishRawReadings(List<SonarSensorReadingDto> readings,
      RawSonarPublisher rawSonarPublisher) {
    if (readings == null) {
      return 0;
    }
    for (SonarSensorReadingDto reading : readings) {
      rawSonarPublisher.publish(reading);
    }
    return readings.size();
  }
}
