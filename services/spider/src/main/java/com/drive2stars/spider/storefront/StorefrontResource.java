package com.drive2stars.spider.storefront;

import com.drive2stars.spider.adapter.orchestrator.OrchestratorGrpcAdapter;
import com.drive2stars.spider.storefront.dto.EventLogEntryDto;
import com.drive2stars.spider.storefront.dto.VehicleStateDto;
import com.drive2stars.spider.storefront.StorefrontService;
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
    StorefrontService storefrontService;

    public StorefrontResource(OrchestratorGrpcAdapter orchestratorAdapter, StorefrontService storefrontService) {
        this.orchestratorAdapter = orchestratorAdapter;
        this.storefrontService = storefrontService;
    }

    @GET
    @Path("/vehicles")
    @Blocking
    public List<VehicleStateDto> getVehicles() {
        return storefrontService.getVehicleStates();
    }

    @GET
    @Path("/vehicles/{vin}")
    @Blocking
    @Operation(summary = "Single vehicle state by VIN")
    public VehicleStateDto getVehicle(@PathParam("vin") String vin) {
        VehicleStateDto dto = storefrontService.getVehicleState(vin);
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
