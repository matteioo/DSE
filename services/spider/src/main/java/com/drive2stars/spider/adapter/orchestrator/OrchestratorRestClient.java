package com.drive2stars.spider.adapter.orchestrator;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import java.util.List;

@RegisterRestClient(configKey = "orchestrator")
@Path("/api/brake")
public interface OrchestratorRestClient {

    @GET
    @Path("/state")
    @Produces(MediaType.APPLICATION_JSON)
    List<OrchestratorBrakeStateDto> getBrakeStates();
}
