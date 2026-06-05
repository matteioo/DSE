package com.drive2stars.orchestrator.mq;

import com.drive2stars.orchestrator.service.EventService;
import com.drive2stars.orchestrator.service.SimulationResetStateService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SimulationScenarioConsumerTest {

  @Test
  void resetCommandClearsEventsAndRecordsReset() {
    EventService eventService = mock(EventService.class);
    SimulationResetStateService resetStateService = mock(SimulationResetStateService.class);
    when(eventService.clearEvents()).thenReturn(3L);
    SimulationScenarioConsumer consumer = new SimulationScenarioConsumer(eventService, resetStateService);

    consumer.process("RESET".getBytes(StandardCharsets.UTF_8));

    verify(eventService).clearEvents();
    verify(resetStateService).recordReset();
  }

  @Test
  void nonResetCommandDoesNotClearEvents() {
    EventService eventService = mock(EventService.class);
    SimulationResetStateService resetStateService = mock(SimulationResetStateService.class);
    SimulationScenarioConsumer consumer = new SimulationScenarioConsumer(eventService, resetStateService);

    consumer.process("SCENARIO_1".getBytes(StandardCharsets.UTF_8));

    verify(eventService, never()).clearEvents();
    verify(resetStateService, never()).recordReset();
  }
}
