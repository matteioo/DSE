package com.drive2stars.spider.storefront;

import com.drive2stars.spider.adapter.orchestrator.OrchestratorGrpcAdapter;
import com.drive2stars.spider.storefront.dto.EventLogEntryDto;
import com.drive2stars.spider.storefront.dto.VehicleStateDto;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/storefront")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Storefront", description = "API Gateway endpoints consumed by the Storefront web UI. Aggregates data from UTRACKED, SONAR, and ORCHESTRATOR via gRPC.")
public class StorefrontResource {

    OrchestratorGrpcAdapter orchestratorAdapter;
    StorefrontService storefrontService;

    public StorefrontResource(OrchestratorGrpcAdapter orchestratorAdapter, StorefrontService storefrontService) {
        this.orchestratorAdapter = orchestratorAdapter;
        this.storefrontService = storefrontService;
    }

    @GET
    @Path("/vehicles")
    @Blocking
    @Operation(
        summary = "All vehicle states",
        description = "Returns the aggregated real-time state of every tracked vehicle: GPS position (UTRACKED), " +
                      "front/rear distances and change rate (SONAR), brake and pre-emergency state (ORCHESTRATOR). " +
                      "Speed is estimated from consecutive GPS updates using the haversine formula."
    )
    @APIResponse(responseCode = "200", description = "List of vehicle states (may be empty if no vehicles are known yet)",
        content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = VehicleStateDto.class)))
    public List<VehicleStateDto> getVehicles() {
        return storefrontService.getVehicleStates();
    }

    @GET
    @Path("/vehicles/{vin}")
    @Blocking
    @Operation(
        summary = "Single vehicle state by VIN",
        description = "Returns the full aggregated state for one vehicle identified by its Vehicle Identification Number (VIN)."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Vehicle state found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = VehicleStateDto.class))),
        @APIResponse(responseCode = "404", description = "No vehicle with this VIN is currently tracked")
    })
    public VehicleStateDto getVehicle(
        @Parameter(description = "Vehicle Identification Number", example = "D2S-DEMO-VIN-001", required = true)
        @PathParam("vin") String vin) {
        VehicleStateDto dto = storefrontService.getVehicleState(vin);
        if (dto == null) {
            throw new NotFoundException("Vehicle not found: " + vin);
        }
        return dto;
    }

    @GET
    @Path("/events")
    @Blocking
    @Operation(
        summary = "Recent brake event log",
        description = "Returns the 100 most recent events persisted by the ORCHESTRATOR service, " +
                      "ordered newest-first. Events include emergency brake triggers (conditions 1–4) " +
                      "and pre-emergency brake state transitions."
    )
    @APIResponse(responseCode = "200", description = "List of event log entries, newest first",
        content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = EventLogEntryDto.class)))
    public List<EventLogEntryDto> getEvents() {
        return orchestratorAdapter.getRecentEvents(100);
    }
}
