package com.drive2stars.orchestrator.endpoint;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "utracked-api")
@Path("/positions")
public interface UtrackedClient {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  List<VehiclePositionDto> getAllVehiclePositions();

  @GET
  @Path("/{vin}")
  @Produces(MediaType.APPLICATION_JSON)
  VehiclePositionDto getLatestVehiclePosition(@PathParam("vin") String vin);

  @GET
  @Path("/{vin}/history")
  @Produces(MediaType.APPLICATION_JSON)
  List<VehiclePositionDto> getHistory(@PathParam("vin") String vin,
                                      @QueryParam("limit") @DefaultValue("2") int limit);
}
