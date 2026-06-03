package com.drive2stars.whereami.endpoint;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "simulator-api")
public interface SimulatorGpsClient {

  @GET
  @Path("/vehicles/{vin}/gps")
  GpsReadingDto getGps(@PathParam("vin") String vin);
}
