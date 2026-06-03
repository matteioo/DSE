package com.drive2stars.spider.storefront;

import com.drive2stars.spider.adapter.utracked.UtrackedClient;
import com.drive2stars.spider.adapter.utracked.UtrackedPositionDto;
import com.drive2stars.spider.storefront.dto.EventLogEntryDto;
import com.drive2stars.spider.storefront.dto.VehicleStateDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Real data UTRACKED
 *   GPS latitude/longitude per VIN.
 *   Distance between vehicles computed.
 *   Distance-change rate.
 */
@ApplicationScoped
public class StorefrontMockService {

    // Fallback VINs used only when UTRACKED returns no data
    private static final String MOCK_VIN_LEAD     = "VH-001";
    private static final String MOCK_VIN_FOLLOWER = "VH-002";

    private static final double MOCK_LEAD_SPEED_KMH     = 72.0;
    private static final double MOCK_FOLLOWER_SPEED_KMH = 86.4;

    private static final double EMERGENCY_BRAKE_THRESHOLD_M = 20.0;

    private static final double BASE_LAT           = 48.2082;
    private static final double BASE_LON           = 16.3738;
    private static final double LON_PER_METER      = 0.0000090;
    private static final double MOCK_INITIAL_GAP_M = 120.0;

    @Inject
    UtrackedClient utrackedClient;

    private volatile double  lastDistance     = -1;
    private volatile Instant lastDistanceTime = null;

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
        List<UtrackedPositionDto> positions = utrackedClient.getLatestPositions();

