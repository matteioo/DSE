package com.drive2stars.spider.adapter.orchestrator;

import com.drive2stars.grpc.orchestrator.GetEventsRequest;
import com.drive2stars.grpc.orchestrator.GetEventsResponse;
import com.drive2stars.grpc.orchestrator.OrchestratorServiceGrpc;
import com.drive2stars.spider.storefront.dto.EventLogEntryDto;
import io.quarkus.grpc.GrpcClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.Fallback;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class OrchestratorGrpcAdapter {

    @GrpcClient("orchestrator")
    OrchestratorServiceGrpc.OrchestratorServiceBlockingStub stub;

    @Fallback(fallbackMethod = "getRecentEventsFallback")
    public List<EventLogEntryDto> getRecentEvents(int limit) {
        GetEventsResponse response = stub.getEvents(GetEventsRequest.newBuilder().setLimit(limit).build());

        return response.getEventsList().stream()
                .map(e -> {
                    EventLogEntryDto eventLogEntryDto = new EventLogEntryDto();
                    eventLogEntryDto.timestamp = Instant.parse(e.getTimestamp());
                    eventLogEntryDto.source = "ORCHESTRATOR";
                    eventLogEntryDto.level = e.getEventType().contains("EMERGENCY") ? "ERROR" : "WARN";
                    eventLogEntryDto.message = formatMessage(e.getEventType(), e.getVin(), e.getTriggerCondition());
                    return eventLogEntryDto;
                }).toList();
    }

    List<EventLogEntryDto> getRecentEventsFallback(int limit) {
        return List.of();
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
