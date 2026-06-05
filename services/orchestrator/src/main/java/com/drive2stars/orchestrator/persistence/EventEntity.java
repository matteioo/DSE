package com.drive2stars.orchestrator.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

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

  public static EventEntity findLatest() {
    return find("from EventEntity order by id desc").firstResult();
  }

  public static EventEntity findLatestByVin(String vin) {
    return find("vin = ?1 order by id desc", vin).firstResult();
  }

  public enum BrakeEventType {
    EMERGENCY_BRAKE_SENT,       // ORCHESTRATOR sent brake command to BRAKENOW
    PRE_EMERGENCY_REPORTED,     // BRAKENOW reported entering pre-emergency state
    LOCAL_BRAKE_REPORTED        // BRAKENOW reported triggering brake locally
  }
}