        if (positions.size() >= 2) {
            return buildFromUtrackedPositions(positions);
        }
        return buildMockVehicleStates();
    }


    private List<VehicleStateDto> buildFromUtrackedPositions(List<UtrackedPositionDto> positions) {
        List<UtrackedPositionDto> sorted = positions.stream()
            .sorted((a, b) -> Double.compare(b.longitude, a.longitude))
            .toList();

        UtrackedPositionDto posLead     = sorted.get(0);
        UtrackedPositionDto posFollower = sorted.get(1);

        double distance       = haversineMeters(posLead.latitude, posLead.longitude,
                                                posFollower.latitude, posFollower.longitude);
        double distanceChange = computeDistanceChange(distance);
        boolean emergencyBrake = distance < EMERGENCY_BRAKE_THRESHOLD_M;

        Instant now = Instant.now();

        VehicleStateDto lead = new VehicleStateDto();
        lead.vin                  = posLead.vin;
        lead.role                 = "LEAD";
        lead.latitude             = posLead.latitude;
        lead.longitude            = posLead.longitude;
        lead.speedKmh             = MOCK_LEAD_SPEED_KMH;
        lead.distanceToFrontM     = null;
        lead.distanceToRearM      = distance;
        lead.distanceChangeMps    = distanceChange;
        lead.emergencyBrakeActive = emergencyBrake;
        lead.updatedAt            = now;

        VehicleStateDto follower = new VehicleStateDto();
        follower.vin                  = posFollower.vin;D
        follower.role                 = "FOLLOWER";
        follower.latitude             = posFollower.latitude;
        follower.longitude            = posFollower.longitude;
        follower.speedKmh             = MOCK_FOLLOWER_SPEED_KMH;
        follower.distanceToFrontM     = distance;
        follower.distanceToRearM      = null;
        follower.distanceChangeMps    = distanceChange;
        follower.emergencyBrakeActive = emergencyBrake;
        follower.updatedAt            = now;

        return List.of(lead, follower);
    }

    private List<VehicleStateDto> buildMockVehicleStates() {
        long t = Instant.now().getEpochSecond() % 60;

        double distance; double leadSpeed; double followerSpeed;
        double distanceChange; boolean emergencyBrake;

        if (t < 20) {
            distance = 80.0 - t * 2.0; leadSpeed = 80.0; followerSpeed = 84.0;
            distanceChange = -1.1; emergencyBrake = false;
        } else if (t < 40) {
            distance = 40.0 - (t - 20) * 1.0; leadSpeed = 70.0; followerSpeed = 80.0;
            distanceChange = -2.8; emergencyBrake = false;
        } else if (t < 52) {
            distance = 20.0 - (t - 40) * 0.8; leadSpeed = 55.0; followerSpeed = 65.0;
            distanceChange = -2.8; emergencyBrake = true;
        } else {
            distance = 10.4 + (t - 52) * 8.7; leadSpeed = 72.0; followerSpeed = 68.0;
            distanceChange = 2.4; emergencyBrake = false;
        }

        Instant now = Instant.now();

        VehicleStateDto lead = new VehicleStateDto();
        lead.vin = MOCK_VIN_LEAD; lead.role = "LEAD";
        lead.latitude = BASE_LAT; lead.longitude = BASE_LON + distance * LON_PER_METER;
        lead.speedKmh = leadSpeed; lead.distanceToFrontM = null; lead.distanceToRearM = distance;
        lead.distanceChangeMps = distanceChange; lead.emergencyBrakeActive = emergencyBrake;
        lead.updatedAt = now;

        VehicleStateDto follower = new VehicleStateDto();
        follower.vin = MOCK_VIN_FOLLOWER; follower.role = "FOLLOWER";
        follower.latitude = BASE_LAT; follower.longitude = BASE_LON;
        follower.speedKmh = followerSpeed; follower.distanceToFrontM = distance;
        follower.distanceToRearM = null; follower.distanceChangeMps = distanceChange;
        follower.emergencyBrakeActive = emergencyBrake; follower.updatedAt = now;

        return List.of(lead, follower);
    }

    private double computeDistanceChange(double currentDistance) {
        double rate = 0.0;
        if (lastDistance >= 0 && lastDistanceTime != null) {
            long elapsedMs = Duration.between(lastDistanceTime, Instant.now()).toMillis();
            if (elapsedMs > 0) {
                rate = (currentDistance - lastDistance) / (elapsedMs / 1000.0);
            }
        }
        lastDistance     = currentDistance;
        lastDistanceTime = Instant.now();
        return rate;
    }

    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6_371_000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a    = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                    * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private List<EventLogEntryDto> buildEventLog(List<VehicleStateDto> vehicles) {
        Instant now = Instant.now();
        List<EventLogEntryDto> events = new ArrayList<>();

        VehicleStateDto follower = vehicles.stream()
            .filter(v -> "FOLLOWER".equals(v.role)).findFirst().orElse(null);
        VehicleStateDto lead = vehicles.stream()
            .filter(v -> "LEAD".equals(v.role)).findFirst().orElse(null);

        String leadVin     = lead     != null ? lead.vin     : MOCK_VIN_LEAD;
        String followerVin = follower != null ? follower.vin : MOCK_VIN_FOLLOWER;

        if (follower != null && follower.emergencyBrakeActive) {
            events.add(event(now.minus(1, ChronoUnit.SECONDS), "ERROR", "BRAKENOW",
                "EMERGENCY BRAKE activated for " + followerVin
                + " — distance: " + fmt(follower.distanceToFrontM)));
            events.add(event(now.minus(3, ChronoUnit.SECONDS), "WARN", "SONAR",
                followerVin + " closing rate "
                + String.format("%.1f", Math.abs(follower.distanceChangeMps))
                + " m/s — hard-brake threshold exceeded"));
        }

        events.add(event(now.minus(5,   ChronoUnit.SECONDS), "INFO", "SONAR",
            followerVin + " distance to front: " + fmt(follower != null ? follower.distanceToFrontM : null)));
        events.add(event(now.minus(10,  ChronoUnit.SECONDS), "INFO", "ORCHESTRATOR",
            "Platoon active — lead: " + leadVin + ", follower: " + followerVin));
        events.add(event(now.minus(15,  ChronoUnit.SECONDS), "INFO", "UTRACKED",
            "GPS update received for " + leadVin + " and " + followerVin));
        events.add(event(now.minus(25,  ChronoUnit.SECONDS), "INFO", "ORCHESTRATOR",
            "Lead speed: " + (lead != null ? String.format("%.1f km/h", lead.speedKmh) : "N/A")));
        events.add(event(now.minus(40,  ChronoUnit.SECONDS), "INFO", "WHEREAMI",
            "GPS heartbeat OK — " + leadVin + ", " + followerVin));
        events.add(event(now.minus(90,  ChronoUnit.SECONDS), "INFO", "ORCHESTRATOR",
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

    private String fmt(Double m) {
        return m == null ? "N/A" : String.format("%.1f m", m);
    }
}
