package com.drive2stars.orchestrator.service;

import com.drive2stars.orchestrator.persistence.EventEntity;
import com.drive2stars.shared.messaging.BrakeMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;

@ApplicationScoped
public class EventService {

  @Transactional
  public void recordBrake(String vin, int conditionTriggered) {
    EventEntity event = new EventEntity();
    event.vin = vin;
    event.timestamp = Instant.now();
    event.eventType = EventEntity.BrakeEventType.EMERGENCY_BRAKE_SENT;
    event.triggerCondition = conditionTriggered;
    event.persist();
  }

  @Transactional
  public void recordBrake(BrakeMessage msg) {
    EventEntity event = new EventEntity();
    event.vin = msg.vin;
    event.timestamp = msg.timestamp;
    event.eventType = msg.active
        ? EventEntity.BrakeEventType.LOCAL_BRAKE_REPORTED
        : EventEntity.BrakeEventType.PRE_EMERGENCY_REPORTED;
    event.triggerCondition = msg.conditionTriggered;
    event.persist();
  }
}
