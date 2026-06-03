package com.drive2stars.simulator.endpoint;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.math.BigDecimal;

@RegisterForReflection
public class VehicleStateDto {

  public String vin;
  public BigDecimal latitude;
  public BigDecimal longitude;
  public BigDecimal speedMetersPerSecond;

  public VehicleStateDto() {}

  public VehicleStateDto(String vin, BigDecimal latitude, BigDecimal longitude,
      BigDecimal speedMetersPerSecond) {
    this.vin = vin;
    this.latitude = latitude;
    this.longitude = longitude;
    this.speedMetersPerSecond = speedMetersPerSecond;
  }
}
