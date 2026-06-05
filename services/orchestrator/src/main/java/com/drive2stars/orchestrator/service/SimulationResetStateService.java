package com.drive2stars.orchestrator.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@ApplicationScoped
public class SimulationResetStateService {

  private final Clock clock;
  private volatile Instant lastResetAt;

  public SimulationResetStateService() {
    this(Clock.systemUTC());
  }

  SimulationResetStateService(Clock clock) {
    this.clock = clock;
  }

  public void recordReset() {
    lastResetAt = Instant.now(clock);
  }

  public boolean isWithinResetGrace(Duration gracePeriod) {
    Instant resetAt = lastResetAt;
    if (resetAt == null || gracePeriod == null || gracePeriod.isNegative() || gracePeriod.isZero()) {
      return false;
    }
    return Duration.between(resetAt, Instant.now(clock)).compareTo(gracePeriod) < 0;
  }
}
