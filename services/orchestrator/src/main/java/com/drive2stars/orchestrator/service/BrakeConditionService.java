package com.drive2stars.orchestrator.service;

import com.drive2stars.orchestrator.mq.EmergencyBrakeProducer;
import com.drive2stars.shared.messaging.BrakeMessage;
import com.drive2stars.shared.messaging.DistanceMessage;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class BrakeConditionService {

  private final EmergencyBrakeProducer brakeProducer;
  private final EventService eventService;

  public BrakeConditionService(EmergencyBrakeProducer brakeProducer, EventService eventService) {
    this.brakeProducer = brakeProducer;
    this.eventService = eventService;
  }

  public CompletableFuture<Void> checkSonarConditions(DistanceMessage msg) {
    CompletableFuture<Void> cond1 = CompletableFuture.runAsync(() -> {
      if (msg.distanceMeters < 30 && msg.changeRateMps > 4) {
        triggerBrake(msg, 1);
      }
    });

    CompletableFuture<Void> cond2 = CompletableFuture.runAsync(() -> {
      if (msg.distanceMeters < 15 && msg.changeRateMps > 2) {
        triggerBrake(msg, 2);
      }
    });

    CompletableFuture<Void> cond3 = CompletableFuture.runAsync(() -> {
      if (msg.distanceMeters < 5 && msg.changeRateMps > 0) {
        triggerBrake(msg, 3);
      }
    });

    return CompletableFuture.allOf(cond1, cond2, cond3);
  }

  void triggerBrake(DistanceMessage msg, int condition) {
    brakeProducer.sendBrakeMessage(new BrakeMessage(msg.vin, true, condition, BrakeMessage.Source.ORCHESTRATOR, Instant.now()));
    eventService.recordBrake(msg.vin, condition);
  }
}
