package com.drive2stars.orchestrator.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "events")
public class EventEntity extends PanacheEntity {

    @Column(nullable = false)
    public String vin;

    @Column(nullable = false)
    public Instant timestamp;

    @Column(nullable = false, name = "event_type")
    @Enumerated(EnumType.STRING)
    public BrakeEventType eventType;

    // null if not triggered by ORCHESTRATOR condition 1-4
    @Column(name = "trigger_condition")
    public Integer triggerCondition;

    @Column(nullable = false, name = "trigger_source")
    @Enumerated(EnumType.STRING)
    public TriggerSource triggeredBy;

    public static long clearAll() {
        return deleteAll();
    }

    public static List<EventEntity> findRecent(int limit) {
        var query = find("from EventEntity order by id desc");
        return limit > 0 ? query.page(0, limit).list() : query.list();
    }

    public enum BrakeEventType {
        EMERGENCY_BRAKE_SENT,       // ORCHESTRATOR sent brake command to BRAKENOW
        PRE_EMERGENCY_REPORTED,     // BRAKENOW reported entering pre-emergency state
        LOCAL_BRAKE_REPORTED        // BRAKENOW reported triggering brake locally
    }

    public enum TriggerSource {
        ORCHESTRATOR,
        BRAKENOW
    }
}
