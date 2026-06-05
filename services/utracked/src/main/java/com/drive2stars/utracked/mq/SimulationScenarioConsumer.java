package com.drive2stars.utracked.mq;

import com.drive2stars.shared.messaging.SimulationScenarioCommand;
import com.drive2stars.shared.messaging.SimulationScenarioCommandParser;
import com.drive2stars.utracked.service.SimulationResetService;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SimulationScenarioConsumer {

  private static final Logger LOG = Logger.getLogger(SimulationScenarioConsumer.class);

  @Inject
  SimulationResetService simulationResetService;

  @Incoming("scenario-command")
  @Blocking
  public void process(byte[] raw) {
    SimulationScenarioCommand command = decode(raw);
    if (command == null || command.scenario != SimulationScenarioCommand.Scenario.RESET) {
      return;
    }

    long deleted = simulationResetService.clearPositions();
    LOG.infof("Cleared %d UTRACKED GPS positions after simulation reset", deleted);
  }

  private SimulationScenarioCommand decode(byte[] raw) {
    String payload = new String(raw, StandardCharsets.UTF_8).trim();
    try {
      return SimulationScenarioCommandParser.parse(payload);
    } catch (IllegalArgumentException e) {
      LOG.errorf("Dropping malformed scenario command %s: %s", payload, e.getMessage());
      return null;
    }
  }
}
