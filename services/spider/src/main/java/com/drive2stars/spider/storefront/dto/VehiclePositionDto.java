package com.drive2stars.spider.storefront.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class VehiclePositionDto {

    public String vin;
    public double latitude;
    public double longitude;
}
