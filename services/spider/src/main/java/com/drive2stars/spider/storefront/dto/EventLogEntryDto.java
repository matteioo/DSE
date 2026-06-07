package com.drive2stars.spider.storefront.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@RegisterForReflection
@Schema(description = "A single entry in the brake event log, persisted by the ORCHESTRATOR service")
public class EventLogEntryDto {

    @Schema(description = "UTC timestamp when the event was recorded by the ORCHESTRATOR")
    public Instant timestamp;

    @Schema(description = "Severity level of the event", example = "WARN", enumeration = {"INFO", "WARN", "ERROR"})
    public String level;

    @Schema(description = "VIN of the vehicle that triggered the event, or the evaluating service name", example = "D2S-DEMO-VIN-002")
    public String source;

    @Schema(description = "Human-readable description of the event, including the triggering condition", example = "Emergency brake triggered: distance=28m, changeRate=-4.5m/s (Condition 1)")
    public String message;
}
