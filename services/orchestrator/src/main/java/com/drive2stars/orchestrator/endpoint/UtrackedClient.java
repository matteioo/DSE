package com.drive2stars.orchestrator.endpoint;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.temporal.ChronoUnit;
import java.util.List;

@RegisterRestClient(configKey = "utracked-api")
@Path("/positions")
public interface UtrackedClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @CircuitBreaker(requestVolumeThreshold = 5, delay = 10, delayUnit = ChronoUnit.SECONDS)
    List<VehiclePositionDto> getAllVehiclePositions();

    @GET
    @Path("/{vin}/history")
    @Produces(MediaType.APPLICATION_JSON)
    @CircuitBreaker(requestVolumeThreshold = 5, delay = 10, delayUnit = ChronoUnit.SECONDS)
    List<VehiclePositionDto> getHistory(@PathParam("vin") String vin,
                                        @QueryParam("limit") @DefaultValue("2") int limit);
}
