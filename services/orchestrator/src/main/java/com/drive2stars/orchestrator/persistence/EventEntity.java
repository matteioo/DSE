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

  @Enumerated(EnumType.STRING)
  public EventType type;

  @Column(nullable = false)
  public Instant occurredAt;

  public static EventEntity findLatest() {
    return find("from EventEntity order by id desc").firstResult();
  }

  public enum EventType {
    EMERGENCY_BRAKE_SENT
  }
}
