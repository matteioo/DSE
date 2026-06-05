package com.drive2stars.orchestrator.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.drive2stars.orchestrator.endpoint.UtrackedClient;
import com.drive2stars.orchestrator.endpoint.VehiclePositionDto;
import com.drive2stars.shared.messaging.DistanceMessage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlausibilityServiceTest {

  private static final String VIN = "VIN-1";
  private static final String OTHER = "VIN-2";

  @Mock
  UtrackedClient utrackedClient;
  @Mock
  BrakeConditionService brakeConditionService;
  @InjectMocks
  PlausibilityService service;

  private DistanceMessage msg(double distanceMeters) {
    return new DistanceMessage(VIN, distanceMeters, 5.0, DistanceMessage.Direction.FRONT,
        Instant.now());
  }

  private VehiclePositionDto pos(String vin, double lat, double lon) {
    VehiclePositionDto dto = new VehiclePositionDto();
    dto.vin = vin;
    dto.latitude = BigDecimal.valueOf(lat);
    dto.longitude = BigDecimal.valueOf(lon);
    return dto;
  }

  /**
   * Two GPS history entries for the reporting vehicle — heading derivation only needs 2 points,
   * exact coordinates don't matter for current logic.
   */
  private List<VehiclePositionDto> history() {
    return List.of(pos(VIN, 0.0002, 0.0), pos(VIN, 0.0, 0.0));
  }

  // At the equator, 0.001 degree longitude ≈ 111.2 m (Haversine, Earth radius 6 371 000 m).
  // These helpers return positions with that known separation.
  private List<VehiclePositionDto> twoVehicles() {
    return List.of(pos(VIN, 0.0, 0.0), pos(OTHER, 0.0, 0.001));
  }

  // --- Condition 4 trigger ---

  @Test
  void condition4_triggers_when_sonar_deviates_significantly_from_gps() {
    // GPS distance ≈ 111.3 m; SONAR reports 200 m → deviation ≈ 88.7 m > 15 m threshold
    when(utrackedClient.getHistory(VIN, 2)).thenReturn(history());
    when(utrackedClient.getAllVehiclePositions()).thenReturn(twoVehicles());

    service.validateDistance(msg(200.0)).join();

    verify(brakeConditionService).triggerBrake(any(DistanceMessage.class), eq(4));
  }

  @Test
  void condition4_not_triggered_when_sonar_matches_gps() {
    // GPS distance ≈ 111.3 m; SONAR reports 115 m → deviation ≈ 3.7 m < 15 m threshold
    when(utrackedClient.getHistory(VIN, 2)).thenReturn(history());
    when(utrackedClient.getAllVehiclePositions()).thenReturn(twoVehicles());

    service.validateDistance(msg(115.0)).join();

    verify(brakeConditionService, never()).triggerBrake(any(), eq(4));
  }

  // --- Guard: UTRACKED history unavailable ---

  @Test
  void check_skipped_when_history_call_throws() {
    when(utrackedClient.getHistory(VIN, 2)).thenThrow(new RuntimeException("UTRACKED down"));

    service.validateDistance(msg(200.0)).join();

    verifyNoInteractions(brakeConditionService);
  }

  @Test
  void check_skipped_when_fewer_than_two_history_entries() {
    when(utrackedClient.getHistory(VIN, 2)).thenReturn(List.of(pos(VIN, 0.0, 0.0)));

    service.validateDistance(msg(200.0)).join();

    verifyNoInteractions(brakeConditionService);
  }

  // --- Guard: UTRACKED positions unavailable ---

  @Test
  void check_skipped_when_positions_call_throws() {
    when(utrackedClient.getHistory(VIN, 2)).thenReturn(history());
    when(utrackedClient.getAllVehiclePositions()).thenThrow(new RuntimeException("UTRACKED down"));

    service.validateDistance(msg(200.0)).join();

    verifyNoInteractions(brakeConditionService);
  }

  @Test
  void check_skipped_when_only_one_vehicle_position_known() {
    when(utrackedClient.getHistory(VIN, 2)).thenReturn(history());
    when(utrackedClient.getAllVehiclePositions()).thenReturn(List.of(pos(VIN, 0.0, 0.0)));

    service.validateDistance(msg(200.0)).join();

    verifyNoInteractions(brakeConditionService);
  }

  @Test
  void check_skipped_when_history_is_empty() {
    when(utrackedClient.getHistory(VIN, 2)).thenReturn(List.of());

    service.validateDistance(msg(200.0)).join();

    verifyNoInteractions(brakeConditionService);
  }

  @Test
  void check_skipped_when_own_vin_not_in_positions() {
    // Two positions returned but neither matches VIN — startup edge case where
    // UTRACKED has not yet received a GPS reading for the reporting vehicle.
    when(utrackedClient.getHistory(VIN, 2)).thenReturn(history());
    when(utrackedClient.getAllVehiclePositions())
        .thenReturn(List.of(pos(OTHER, 0.0, 0.001), pos("VIN-3", 0.0, 0.002)));

    service.validateDistance(msg(200.0)).join();

    verify(brakeConditionService, never()).triggerBrake(any(), eq(4));
  }

  @Test
  void condition4_not_triggered_when_deviation_below_threshold() {
    // GPS ≈ 111.2 m; SONAR = 126.0 m → deviation ≈ 14.8 m, NOT strictly > 15 m
    when(utrackedClient.getHistory(VIN, 2)).thenReturn(history());
    when(utrackedClient.getAllVehiclePositions()).thenReturn(twoVehicles());

    service.validateDistance(msg(126.0)).join();

    verify(brakeConditionService, never()).triggerBrake(any(), eq(4));
  }
}
