package com.drive2stars.brakenow.mq;

import com.drive2stars.brakenow.service.BrakeService;
import com.drive2stars.shared.messaging.SimulationScenarioCommand;
import com.drive2stars.shared.messaging.SimulationScenarioCommandParser;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ScenarioConsumer {

    private static final Logger LOG = Logger.getLogger(ScenarioConsumer.class);

    @Inject
    BrakeService brakeService;

    @Incoming("scenario-command")
    @Blocking
    public void process(byte[] raw) {
        String payload = new String(raw, StandardCharsets.UTF_8).trim();
        SimulationScenarioCommand command;
        try {
            command = SimulationScenarioCommandParser.parse(payload);
        } catch (IllegalArgumentException e) {
            LOG.errorf("Dropping malformed scenario command: %s", e.getMessage());
            return;
        }

        if (command.scenario == SimulationScenarioCommand.Scenario.RESET
                || command.scenario == SimulationScenarioCommand.Scenario.IDLE) {
            brakeService.resetState();
        }
    }
}
