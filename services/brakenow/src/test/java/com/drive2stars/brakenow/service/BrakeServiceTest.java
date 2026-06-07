package com.drive2stars.brakenow.service;

import com.drive2stars.brakenow.mq.BrakePublisher;
import com.drive2stars.brakenow.mq.SimulatorPublisher;
import com.drive2stars.shared.messaging.BrakeMessage;
import com.drive2stars.shared.messaging.DistanceMessage;
import com.drive2stars.shared.messaging.SimulatorBrakeMessage;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrakeServiceTest {

    @Mock
    BrakePublisher brakePublisher;

    @Mock
    SimulatorPublisher simulatorPublisher;

    BrakeService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new BrakeService(brakePublisher);
        inject(service, "resumeDistanceM", 150.0);
        inject(service, "ownVin", "VIN-1");
        inject(service, "simulatorPublisher", simulatorPublisher);
    }

    @Test
    void normalStateWhenDistanceLarge() {
        service.processDistance("VIN-1", 100.0, 2.0, DistanceMessage.Direction.FRONT);

        BrakeMessage msg = captureBreakMessage();
        assertFalse(msg.active);
        assertFalse(msg.preEmergencyBrake);
        assertEquals(0, msg.conditionTriggered);
    }

    @Test
    void preEmergencyOnlyWhenDistanceBelow45() {
        service.processDistance("VIN-1", 40.0, 1.0, DistanceMessage.Direction.FRONT);

        BrakeMessage msg = captureBreakMessage();
        assertFalse(msg.active);
        assertTrue(msg.preEmergencyBrake);
        assertEquals(0, msg.conditionTriggered);

        SimulatorBrakeMessage sim = captureSimulatorMessage();
        assertFalse(sim.brakeActive);
        assertTrue(sim.preBrake);
    }

    @Test
    void condition1TriggersBrakeAndPreEmergency() {
        service.processDistance("VIN-1", 25.0, 5.0, DistanceMessage.Direction.FRONT);

        BrakeMessage msg = captureBreakMessage();
        assertTrue(msg.active);
        assertTrue(msg.preEmergencyBrake);
        assertEquals(1, msg.conditionTriggered);
    }

    @Test
    void condition1NotTriggeredWhenRateTooLow() {
        service.processDistance("VIN-1", 25.0, 3.9, DistanceMessage.Direction.FRONT);

        BrakeMessage msg = captureBreakMessage();
        assertFalse(msg.active);
    }

    @Test
    void condition2TriggersBrakeAndPreEmergency() {
        service.processDistance("VIN-1", 10.0, 2.5, DistanceMessage.Direction.FRONT);

        BrakeMessage msg = captureBreakMessage();
        assertTrue(msg.active);
        assertTrue(msg.preEmergencyBrake);
        assertEquals(2, msg.conditionTriggered);
    }

    @Test
    void condition2NotTriggeredWhenRateTooLow() {
        service.processDistance("VIN-1", 10.0, 1.9, DistanceMessage.Direction.FRONT);

        BrakeMessage msg = captureBreakMessage();
        assertFalse(msg.active);
    }

    @Test
    void condition3TriggersBrakeAndPreEmergency() {
        service.processDistance("VIN-1", 3.0, 0.1, DistanceMessage.Direction.FRONT);

        BrakeMessage msg = captureBreakMessage();
        assertTrue(msg.active);
        assertTrue(msg.preEmergencyBrake);
        assertEquals(3, msg.conditionTriggered);
    }

    @Test
    void condition3NotTriggeredWhenReceding() {
        service.processDistance("VIN-1", 3.0, -0.5, DistanceMessage.Direction.FRONT);

        BrakeMessage msg = captureBreakMessage();
        assertFalse(msg.active);
    }

    @Test
    void condition3TriggersBrakeAtHighClosingRate() {
        service.processDistance("VIN-1", 3.0, 3.0, DistanceMessage.Direction.FRONT);

        BrakeMessage msg = captureBreakMessage();
        assertTrue(msg.active);
        assertTrue(msg.preEmergencyBrake);
        assertEquals(3, msg.conditionTriggered);
    }

    // Pre-emergency at 45 m

    @Test
    void preEmergencyExitsWhenDistanceReaches45() {
        service.processDistance("VIN-1", 40.0, 0.0, DistanceMessage.Direction.FRONT);
        reset(brakePublisher, simulatorPublisher);

        service.processDistance("VIN-1", 45.0, 0.0, DistanceMessage.Direction.FRONT);

        BrakeMessage msg = captureBreakMessage();
        assertFalse(msg.preEmergencyBrake);
        assertFalse(msg.active);
    }

    @Test
    void publishedForEveryFrontReading() {
        service.processDistance("VIN-1", 100.0, 0.0, DistanceMessage.Direction.FRONT);
        service.processDistance("VIN-1", 100.0, 0.0, DistanceMessage.Direction.FRONT);

        verify(brakePublisher, times(2)).publish(any());
    }

    @Test
    void backSensorReadingNotPublished() {
        service.processDistance("VIN-1", 100.0, 0.0, DistanceMessage.Direction.BACK);

        verifyNoInteractions(brakePublisher, simulatorPublisher);
    }

    @Test
    void publishedAgainWhenStateChanges() {
        service.processDistance("VIN-1", 100.0, 0.0, DistanceMessage.Direction.FRONT);
        service.processDistance("VIN-1", 40.0, 0.0, DistanceMessage.Direction.FRONT);  // pre-emergency

        verify(brakePublisher, times(2)).publish(any());
    }

    @Test
    void publishedMessageHasBrakeNowSource() {
        service.processDistance("VIN-1", 3.0, 1.0, DistanceMessage.Direction.FRONT);

        BrakeMessage msg = captureBreakMessage();
        assertEquals(BrakeMessage.Source.BRAKENOW, msg.source);
    }

    @Test
    void preEmergencyPublishedForBackSensorWhenClose() {
        service.processDistance("VIN-1", 40.0, 1.0, DistanceMessage.Direction.BACK);

        BrakeMessage msg = captureBreakMessage();
        assertFalse(msg.active);
        assertTrue(msg.preEmergencyBrake);
    }

    @Test
    void backPreEmergencyClearedWhenDistanceExceeds45() {
        service.processDistance("VIN-1", 40.0, 1.0, DistanceMessage.Direction.BACK);
        reset(brakePublisher, simulatorPublisher);

        service.processDistance("VIN-1", 45.0, 0.0, DistanceMessage.Direction.BACK);

        BrakeMessage msg = captureBreakMessage();
        assertFalse(msg.active);
        assertFalse(msg.preEmergencyBrake);
    }

    @Test
    void backNormalDistanceNotRepublishedAfterClearing() {
        service.processDistance("VIN-1", 40.0, 1.0, DistanceMessage.Direction.BACK);
        service.processDistance("VIN-1", 50.0, 0.0, DistanceMessage.Direction.BACK);
        reset(brakePublisher, simulatorPublisher);

        service.processDistance("VIN-1", 100.0, 0.0, DistanceMessage.Direction.BACK);

        verifyNoInteractions(brakePublisher, simulatorPublisher);
    }

    @Test
    void normalDistanceOnFrontAfterPreEmergency() {
        service.processDistance("VIN-1", 40.0, 1.0, DistanceMessage.Direction.FRONT);
        reset(brakePublisher, simulatorPublisher);

        service.processDistance("VIN-1", 100.0, -1.0, DistanceMessage.Direction.FRONT);

        BrakeMessage msg = captureBreakMessage();
        assertFalse(msg.active);
        assertFalse(msg.preEmergencyBrake);
    }

    @Test
    void emergencyBrakeReleasedWhenDistanceExceedsResumeThreshold() {
        service.processDistance("VIN-1", 3.0, 1.0, DistanceMessage.Direction.FRONT);
        reset(brakePublisher, simulatorPublisher);

        service.processDistance("VIN-1", 155.0, -1.0, DistanceMessage.Direction.FRONT);

        BrakeMessage msg = captureBreakMessage();
        assertFalse(msg.active);

        SimulatorBrakeMessage sim = captureSimulatorMessage();
        assertFalse(sim.brakeActive);
    }

    @Test
    void preEmergencyFiredOnSecondApproachAfterFullCycle() {
        service.processDistance("VIN-1", 40.0, 1.0, DistanceMessage.Direction.FRONT);
        service.processDistance("VIN-1", 3.0, 1.0, DistanceMessage.Direction.FRONT);
        service.processDistance("VIN-1", 155.0, -1.0, DistanceMessage.Direction.FRONT);
        reset(brakePublisher, simulatorPublisher);

        service.processDistance("VIN-1", 40.0, 1.0, DistanceMessage.Direction.FRONT);

        BrakeMessage msg = captureBreakMessage();
        assertFalse(msg.active);
        assertTrue(msg.preEmergencyBrake);
    }

    @Test
    void emergencyBrakeRefiredAfterNormalState() {
        service.processDistance("VIN-1", 3.0, 1.0, DistanceMessage.Direction.FRONT);
        service.processDistance("VIN-1", 155.0, -1.0, DistanceMessage.Direction.FRONT);
        reset(brakePublisher, simulatorPublisher);

        service.processDistance("VIN-1", 3.0, 1.0, DistanceMessage.Direction.FRONT);

        BrakeMessage msg = captureBreakMessage();
        assertTrue(msg.active);
        assertTrue(msg.preEmergencyBrake);
        assertEquals(3, msg.conditionTriggered);
    }

    @Test
    void resetStateBypassesHoldOnNextReading() {
        service.processDistance("VIN-1", 3.0, 1.0, DistanceMessage.Direction.FRONT);
        service.resetState();
        reset(brakePublisher, simulatorPublisher);

        service.processDistance("VIN-1", 80.0, 1.38, DistanceMessage.Direction.FRONT);

        BrakeMessage msg = captureBreakMessage();
        assertFalse(msg.active);
        assertFalse(msg.preEmergencyBrake);
    }

    @Test
    void resetStateClearsGetState() {
        service.processDistance("VIN-1", 3.0, 1.0, DistanceMessage.Direction.FRONT);
        assertNotNull(service.getState("VIN-1"));

        service.resetState();

        assertNull(service.getState("VIN-1"));
        assertTrue(service.getAllStates().isEmpty());
    }

    @Test
    void processBrakeForwardsActiveToSimulator() {
        service.processBrake("VIN-1", true, 4, Instant.now());

        SimulatorBrakeMessage sim = captureSimulatorMessage();
        assertTrue(sim.brakeActive);
        assertFalse(sim.preBrake);
        assertEquals("VIN-1", sim.vin);
    }

    @Test
    void processBrakeForwardsClearToSimulator() {
        service.processBrake("VIN-1", false, 4, Instant.now());

        SimulatorBrakeMessage sim = captureSimulatorMessage();
        assertFalse(sim.brakeActive);
        assertFalse(sim.preBrake);
    }

    @Test
    void processBrakeUpdatesInternalState() {
        service.processBrake("VIN-1", true, 4, Instant.now());

        BrakeState state = service.getState("VIN-1");
        assertNotNull(state);
        assertTrue(state.emergencyBrakeActive);
        assertEquals(4, state.conditionTriggered);
        verifyNoInteractions(brakePublisher);
    }

    private BrakeMessage captureBreakMessage() {
        ArgumentCaptor<BrakeMessage> captor = ArgumentCaptor.forClass(BrakeMessage.class);
        verify(brakePublisher).publish(captor.capture());
        return captor.getValue();
    }

    private SimulatorBrakeMessage captureSimulatorMessage() {
        ArgumentCaptor<SimulatorBrakeMessage> captor = ArgumentCaptor.forClass(SimulatorBrakeMessage.class);
        verify(simulatorPublisher).publish(captor.capture());
        return captor.getValue();
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        var field = BrakeService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
