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
        persist(vin, Instant.now(), EventEntity.BrakeEventType.EMERGENCY_BRAKE_SENT,
                conditionTriggered);
    }

    @Transactional
    public void recordBrake(BrakeMessage msg) {
        EventEntity.BrakeEventType type = msg.active
                ? EventEntity.BrakeEventType.LOCAL_BRAKE_REPORTED
                : EventEntity.BrakeEventType.PRE_EMERGENCY_REPORTED;
        persist(msg.vin, msg.timestamp, type, msg.conditionTriggered);
    }

    private void persist(String vin, Instant timestamp, EventEntity.BrakeEventType eventType,
                         int triggerCondition) {
        EventEntity event = new EventEntity();
        event.vin = vin;
        event.timestamp = timestamp;
        event.eventType = eventType;
        event.triggerCondition = triggerCondition;
        event.persist();
    }
}
