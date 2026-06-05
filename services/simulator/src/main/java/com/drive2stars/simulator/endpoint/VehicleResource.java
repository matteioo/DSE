package com.drive2stars.simulator.endpoint;

import com.drive2stars.simulator.service.VehicleSimulationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/vehicles")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class VehicleResource {

  @Inject
  VehicleSimulationService vehicleSimulationService;

  @GET
  public List<VehicleStateDto> getVehicles() {
    return vehicleSimulationService.getVehicles();
  }

  @GET
  @Path("/{vin}/gps")
  public VehicleGpsDto getGps(@PathParam("vin") String vin) {
    return vehicleSimulationService.getGps(vin);
  }

  @GET
  @Path("/{vin}/sonar")
  public List<SonarSensorReadingDto> getSonar(@PathParam("vin") String vin) {
    return vehicleSimulationService.getSonarReadings(vin);
  }
}
