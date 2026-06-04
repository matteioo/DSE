package com.drive2stars.orchestrator.service;

import com.drive2stars.orchestrator.endpoint.UtrackedClient;
import com.drive2stars.orchestrator.endpoint.VehiclePositionDto;
import com.drive2stars.shared.messaging.DistanceMessage;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PlausibilityService {

  private static final Logger LOG = Logger.getLogger(PlausibilityService.class);
  private static final double DEVIATION_THRESHOLD_METERS = 15.0;
  private static final double EARTH_RADIUS_METERS = 6_371_000.0;

  private final UtrackedClient utrackedClient;
  private final BrakeConditionService brakeConditionService;

  public PlausibilityService(@RestClient UtrackedClient utrackedClient,
                              BrakeConditionService brakeConditionService) {
    this.utrackedClient = utrackedClient;
    this.brakeConditionService = brakeConditionService;
  }

  public CompletableFuture<Void> validateDistance(DistanceMessage msg) {
    return CompletableFuture.runAsync(() -> check(msg));
  }

  private void check(DistanceMessage msg) {
    // Stores last two positions for vehicle to derive driving direction
    List<VehiclePositionDto> history;
    try {
      history = utrackedClient.getHistory(msg.vin, 2);
    } catch (Exception e) {
      LOG.warnf("Plausibility check skipped for %s — history unavailable: %s",
          msg.vin, e.getMessage());
      return;
    }

    // Cannot derive driving direction from only one GPS entry, so skip plausibility check in that case
    if (history.size() < 2) {
      LOG.infof("Plausibility check skipped for %s — only %d GPS entries",
          msg.vin, history.size());
      return;
    }

    // derive heading direction
    VehiclePositionDto currentPos = history.get(0);
    VehiclePositionDto previousPos = history.get(1);
    double headingDeltaLon = currentPos.longitude.doubleValue() - previousPos.longitude.doubleValue();
    // headingDeltaLon > 0 = moving east, headingDeltaLon < 0 = moving west
    // Currently unused in 2-vehicle simulation setup


    List<VehiclePositionDto> allPositions;
    try {
      allPositions = utrackedClient.getAllVehiclePositions();
    } catch (Exception e) {
      LOG.warnf("Plausibility check skipped for %s — UTRACKED unavailable: %s", msg.vin, e.getMessage());
      return;
    }

    if (allPositions.size() < 2) {
      LOG.infof("Plausibility check skipped for %s — fewer than 2 vehicles known", msg.vin);
      return;
    }

    VehiclePositionDto self = allPositions.stream()
        .filter(p -> msg.vin.equals(p.vin))
        .findFirst()
        .orElse(null);

    if (self == null) {
      LOG.infof("Plausibility check skipped for %s — no GPS position in UTRACKED yet", msg.vin);
      return;
    }

    double bestDeviation = allPositions.stream()
        .filter(p -> !msg.vin.equals(p.vin))
        .mapToDouble(other -> {
          double gpsDistance = haversine(
              self.latitude.doubleValue(), self.longitude.doubleValue(),
              other.latitude.doubleValue(), other.longitude.doubleValue());
          return Math.abs(gpsDistance - msg.distanceMeters);
        })
        .min()
        .orElse(Double.MAX_VALUE);

    if (bestDeviation == Double.MAX_VALUE) {
      LOG.infof("Plausibility check skipped for %s — no other vehicles in UTRACKED", msg.vin);
      return;
    }

    LOG.debugf("Plausibility check for %s: deviation=%.2fm (threshold=%.1fm)", msg.vin, bestDeviation, DEVIATION_THRESHOLD_METERS);

    if (bestDeviation > DEVIATION_THRESHOLD_METERS) {
      LOG.warnf("Condition 4 triggered for %s — GPS/SONAR deviation %.2fm exceeds %.1fm threshold",
          msg.vin, bestDeviation, DEVIATION_THRESHOLD_METERS);
      brakeConditionService.triggerBrake(msg, 4);
    }
  }

  /** Haversine formula — returns distance in meters between two GPS coordinates. */
  private double haversine(double lat1, double lon1, double lat2, double lon2) {
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }
}
