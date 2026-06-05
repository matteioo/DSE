package com.drive2stars.orchestrator.endpoint;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

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
