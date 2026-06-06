package com.drive2stars.brakenow.service;

import com.drive2stars.brakenow.mq.BrakePublisher;
import com.drive2stars.brakenow.mq.SimulatorPublisher;
import com.drive2stars.shared.messaging.BrakeMessage;
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
        service.processDistance("VIN-1", 100.0, 2.0);

        BrakeMessage msg = captureBreakMessage();
        assertFalse(msg.active);
        assertFalse(msg.preEmergencyBrake);
        assertEquals(0, msg.conditionTriggered);
    }

    @Test
    void preEmergencyOnlyWhenDistanceBelow45() {
        service.processDistance("VIN-1", 40.0, 1.0);

        BrakeMessage msg = captureBreakMessage();
        assertFalse(msg.active);
        assertTrue(msg.preEmergencyBrake);
        assertEquals(0, msg.conditionTriggered);

        SimulatorBrakeMessage sim = captureSimulatorMessage();
        assertFalse(sim.brakeActive);
        assertTrue(sim.preBrake);
    }

    @Test
    void condition1TriggeredAtThreshold() {
        service.processDistance("VIN-1", 25.0, 5.0);

        BrakeMessage msg = captureBreakMessage();
        assertTrue(msg.active);
        assertEquals(1, msg.conditionTriggered);
    }

    @Test
    void condition1NotTriggeredWhenRateTooLow() {
        service.processDistance("VIN-1", 25.0, 3.9);

        BrakeMessage msg = captureBreakMessage();
        assertFalse(msg.active);
    }

    @Test
    void condition2TriggeredAtThreshold() {
        service.processDistance("VIN-1", 10.0, 2.5);

        BrakeMessage msg = captureBreakMessage();
        assertTrue(msg.active);
        assertEquals(2, msg.conditionTriggered);
    }

    @Test
    void condition2NotTriggeredWhenRateTooLow() {
        service.processDistance("VIN-1", 10.0, 1.9);

        BrakeMessage msg = captureBreakMessage();
        assertFalse(msg.active);
    }

    @Test
    void condition3TriggeredWhenVeryCloseAndApproaching() {
        service.processDistance("VIN-1", 3.0, 0.1);

        BrakeMessage msg = captureBreakMessage();
        assertTrue(msg.active);
        assertEquals(3, msg.conditionTriggered);
    }

    @Test
    void condition3NotTriggeredWhenReceding() {
        service.processDistance("VIN-1", 3.0, -0.5);

        BrakeMessage msg = captureBreakMessage();
        assertFalse(msg.active);
    }

    @Test
    void mostSevereConditionSelectedWhenMultipleApply() {
        // dist=3m qualifies for condition 3 even though also within condition 2 range
        service.processDistance("VIN-1", 3.0, 3.0);

        BrakeMessage msg = captureBreakMessage();
        assertEquals(3, msg.conditionTriggered);
    }

    // Pre-emergency at 45 m

    @Test
    void preEmergencyExitsWhenDistanceReaches45() {
        service.processDistance("VIN-1", 40.0, 0.0);
        reset(brakePublisher, simulatorPublisher);

        service.processDistance("VIN-1", 45.0, 0.0);

        BrakeMessage msg = captureBreakMessage();
        assertFalse(msg.preEmergencyBrake);
        assertFalse(msg.active);
    }

    @Test
    void sameStateNotPublishedTwice() {
        service.processDistance("VIN-1", 100.0, 0.0);
        service.processDistance("VIN-1", 100.0, 0.0);

        verify(brakePublisher, times(1)).publish(any());
    }

    @Test
    void publishedAgainWhenStateChanges() {
        service.processDistance("VIN-1", 100.0, 0.0);
        service.processDistance("VIN-1", 40.0, 0.0);  // pre-emergency

        verify(brakePublisher, times(2)).publish(any());
    }

    @Test
    void publishedMessageHasBrakeNowSource() {
        service.processDistance("VIN-1", 3.0, 1.0);

        BrakeMessage msg = captureBreakMessage();
        assertEquals(BrakeMessage.Source.BRAKENOW, msg.source);
    }

    // Hold logic for brake
    @Test
    void emergencyBrakeHeldWhileDistanceBelowResumeThreshold() {
        service.processDistance("VIN-1", 3.0, 1.0); // emergency brake
        reset(brakePublisher, simulatorPublisher);

        service.processDistance("VIN-1", 100.0, -1.0);

        verifyNoInteractions(brakePublisher, simulatorPublisher);
    }

    @Test
    void emergencyBrakeReleasedWhenDistanceExceedsResumeThreshold() {
        service.processDistance("VIN-1", 3.0, 1.0); // emergency brake
        reset(brakePublisher, simulatorPublisher);

        service.processDistance("VIN-1", 155.0, -1.0);  // > 150m safe to resume

        BrakeMessage msg = captureBreakMessage();
        assertFalse(msg.active);

        SimulatorBrakeMessage sim = captureSimulatorMessage();
        assertFalse(sim.brakeActive);
    }

    @Test
    void holdDoesNotBlockSecondEmergencyBrakeAfterFullCycle() {
        service.processDistance("VIN-1", 3.0, 1.0);  // 1st emergency
        service.processDistance("VIN-1", 155.0, -1.0); // resume
        reset(brakePublisher, simulatorPublisher);

        service.processDistance("VIN-1", 3.0, 1.0); // 2nd emergency

        BrakeMessage msg = captureBreakMessage();
        assertTrue(msg.active);
    }

    @Test
    void resetStateBypassesHoldOnNextReading() {
        service.processDistance("VIN-1", 3.0, 1.0); // emergency brake
        service.resetState();
        reset(brakePublisher, simulatorPublisher);

        service.processDistance("VIN-1", 80.0, 1.38);  // normal distance after reset

        BrakeMessage msg = captureBreakMessage();
        assertFalse(msg.active);
        assertFalse(msg.preEmergencyBrake);
    }

    @Test
    void resetStateClearsGetState() {
        service.processDistance("VIN-1", 3.0, 1.0);
        assertNotNull(service.getState("VIN-1"));

        service.resetState();

        assertNull(service.getState("VIN-1"));
        assertTrue(service.getAllStates().isEmpty());
    }

    @Test
    void processBrakeForwardsActiveToSimulator() {
        service.processBrake("VIN-1", true, Instant.now());

        SimulatorBrakeMessage sim = captureSimulatorMessage();
        assertTrue(sim.brakeActive);
        assertFalse(sim.preBrake);
        assertEquals("VIN-1", sim.vin);
    }

    @Test
    void processBrakeForwardsClearToSimulator() {
        service.processBrake("VIN-1", false, Instant.now());

        SimulatorBrakeMessage sim = captureSimulatorMessage();
        assertFalse(sim.brakeActive);
        assertFalse(sim.preBrake);
    }

    @Test
    void processBrakeDoesNotUpdateInternalState() {
        service.processBrake("VIN-1", true, Instant.now());

        assertNull(service.getState("VIN-1"));
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
