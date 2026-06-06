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

    public static long clearAll() {
        return deleteAll();
    }

    public static List<EventEntity> findRecent(int limit) {
        if (limit <= 0) {
            return find("from EventEntity order by id desc").list();
        }
        return find("from EventEntity order by id desc").page(0, limit).list();
    }

    public enum BrakeEventType {
        EMERGENCY_BRAKE_SENT,       // ORCHESTRATOR sent brake command to BRAKENOW
        PRE_EMERGENCY_REPORTED,     // BRAKENOW reported entering pre-emergency state
        LOCAL_BRAKE_REPORTED        // BRAKENOW reported triggering brake locally
    }
}
