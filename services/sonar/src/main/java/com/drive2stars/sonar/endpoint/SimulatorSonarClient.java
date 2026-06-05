package com.drive2stars.sonar.endpoint;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "simulator-api")
public interface SimulatorSonarClient {

  @GET
  @Path("/vehicles/{vin}/sonar")
  List<SonarSensorReadingDto> getVehicleReadings(@PathParam("vin") String vin);

  @GET
  @Path("/sonar/readings")
  List<SonarSensorReadingDto> getAllReadings();
}
