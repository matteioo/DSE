package com.drive2stars.orchestrator.service;

import com.drive2stars.orchestrator.mq.EmergencyBrakeProducer;
import com.drive2stars.shared.messaging.BrakeMessage;
import com.drive2stars.shared.messaging.DistanceMessage;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class BrakeConditionService {

  private final EmergencyBrakeProducer brakeProducer;
  private final EventService eventService;

  public BrakeConditionService(EmergencyBrakeProducer brakeProducer, EventService eventService) {
    this.brakeProducer = brakeProducer;
    this.eventService = eventService;
  }

  record Condition(double distThreshold, double rateThreshold, int id) {}

  private static final List<Condition> CONDITIONS = List.of(
      new Condition(30, 4, 1),
      new Condition(15, 2, 2),
      new Condition(5, 0, 3)
  );

  public CompletableFuture<Void> checkSonarConditions(DistanceMessage msg) {
    return CompletableFuture.allOf(
        CONDITIONS.stream()
            .map(c -> CompletableFuture.runAsync(() -> {
              if (msg.distanceMeters < c.distThreshold && msg.changeRateMps > c.rateThreshold) {
                triggerBrake(msg, c.id);
              }
            }))
            .toArray(CompletableFuture[]::new)
    );
  }

  void triggerBrake(DistanceMessage msg, int condition) {
    brakeProducer.sendBrakeMessage(new BrakeMessage(msg.vin, true, condition, BrakeMessage.Source.ORCHESTRATOR, Instant.now()));
    eventService.recordBrake(msg.vin, condition);
  }
}
