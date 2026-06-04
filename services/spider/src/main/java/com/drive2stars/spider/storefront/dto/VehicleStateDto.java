package com.drive2stars.spider.storefront.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;
@RegisterForReflection
public class VehicleStateDto {

    public String vin;
    public String role;
    public double latitude;
    public double longitude;
    public double speedKmh;
    public Double distanceToFrontM;
    public Double distanceToRearM;
    public double distanceChangeMps;
    public boolean emergencyBrakeActive;
    public Instant updatedAt;
}
