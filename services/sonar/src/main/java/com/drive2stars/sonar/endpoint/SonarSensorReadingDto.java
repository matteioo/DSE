package com.drive2stars.sonar.endpoint;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.math.BigDecimal;
import java.time.Instant;

@RegisterForReflection
public class SonarSensorReadingDto {

  public String vin;
  public String targetVin;
  public String direction;
  public BigDecimal radarDistanceMeters;
  public BigDecimal lidarDistanceMeters;
  public BigDecimal ultrasonicDistanceMeters;
  public Instant measuredAt;

  public SonarSensorReadingDto() {}

  public SonarSensorReadingDto(String vin, String targetVin, String direction,
      BigDecimal radarDistanceMeters, BigDecimal lidarDistanceMeters,
      BigDecimal ultrasonicDistanceMeters, Instant measuredAt) {
    this.vin = vin;
    this.targetVin = targetVin;
    this.direction = direction;
    this.radarDistanceMeters = radarDistanceMeters;
    this.lidarDistanceMeters = lidarDistanceMeters;
    this.ultrasonicDistanceMeters = ultrasonicDistanceMeters;
    this.measuredAt = measuredAt;
  }
}
