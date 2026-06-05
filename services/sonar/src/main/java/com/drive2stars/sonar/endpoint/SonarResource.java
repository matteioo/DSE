package com.drive2stars.sonar.endpoint;

import com.drive2stars.sonar.service.SonarPollingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/sonar")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class SonarResource {

  @Inject
  SonarPollingService sonarPollingService;

  @GET
  @Path("/readings/{vin}")
  public List<SonarReadingDto> getReadings(@PathParam("vin") String vin) {
    return sonarPollingService.readingsForVin(vin);
  }
}
