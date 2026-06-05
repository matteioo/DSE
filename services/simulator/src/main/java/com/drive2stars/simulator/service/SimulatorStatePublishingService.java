package com.drive2stars.simulator.service;

import com.drive2stars.simulator.mq.SimulatorStatePublisher;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SimulatorStatePublishingService {

  @Inject
  VehicleSimulationService vehicleSimulationService;

  @Inject
  SimulatorStatePublisher simulatorStatePublisher;

  @Scheduled(every = "{simulator.state.publish.interval}")
  void publishCurrentState() {
    simulatorStatePublisher.publish(vehicleSimulationService.currentStateMessage());
  }

  public void publishOnce() {
    publishCurrentState();
  }
}
