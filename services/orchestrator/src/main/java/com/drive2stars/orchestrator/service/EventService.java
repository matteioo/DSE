package com.drive2stars.orchestrator.service;

import com.drive2stars.orchestrator.persistence.EventEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;

@ApplicationScoped
public class EventService {

  @Transactional
  public void recordBrake(String vin) {
    EventEntity event = new EventEntity();
    event.vin = vin;
    event.type = EventEntity.EventType.EMERGENCY_BRAKE_SENT;
    event.occurredAt = Instant.now();
    event.persist();
  }
}
