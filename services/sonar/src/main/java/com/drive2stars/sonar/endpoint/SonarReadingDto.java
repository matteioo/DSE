package com.drive2stars.sonar.endpoint;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.math.BigDecimal;
import java.time.Instant;

@RegisterForReflection
public class SonarReadingDto {

  public String vin;
  public String direction;
  public BigDecimal distanceMeters;
  public BigDecimal distanceChangeMetersPerSecond;
  public Instant measuredAt;

  public SonarReadingDto() {}

  public SonarReadingDto(String vin, String direction,
      BigDecimal distanceMeters, BigDecimal distanceChangeMetersPerSecond, Instant measuredAt) {
    this.vin = vin;
    this.direction = direction;
    this.distanceMeters = distanceMeters;
    this.distanceChangeMetersPerSecond = distanceChangeMetersPerSecond;
    this.measuredAt = measuredAt;
  }
}
