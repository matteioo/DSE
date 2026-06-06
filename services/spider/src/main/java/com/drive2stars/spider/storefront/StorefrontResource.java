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

@Path("/api/storefront")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class StorefrontResource {

    OrchestratorGrpcAdapter orchestratorAdapter;
    StorefrontMockService mockService;

    // TODO: replace mockService with real adapters
    //   Sonar      — distance to the vehicle in front and back, distance change rate
    public StorefrontResource(OrchestratorGrpcAdapter orchestratorAdapter, StorefrontMockService mockService) {
        this.orchestratorAdapter = orchestratorAdapter;
        this.mockService = mockService;
    }

    @GET
    @Path("/vehicles")
    @Blocking
    public List<VehicleStateDto> getVehicles() {
        return mockService.getVehicleStates();
    }

    @GET
    @Path("/vehicles/{vin}")
    @Blocking
    @Operation(summary = "Single vehicle state by VIN")
    public VehicleStateDto getVehicle(@PathParam("vin") String vin) {
        VehicleStateDto dto = mockService.getVehicleState(vin);
        if (dto == null) {
            throw new NotFoundException("Vehicle not found: " + vin);
        }
        return dto;
    }

    @GET
    @Path("/events")
    @Blocking
    public List<EventLogEntryDto> getEvents() {
        return orchestratorAdapter.getRecentEvents(100);
    }
}
