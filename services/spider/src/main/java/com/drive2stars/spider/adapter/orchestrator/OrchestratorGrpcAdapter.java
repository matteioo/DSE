package com.drive2stars.spider.adapter.orchestrator;

import com.drive2stars.grpc.orchestrator.BrakeEvent;
import com.drive2stars.grpc.orchestrator.GetEventsRequest;
import com.drive2stars.grpc.orchestrator.GetEventsResponse;
import com.drive2stars.grpc.orchestrator.OrchestratorServiceGrpc;
import com.drive2stars.spider.storefront.dto.EventLogEntryDto;
import io.quarkus.grpc.GrpcClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class OrchestratorGrpcAdapter {

    private static final Logger LOG = Logger.getLogger(OrchestratorGrpcAdapter.class);

    @GrpcClient("orchestrator")
    OrchestratorServiceGrpc.OrchestratorServiceBlockingStub stub;

    @Fallback(fallbackMethod = "getRecentEventsFallback")
    public List<EventLogEntryDto> getRecentEvents(int limit) {
        GetEventsResponse response = stub.withDeadlineAfter(5, TimeUnit.SECONDS)
                .getEvents(GetEventsRequest.newBuilder().setLimit(limit).build());

        return response.getEventsList().stream()
                .map(this::toDto)
                .toList();
    }

    List<EventLogEntryDto> getRecentEventsFallback(int limit, Throwable cause) {
        LOG.warnf("Falling back to empty event log: %s", cause.getMessage());
        return List.of();
    }

    private EventLogEntryDto toDto(BrakeEvent e) {
        EventLogEntryDto dto = new EventLogEntryDto();
        dto.timestamp = Instant.parse(e.getTimestamp());
        dto.source = e.getTriggeredBy();
        dto.level = logEntryLevel(e.getEventType());
        dto.message = formatMessage(e.getEventType(), e.getVin(), e.getTriggerCondition());
        return dto;
    }

    private String logEntryLevel(String level) {
        return switch (level) {
            case "PRE_EMERGENCY_REPORTED" -> "WARNING";
            default -> "ERROR";
        };
    }

    private String formatMessage(String eventType, String vin, int condition) {
        return switch (eventType) {
            case "EMERGENCY_BRAKE_SENT" -> String.format("Sent emergency brake command to VIN %s (triggered by condition %d)", vin, condition);
            case "PRE_EMERGENCY_REPORTED" -> String.format("Received pre-emergency report from VIN %s (triggered by condition %d)", vin, condition);
            case "LOCAL_BRAKE_REPORTED" -> String.format("Received local brake report from VIN %s (triggered by condition %d)", vin, condition);
            default -> String.format("Received unknown event type '%s' from VIN %s (triggered by condition %d)", eventType, vin, condition);
        };
    }
}
