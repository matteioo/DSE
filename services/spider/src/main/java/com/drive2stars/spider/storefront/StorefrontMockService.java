package com.drive2stars.spider.storefront;

import com.drive2stars.spider.storefront.dto.EventLogEntryDto;
import com.drive2stars.spider.storefront.dto.StorefrontDataDto;
import com.drive2stars.spider.storefront.dto.VehicleStateDto;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides mocked simulation data for the Storefront dashboard.
 *
 * Scenario: two vehicles on a straight road. VH-002 slowly closes on VH-001.
 * A 60-second cycle progresses through normal, mergency brake,recovery.
 *
 */
@ApplicationScoped
public class StorefrontMockService {

    private static final String VIN_LEAD     = "VH-001";
    private static final String VIN_FOLLOWER = "VH-002";

    private static final double BASE_LAT      = 48.2082;
    private static final double BASE_LON      = 16.3738;
    private static final double LON_PER_METER = 0.0000090;

    public StorefrontDataDto getStorefrontData() {
        StorefrontDataDto dto = new StorefrontDataDto();
        dto.fetchedAt = Instant.now();
        dto.vehicles  = buildVehicleStates();
        dto.eventLog  = buildEventLog(dto.vehicles);
        return dto;
    }

    public List<VehicleStateDto> getVehicleStates() {
        return buildVehicleStates();
    }

    public VehicleStateDto getVehicleState(String vin) {
        return buildVehicleStates().stream()
            .filter(v -> vin.equals(v.vin))
            .findFirst()
            .orElse(null);
    }

    public List<EventLogEntryDto> getEventLog() {
        return buildEventLog(buildVehicleStates());
    }

    private List<VehicleStateDto> buildVehicleStates() {
        long t = Instant.now().getEpochSecond() % 60;

        double distance;
        double leadSpeed;
        double followerSpeed;
        double distanceChange;
        boolean emergencyBrake;

        if (t < 20) {
            distance       = 80.0 - t * 2.0;
            leadSpeed      = 80.0;
            followerSpeed  = 84.0;
            distanceChange = -1.1;
            emergencyBrake = false;
        } else if (t < 40) {
            distance       = 40.0 - (t - 20) * 1.0;
            leadSpeed      = 70.0;
            followerSpeed  = 80.0;
            distanceChange = -2.8;
            emergencyBrake = false;
        } else if (t < 52) {
            distance       = 20.0 - (t - 40) * 0.8;
            leadSpeed      = 55.0;
            followerSpeed  = 65.0;
            distanceChange = -2.8;
            emergencyBrake = true;
        } else {
            distance       = 10.4 + (t - 52) * 8.7;
            leadSpeed      = 72.0;
            followerSpeed  = 68.0;
            distanceChange = 2.4;
            emergencyBrake = false;
        }

        Instant now = Instant.now();

        VehicleStateDto lead = new VehicleStateDto();
        lead.vin                 = VIN_LEAD;
        lead.displayName         = "VH-001 (Lead)";
        lead.role                = "LEAD";
        lead.latitude            = BASE_LAT;
        lead.longitude           = BASE_LON + distance * LON_PER_METER;
        lead.speedKmh            = leadSpeed;
        lead.distanceToFrontM    = null;
        lead.distanceToRearM     = distance;
        lead.distanceChangeMps   = -distanceChange;
        lead.emergencyBrakeActive = emergencyBrake;
        lead.updatedAt           = now;

        VehicleStateDto follower = new VehicleStateDto();
        follower.vin                 = VIN_FOLLOWER;
        follower.displayName         = "VH-002 (Follower)";
        follower.role                = "FOLLOWER";
        follower.latitude            = BASE_LAT;
        follower.longitude           = BASE_LON;
        follower.speedKmh            = followerSpeed;
        follower.distanceToFrontM    = distance;
        follower.distanceToRearM     = null;
        follower.distanceChangeMps   = distanceChange;
        follower.emergencyBrakeActive = emergencyBrake;
        follower.updatedAt           = now;

        return List.of(lead, follower);
    }

    private List<EventLogEntryDto> buildEventLog(List<VehicleStateDto> vehicles) {
        Instant now = Instant.now();
        List<EventLogEntryDto> events = new ArrayList<>();

        VehicleStateDto follower = vehicles.stream()
            .filter(v -> "FOLLOWER".equals(v.role)).findFirst().orElse(null);
        VehicleStateDto lead = vehicles.stream()
            .filter(v -> "LEAD".equals(v.role)).findFirst().orElse(null);

        if (follower != null && follower.emergencyBrakeActive) {
            events.add(event(now.minus(1, ChronoUnit.SECONDS), "ERROR", "BRAKENOW",
                "EMERGENCY BRAKE activated for VH-002 — distance critical: " + fmt(follower.distanceToFrontM)));
            events.add(event(now.minus(3, ChronoUnit.SECONDS), "WARN", "SONAR",
                "VH-002 closing rate " + String.format("%.1f", Math.abs(follower.distanceChangeMps)) + " m/s — hard-brake threshold exceeded"));
        }

        events.add(event(now.minus(5, ChronoUnit.SECONDS), "INFO", "SONAR",
            "VH-002 distance to front: " + fmt(follower != null ? follower.distanceToFrontM : null)));
        events.add(event(now.minus(10, ChronoUnit.SECONDS), "INFO", "ORCHESTRATOR",
            "Platoon session active — lead: VH-001, follower: VH-002"));
        events.add(event(now.minus(15, ChronoUnit.SECONDS), "INFO", "UTRACKED",
            "GPS update received for VH-001 and VH-002"));
        events.add(event(now.minus(25, ChronoUnit.SECONDS), "INFO", "ORCHESTRATOR",
            "VH-001 speed: " + (lead != null ? String.format("%.0f km/h", lead.speedKmh) : "N/A")));
        events.add(event(now.minus(40, ChronoUnit.SECONDS), "INFO", "WHEREAMI",
            "GPS heartbeat OK — VH-001, VH-002"));
        events.add(event(now.minus(90, ChronoUnit.SECONDS), "INFO", "ORCHESTRATOR",
            "Platoon configuration verified"));
        events.add(event(now.minus(300, ChronoUnit.SECONDS), "INFO", "ORCHESTRATOR",
            "Simulation scenario started"));

        events.sort((a, b) -> b.timestamp.compareTo(a.timestamp));
        return events;
    }

    private EventLogEntryDto event(Instant ts, String level, String source, String message) {
        EventLogEntryDto e = new EventLogEntryDto();
        e.timestamp = ts;
        e.level     = level;
        e.source    = source;
        e.message   = message;
        return e;
    }

    private String fmt(Double meters) {
        return meters == null ? "N/A" : String.format("%.1f m", meters);
    }
}
