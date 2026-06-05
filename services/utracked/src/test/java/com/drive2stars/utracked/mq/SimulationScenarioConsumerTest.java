package com.drive2stars.utracked.mq;

import com.drive2stars.utracked.service.SimulationResetService;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulationScenarioConsumerTest {

  @Test
  void resetCommandClearsPositions() {
    RecordingResetService resetService = new RecordingResetService();
    SimulationScenarioConsumer consumer = consumer(resetService);

    consumer.process("RESET".getBytes(StandardCharsets.UTF_8));

    assertEquals(1, resetService.clearCalls);
  }

  @Test
  void nonResetCommandDoesNotClearPositions() {
    RecordingResetService resetService = new RecordingResetService();
    SimulationScenarioConsumer consumer = consumer(resetService);

    consumer.process("SCENARIO_1".getBytes(StandardCharsets.UTF_8));

    assertEquals(0, resetService.clearCalls);
  }

  private static SimulationScenarioConsumer consumer(RecordingResetService resetService) {
    SimulationScenarioConsumer consumer = new SimulationScenarioConsumer();
    consumer.simulationResetService = resetService;
    return consumer;
  }

  private static class RecordingResetService extends SimulationResetService {
    int clearCalls;

    @Override
    public long clearPositions() {
      clearCalls++;
      return 2;
    }
  }
}
