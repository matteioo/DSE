package com.drive2stars.orchestrator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.drive2stars.orchestrator.mq.EmergencyBrakeProducer;
import com.drive2stars.shared.messaging.BrakeMessage;
import com.drive2stars.shared.messaging.DistanceMessage;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BrakeConditionServiceTest {

  @Mock
  EmergencyBrakeProducer brakeProducer;
  @Mock
  EventService eventService;
  @InjectMocks
  BrakeConditionService service;

  private DistanceMessage msg(double distanceMeters, double changeRateMps) {
    return new DistanceMessage("VIN-TEST", distanceMeters, changeRateMps,
        DistanceMessage.Direction.FRONT, Instant.now());
  }

  // --- Condition 1: distanceMeters < 30 AND changeRateMps > 4 ---

  @Test
  void condition1_triggers_when_close_and_approaching_fast() {
    // 25 < 30, 5.0 > 4 → only cond1 fires (25 not < 15 or < 5)
    service.checkSonarConditions(msg(25.0, 5.0)).join();

    ArgumentCaptor<BrakeMessage> captor = ArgumentCaptor.forClass(BrakeMessage.class);
    verify(brakeProducer).sendBrakeMessage(captor.capture());
    assertEquals(1, captor.getValue().conditionTriggered);
    verify(eventService).recordBrake("VIN-TEST", 1);
  }

  @Test
  void condition1_not_triggered_when_distance_above_30() {
    service.checkSonarConditions(msg(31.0, 5.0)).join();
    verifyNoInteractions(brakeProducer);
  }

  @Test
  void condition1_not_triggered_when_rate_at_boundary() {
    // changeRateMps = 4 is NOT strictly > 4
    service.checkSonarConditions(msg(25.0, 4.0)).join();
    verifyNoInteractions(brakeProducer);
  }

  @Test
  void condition1_not_triggered_when_receding() {
    service.checkSonarConditions(msg(25.0, -5.0)).join();
    verifyNoInteractions(brakeProducer);
  }

  // --- Condition 2: distanceMeters < 15 AND changeRateMps > 2 ---

  @Test
  void condition2_triggers_when_very_close_and_approaching() {
    // 10 < 15, 3.0 > 2 → only cond2 fires (10 not < 5, rate 3 not > 4)
    service.checkSonarConditions(msg(10.0, 3.0)).join();

    ArgumentCaptor<BrakeMessage> captor = ArgumentCaptor.forClass(BrakeMessage.class);
    verify(brakeProducer).sendBrakeMessage(captor.capture());
    assertEquals(2, captor.getValue().conditionTriggered);
    verify(eventService).recordBrake("VIN-TEST", 2);
  }

  @Test
  void condition2_not_triggered_when_distance_above_15() {
    service.checkSonarConditions(msg(16.0, 3.0)).join();
    verifyNoInteractions(brakeProducer);
  }

  @Test
  void condition2_not_triggered_when_rate_at_boundary() {
    // changeRateMps = 2 is NOT strictly > 2
    service.checkSonarConditions(msg(10.0, 2.0)).join();
    verifyNoInteractions(brakeProducer);
  }

  // --- Condition 3: distanceMeters < 5 AND changeRateMps > 0 ---

  @Test
  void condition3_triggers_at_critical_distance() {
    // 3 < 5, 0.1 > 0 → only cond3 fires (rate 0.1 not > 2 or > 4)
    service.checkSonarConditions(msg(3.0, 0.1)).join();

    ArgumentCaptor<BrakeMessage> captor = ArgumentCaptor.forClass(BrakeMessage.class);
    verify(brakeProducer).sendBrakeMessage(captor.capture());
    assertEquals(3, captor.getValue().conditionTriggered);
    verify(eventService).recordBrake("VIN-TEST", 3);
  }

  @Test
  void condition3_not_triggered_when_distance_above_5() {
    service.checkSonarConditions(msg(6.0, 1.0)).join();
    verifyNoInteractions(brakeProducer);
  }

  @Test
  void condition3_not_triggered_when_not_approaching() {
    // changeRateMps = 0 is NOT strictly > 0
    service.checkSonarConditions(msg(3.0, 0.0)).join();
    verifyNoInteractions(brakeProducer);
  }

  // --- Exact distance boundaries (strict < not <=) ---

  @Test
  void condition1_not_triggered_when_distance_exactly_at_boundary() {
    // distanceMeters = 30 is NOT strictly < 30
    service.checkSonarConditions(msg(30.0, 5.0)).join();
    verifyNoInteractions(brakeProducer);
  }

  @Test
  void condition2_not_triggered_when_distance_exactly_at_boundary() {
    // distanceMeters = 15 is NOT strictly < 15
    service.checkSonarConditions(msg(15.0, 3.0)).join();
    verifyNoInteractions(brakeProducer);
  }

  @Test
  void condition3_not_triggered_when_distance_exactly_at_boundary() {
    // distanceMeters = 5 is NOT strictly < 5
    service.checkSonarConditions(msg(5.0, 1.0)).join();
    verifyNoInteractions(brakeProducer);
  }

  // --- Combined scenarios ---

  @Test
  void conditions1_and_2_trigger_but_not_3() {
    // dist=10: 10<30 && 5>4 → cond1, 10<15 && 5>2 → cond2, 10 NOT < 5 → no cond3
    service.checkSonarConditions(msg(10.0, 5.0)).join();

    verify(brakeProducer, times(2)).sendBrakeMessage(any());
    verify(eventService, times(2)).recordBrake(eq("VIN-TEST"), any(Integer.class));
  }

  @Test
  void all_three_conditions_trigger_simultaneously() {
    // dist=4: 4<30 && 5>4 ✓, 4<15 && 5>2 ✓, 4<5 && 5>0 ✓
    service.checkSonarConditions(msg(4.0, 5.0)).join();

    verify(brakeProducer, times(3)).sendBrakeMessage(any());
    verify(eventService, times(3)).recordBrake(eq("VIN-TEST"), any(Integer.class));
  }

  @Test
  void safe_distance_triggers_nothing() {
    service.checkSonarConditions(msg(100.0, 0.0)).join();
    verifyNoInteractions(brakeProducer);
    verifyNoInteractions(eventService);
  }
}
