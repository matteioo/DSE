package com.drive2stars.spider.adapter.utracked;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import java.util.List;

@RegisterRestClient(configKey = "utracked")
@Path("/positions")
public interface UtrackedRestClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<UtrackedPositionDto> getLatestPositions();

    @GET
    @Path("/{vin}")
    @Produces(MediaType.APPLICATION_JSON)
    UtrackedPositionDto getLatestPosition(@PathParam("vin") String vin);
}
