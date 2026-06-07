package com.drive2stars.spider.storefront.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@RegisterForReflection
@Schema(description = "Aggregated real-time state of a single tracked vehicle")
public class VehicleStateDto {

    @Schema(description = "Vehicle Identification Number", example = "D2S-DEMO-VIN-001")
    public String vin;

    @Schema(description = "LEAD = front vehicle (no vehicle ahead), FOLLOWER = vehicle behind", example = "FOLLOWER")
    public String role;

    @Schema(description = "Current GPS latitude in WGS84 degrees", example = "48.208176")
    public double latitude;

    @Schema(description = "Current GPS longitude in WGS84 degrees", example = "16.373819")
    public double longitude;

    @Schema(description = "Estimated speed derived from consecutive GPS updates (km/h)", example = "72.4")
    public double speedKmh;

    @Schema(description = "Distance to the vehicle ahead in metres as reported by SONAR. Null if this vehicle is at the front.", example = "42.5")
    public Double distanceToFrontM;

    @Schema(description = "Distance to the vehicle behind in metres as reported by SONAR. Null if this vehicle is at the rear.", example = "38.1")
    public Double distanceToRearM;

    @Schema(description = "Rate of change of the front distance in m/s. Negative means the gap is closing.", example = "-1.2")
    public double distanceChangeMps;

    @Schema(description = "True while an emergency brake signal is active or latched (remains true until vehicle is safe)", example = "false")
    public boolean emergencyBrakeActive;

    @Schema(description = "True when the vehicle has entered the pre-emergency brake state (distance to front < 45 m)", example = "true")
    public boolean preEmergencyBrake;

    @Schema(description = "Timestamp of the last data aggregation for this vehicle (UTC)")
    public Instant updatedAt;
}
