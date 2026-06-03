package com.drive2stars.simulator.endpoint;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.math.BigDecimal;

@RegisterForReflection
public class VehicleGpsDto {

  public String vin;
  public BigDecimal latitude;
  public BigDecimal longitude;

  public VehicleGpsDto() {}

  public VehicleGpsDto(String vin, BigDecimal latitude, BigDecimal longitude) {
    this.vin = vin;
    this.latitude = latitude;
    this.longitude = longitude;
  }
}
