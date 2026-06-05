package com.drive2stars.shared.messaging;

import java.time.Instant;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SimulationScenarioCommandParser {

    private static final Pattern SCENARIO_FIELD =
            Pattern.compile("\"(?:scenario|command)\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

    private SimulationScenarioCommandParser() {}

    public static SimulationScenarioCommand parse(String payload) {
        return new SimulationScenarioCommand(parseScenario(extractScenario(payload)), Instant.now());
    }

    public static SimulationScenarioCommand.Scenario parseScenario(String value) {
        if (value == null) {
            throw new IllegalArgumentException("missing scenario");
        }
        String normalized = value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "1", "SCENARIO1", "SCENARIO_1" -> SimulationScenarioCommand.Scenario.SCENARIO_1;
            case "2", "SCENARIO2", "SCENARIO_2" -> SimulationScenarioCommand.Scenario.SCENARIO_2;
            case "3", "SCENARIO3", "SCENARIO_3" -> SimulationScenarioCommand.Scenario.SCENARIO_3;
            case "IDLE", "IDEL" -> SimulationScenarioCommand.Scenario.IDLE;
            case "RESET" -> SimulationScenarioCommand.Scenario.RESET;
            default -> SimulationScenarioCommand.Scenario.valueOf(normalized);
        };
    }

    private static String extractScenario(String payload) {
        if (payload == null) {
            throw new IllegalArgumentException("missing payload");
        }
        String trimmed = payload.trim();
        if (!trimmed.startsWith("{")) {
            return trimmed;
        }
        Matcher matcher = SCENARIO_FIELD.matcher(trimmed);
        if (!matcher.find()) {
            throw new IllegalArgumentException("missing scenario field");
        }
        return matcher.group(1);
    }
}
