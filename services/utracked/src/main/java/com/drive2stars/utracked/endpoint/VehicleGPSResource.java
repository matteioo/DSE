package com.drive2stars.utracked.endpoint;

import com.drive2stars.utracked.service.VehicleService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/positions")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class VehicleGPSResource {

  @Inject
  VehicleService vehicleService;

  @GET
  public List<VehicleGPSDto> getLatestPositions() {
    return vehicleService.getLatestPositions();
  }

  @GET
  @Path("/{vin}")
  public VehicleGPSDto getLatestPosition(@PathParam("vin") String vin) {
    VehicleGPSDto dto = vehicleService.getLatestPosition(vin);
    if (dto == null) {
      throw new NotFoundException("No position found for VIN: " + vin);
    }
    return dto;
  }

  @GET
  @Path("/{vin}/history")
  public List<VehicleGPSDto> getLatestPosition(@PathParam("vin") String vin, @QueryParam("limit") @DefaultValue("20") int limit) {
    return vehicleService.getHistory(vin, limit);
  }
}
