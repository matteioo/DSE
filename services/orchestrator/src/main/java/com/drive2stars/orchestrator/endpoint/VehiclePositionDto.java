package com.drive2stars.orchestrator.endpoint;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.math.BigDecimal;

@RegisterForReflection
public class VehiclePositionDto {

  public String vin;
  public BigDecimal latitude;
  public BigDecimal longitude;

  @Override
  public String toString() {
    return "VehiclePositionMessage{" + "vin='" + vin + '\'' +
        ", latitude=" + latitude +
        ", longitude=" + longitude +
        '}';
  }
}
