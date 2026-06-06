package com.drive2stars.brakenow.endpoint;

import com.drive2stars.brakenow.service.BrakeService;
import com.drive2stars.brakenow.service.BrakeState;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/brake")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class BrakeStatusResource {

    @Inject
    BrakeService brakeService;

    @GET
    @Path("/status")
    public List<BrakeStatusDto> getAllStatus() {
        return brakeService.getAllStates().stream()
            .map(BrakeStatusDto::from)
            .toList();
    }

    @GET
    @Path("/status/{vin}")
    public BrakeStatusDto getStatus(@PathParam("vin") String vin) {
        BrakeState state = brakeService.getState(vin);
        if (state == null) throw new NotFoundException("No brake data for VIN: " + vin);
        return BrakeStatusDto.from(state);
    }
}
