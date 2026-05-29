package com.drive2stars.utracked;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.math.BigDecimal;

@RegisterForReflection
public class VehicleGPSDto {

  public String vin;
  public BigDecimal latitude;
  public BigDecimal longitude;

  @Override
  public String toString() {
    return "VehicleGPSMessage{" + "vin='" + vin + '\'' +
        ", latitude=" + latitude +
        ", longitude=" + longitude +
        '}';
  }
}
