package com.drive2stars.spider.storefront.dto;

public class BrakeStateDto {
    public final String vin;
    public final boolean emergencyBrakeActive;
    public final boolean preEmergencyBrakeActive;

    public BrakeStateDto(String vin, boolean emergencyBrakeActive, boolean preEmergencyBrakeActive) {
        this.vin = vin;
        this.emergencyBrakeActive = emergencyBrakeActive;
        this.preEmergencyBrakeActive = preEmergencyBrakeActive;
    }
}
