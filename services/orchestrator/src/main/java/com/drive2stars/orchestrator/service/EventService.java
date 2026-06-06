package com.drive2stars.orchestrator.service;

import com.drive2stars.orchestrator.persistence.EventEntity;
import com.drive2stars.shared.messaging.BrakeMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class EventService {

    @Transactional
    public long clearEvents() {
        return EventEntity.clearAll();
    }

    /**
     * Triggered by ORCHESTRATOR
     */
    @Transactional
    public void recordBrake(String vin, int conditionTriggered) {
        persist(vin, Instant.now(), EventEntity.BrakeEventType.EMERGENCY_BRAKE_SENT,
                conditionTriggered, EventEntity.TriggerSource.ORCHESTRATOR);
    }

    /**
     * Triggered by BRAKENOW
     */
    @Transactional
    public void recordBrake(BrakeMessage msg) {
        EventEntity.BrakeEventType type = msg.active
                ? EventEntity.BrakeEventType.LOCAL_BRAKE_REPORTED
                : EventEntity.BrakeEventType.PRE_EMERGENCY_REPORTED;
        persist(msg.vin, msg.timestamp, type, msg.conditionTriggered, EventEntity.TriggerSource.BRAKENOW);
    }

    public List<EventEntity> getRecentEvents(int limit) {
        return EventEntity.findRecent(limit);
    }

    private void persist(String vin, Instant timestamp, EventEntity.BrakeEventType eventType,
                         int triggerCondition, EventEntity.TriggerSource source) {
        EventEntity event = new EventEntity();
        event.vin = vin;
        event.timestamp = timestamp;
        event.eventType = eventType;
        event.triggerCondition = triggerCondition;
        event.triggeredBy = source;
        event.persist();
    }
}
