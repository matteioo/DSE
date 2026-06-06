package com.drive2stars.spider.storefront;

import com.drive2stars.spider.adapter.orchestrator.OrchestratorGrpcAdapter;
import com.drive2stars.spider.adapter.sonar.SonarGrpcAdapter;
import com.drive2stars.spider.adapter.utracked.UtrackedGrpcAdapter;
import com.drive2stars.spider.storefront.dto.BrakeStateDto;
import com.drive2stars.spider.storefront.dto.VehiclePositionDto;
import com.drive2stars.spider.storefront.dto.VehicleStateDto;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class StorefrontService {

    private record CachedPosition(double lat, double lon, Instant seenAt, double speedKmh) {}
    private final Map<String, CachedPosition> positionCache = new ConcurrentHashMap<>();
    // true when emergency brake fires releases once signal clears, speed is 0.
    private final Set<String> brakeLatch = ConcurrentHashMap.newKeySet();

    private final UtrackedGrpcAdapter utrackedAdapter;
    private final OrchestratorGrpcAdapter orchestratorAdapter;
    private final SonarGrpcAdapter sonarAdapter;

    public StorefrontService(UtrackedGrpcAdapter utrackedAdapter,
                              OrchestratorGrpcAdapter orchestratorAdapter,
                              SonarGrpcAdapter sonarAdapter) {
        this.utrackedAdapter = utrackedAdapter;
        this.orchestratorAdapter = orchestratorAdapter;
        this.sonarAdapter = sonarAdapter;
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
        List<VehiclePositionDto> positions = utrackedAdapter.getLatestPositions();
        Map<String, BrakeStateDto> brakeStates = orchestratorAdapter.getBrakeStates();
        List<SonarGrpcAdapter.SonarReadingDto> sonarReadings = sonarAdapter.getLatestReadings();

        Map<String, Double> distanceByVinDir = new HashMap<>();
        Map<String, Double> changeRateByVinDir = new HashMap<>();
        for (SonarGrpcAdapter.SonarReadingDto r : sonarReadings) {
            distanceByVinDir.put(r.vin() + "|" + r.direction(), r.distanceMeters());
            changeRateByVinDir.put(r.vin() + "|" + r.direction(), r.distanceChangeMps());
        }

        return positions.stream().map(pos -> {
            VehicleStateDto dto = new VehicleStateDto();
            dto.vin       = pos.vin;
            dto.latitude  = pos.latitude;
            dto.longitude = pos.longitude;
            dto.updatedAt = Instant.now();

            dto.distanceToFrontM  = distanceByVinDir.get(pos.vin + "|FRONT");
            dto.distanceToRearM   = distanceByVinDir.get(pos.vin + "|BACK");
            dto.distanceChangeMps = changeRateByVinDir.getOrDefault(pos.vin + "|FRONT",
                                    changeRateByVinDir.getOrDefault(pos.vin + "|BACK", 0.0));

            // vehicle with no FRONT sonar reading is at the front
            dto.role = dto.distanceToFrontM == null ? "LEAD" : "FOLLOWER";

            dto.speedKmh = estimateSpeedKmh(pos);

            BrakeStateDto brake = brakeStates.get(pos.vin);
            if (brake != null) {
                dto.preEmergencyBrake = brake.preEmergencyBrakeActive;
                // when emergency fires so red stays visible through
                // release when safe no emergency
                if (brake.emergencyBrakeActive) {
                    brakeLatch.add(pos.vin);
                } else if (!brake.preEmergencyBrakeActive || dto.speedKmh < 0.5) {
                    brakeLatch.remove(pos.vin);
                }
                dto.emergencyBrakeActive = brake.emergencyBrakeActive || brakeLatch.contains(pos.vin);
            }

            return dto;
        }).toList();
    }

    // Speed is estimated from consecutive GPS updates using haversine
    // GPS position change compute distance/elapsed
    private double estimateSpeedKmh(VehiclePositionDto pos) {
        Instant now = Instant.now();
        CachedPosition cached = positionCache.get(pos.vin);

        if (cached == null) {
            positionCache.put(pos.vin, new CachedPosition(pos.latitude, pos.longitude, now, 0.0));
            return 0.0;
        }

        boolean positionUnchanged = Math.abs(cached.lat() - pos.latitude) < 1e-7
                                 && Math.abs(cached.lon() - pos.longitude) < 1e-7;
        if (positionUnchanged) {
            long elapsedSinceLastChange = now.toEpochMilli() - cached.seenAt().toEpochMilli();
            if (elapsedSinceLastChange > 15_000) {
                positionCache.put(pos.vin, new CachedPosition(cached.lat(), cached.lon(), now, 0.0));
                return 0.0;
            }
            return cached.speedKmh();
        }

        long elapsedMs = now.toEpochMilli() - cached.seenAt().toEpochMilli();
        double distanceM = haversineMeters(cached.lat(), cached.lon(), pos.latitude, pos.longitude);
        double speedKmh = elapsedMs > 0 ? (distanceM / (elapsedMs / 1000.0)) * 3.6 : 0.0;

        positionCache.put(pos.vin, new CachedPosition(pos.latitude, pos.longitude, now, speedKmh));
        return speedKmh;
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
