package com.drive2stars.spider.storefront;

import com.drive2stars.spider.adapter.orchestrator.OrchestratorGrpcAdapter;
import com.drive2stars.spider.adapter.utracked.UtrackedGrpcAdapter;
import com.drive2stars.spider.storefront.dto.BrakeStateDto;
import com.drive2stars.spider.storefront.dto.VehiclePositionDto;
import com.drive2stars.spider.storefront.dto.VehicleStateDto;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Builds vehicle state DTOs from:
 *   GPS positions  — UTRACKED (gRPC)
 *   Brake states   — ORCHESTRATOR (gRPC, from BrakeStateService)
 *   Distance/speed — derived from GPS; speed is still mock until SONAR integration lands
 */
@ApplicationScoped
public class StorefrontMockService {

    private static final String MOCK_VIN_LEAD     = "VH-001";
    private static final String MOCK_VIN_FOLLOWER = "VH-002";

    private static final double MOCK_LEAD_SPEED_KMH     = 72.0;
    private static final double MOCK_FOLLOWER_SPEED_KMH = 86.4;

    private static final double BASE_LAT      = 48.2082;
    private static final double BASE_LON      = 16.3738;
    private static final double LON_PER_METER = 0.0000090;

    private volatile double  lastDistance     = -1;
    private volatile Instant lastDistanceTime = null;

    private final UtrackedGrpcAdapter utrackedGrpcAdapter;
    private final OrchestratorGrpcAdapter orchestratorGrpcAdapter;

    public StorefrontMockService(UtrackedGrpcAdapter utrackedGrpcAdapter,
                                  OrchestratorGrpcAdapter orchestratorGrpcAdapter) {
        this.utrackedGrpcAdapter = utrackedGrpcAdapter;
        this.orchestratorGrpcAdapter = orchestratorGrpcAdapter;
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

    private List<VehicleStateDto> buildVehicleStates() {
        List<VehiclePositionDto> positions = utrackedGrpcAdapter.getLatestPositions();
        Map<String, BrakeStateDto> brakeStates = orchestratorGrpcAdapter.getBrakeStates();

        List<VehicleStateDto> states = (positions.size() >= 2)
                ? buildFromUtrackedPositions(positions)
                : buildMockVehicleStates();

        for (VehicleStateDto v : states) {
            BrakeStateDto brake = brakeStates.get(v.vin);
            if (brake != null) {
                v.emergencyBrakeActive = brake.emergencyBrakeActive;
                v.preEmergencyBrake    = brake.preEmergencyBrakeActive;
            }
        }
        return states;
    }

    private List<VehicleStateDto> buildFromUtrackedPositions(List<VehiclePositionDto> positions) {
        List<VehiclePositionDto> sorted = positions.stream()
            .sorted((a, b) -> Double.compare(b.longitude, a.longitude))
            .toList();

        VehiclePositionDto posLead     = sorted.get(0);
        VehiclePositionDto posFollower = sorted.get(1);

        double distance       = haversineMeters(posLead.latitude, posLead.longitude,
                                                posFollower.latitude, posFollower.longitude);
        double distanceChange = computeDistanceChange(distance);
        Instant now           = Instant.now();

        VehicleStateDto lead = new VehicleStateDto();
        lead.vin               = posLead.vin;
        lead.role              = "LEAD";
        lead.latitude          = posLead.latitude;
        lead.longitude         = posLead.longitude;
        lead.speedKmh          = MOCK_LEAD_SPEED_KMH;
        lead.distanceToFrontM  = null;
        lead.distanceToRearM   = distance;
        lead.distanceChangeMps = distanceChange;
        lead.updatedAt         = now;

        VehicleStateDto follower = new VehicleStateDto();
        follower.vin               = posFollower.vin;
        follower.role              = "FOLLOWER";
        follower.latitude          = posFollower.latitude;
        follower.longitude         = posFollower.longitude;
        follower.speedKmh          = MOCK_FOLLOWER_SPEED_KMH;
        follower.distanceToFrontM  = distance;
        follower.distanceToRearM   = null;
        follower.distanceChangeMps = distanceChange;
        follower.updatedAt         = now;

        return List.of(lead, follower);
    }

    private List<VehicleStateDto> buildMockVehicleStates() {
        long t = Instant.now().getEpochSecond() % 60;

        double distance;
        double leadSpeed;
        double followerSpeed;
        double distanceChange;

        if (t < 20) {
            distance = 80.0 - t * 2.0; leadSpeed = 80.0; followerSpeed = 84.0; distanceChange = -1.1;
        } else if (t < 40) {
            distance = 40.0 - (t - 20) * 1.0; leadSpeed = 70.0; followerSpeed = 80.0; distanceChange = -2.8;
        } else if (t < 52) {
            distance = 20.0 - (t - 40) * 0.8; leadSpeed = 55.0; followerSpeed = 65.0; distanceChange = -2.8;
        } else {
            distance = 10.4 + (t - 52) * 8.7; leadSpeed = 72.0; followerSpeed = 68.0; distanceChange = 2.4;
        }

        Instant now = Instant.now();

        VehicleStateDto lead = new VehicleStateDto();
        lead.vin = MOCK_VIN_LEAD; lead.role = "LEAD";
        lead.latitude = BASE_LAT; lead.longitude = BASE_LON + distance * LON_PER_METER;
        lead.speedKmh = leadSpeed; lead.distanceToFrontM = null; lead.distanceToRearM = distance;
        lead.distanceChangeMps = distanceChange; lead.updatedAt = now;

        VehicleStateDto follower = new VehicleStateDto();
        follower.vin = MOCK_VIN_FOLLOWER; follower.role = "FOLLOWER";
        follower.latitude = BASE_LAT; follower.longitude = BASE_LON;
        follower.speedKmh = followerSpeed; follower.distanceToFrontM = distance;
        follower.distanceToRearM = null; follower.distanceChangeMps = distanceChange;
        follower.updatedAt = now;

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
}
