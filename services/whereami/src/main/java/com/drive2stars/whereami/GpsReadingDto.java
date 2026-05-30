package com.drive2stars.whereami;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.math.BigDecimal;

@RegisterForReflection
public class GpsReadingDto {

  public String vin;
  public BigDecimal latitude;
  public BigDecimal longitude;

  public GpsReadingDto() {}

  public GpsReadingDto(String vin, BigDecimal latitude, BigDecimal longitude) {
    this.vin = vin;
    this.latitude = latitude;
    this.longitude = longitude;
  }
}