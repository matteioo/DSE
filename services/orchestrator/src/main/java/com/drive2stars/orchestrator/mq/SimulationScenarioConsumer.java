package com.drive2stars.orchestrator.mq;

import com.drive2stars.orchestrator.service.EventService;
import com.drive2stars.orchestrator.service.SimulationResetStateService;
import com.drive2stars.shared.messaging.SimulationScenarioCommand;
import com.drive2stars.shared.messaging.SimulationScenarioCommandParser;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SimulationScenarioConsumer {

  private static final Logger LOG = Logger.getLogger(SimulationScenarioConsumer.class);

  private final EventService eventService;
  private final SimulationResetStateService simulationResetStateService;

  public SimulationScenarioConsumer(EventService eventService,
      SimulationResetStateService simulationResetStateService) {
    this.eventService = eventService;
    this.simulationResetStateService = simulationResetStateService;
  }

  @Incoming("scenario-command")
  @Blocking
  public void process(byte[] raw) {
    SimulationScenarioCommand command = decode(raw);
    if (command == null || command.scenario != SimulationScenarioCommand.Scenario.RESET) {
      return;
    }

    long deleted = eventService.clearEvents();
    simulationResetStateService.recordReset();
    LOG.infof("Cleared %d ORCHESTRATOR events after simulation reset", deleted);
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
