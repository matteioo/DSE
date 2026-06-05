package com.drive2stars.simulator.endpoint;

import com.drive2stars.simulator.service.VehicleSimulationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/sonar")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class SonarResource {

  @Inject
  VehicleSimulationService vehicleSimulationService;

  @GET
  @Path("/readings")
  public List<SonarSensorReadingDto> getReadings() {
    return vehicleSimulationService.getAllSonarReadings();
  }
}
