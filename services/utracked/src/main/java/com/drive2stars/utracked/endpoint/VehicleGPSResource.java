package com.drive2stars.utracked.endpoint;

import com.drive2stars.utracked.service.VehicleService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/positions")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class VehicleGPSResource {

  @Inject
  VehicleService vehicleService;

  @GET
  @Path("/{vin}")
  public VehicleGPSDto getLatestPosition(@PathParam("vin") String vin) {
    VehicleGPSDto dto = vehicleService.getLatestPosition(vin);
    if (dto == null) {
      throw new NotFoundException("No position found for VIN: " + vin);
    }
    return dto;
  }
}
