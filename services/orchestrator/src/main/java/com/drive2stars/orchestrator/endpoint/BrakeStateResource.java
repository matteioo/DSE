package com.drive2stars.orchestrator.endpoint;

import com.drive2stars.orchestrator.service.BrakeStateService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.List;

@Path("/api/brake")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class BrakeStateResource {

    @RegisterForReflection
    public static class BrakeStateDto {
        public String vin;
        public boolean emergencyBrakeActive;
        public boolean preEmergencyBrakeActive;
        public Instant updatedAt;
    }

    @Inject
    BrakeStateService brakeStateService;

    @GET
    @Path("/state")
    public List<BrakeStateDto> getAllStates() {
        return brakeStateService.getAllStates().stream()
                .map(s -> {
                    BrakeStateDto dto = new BrakeStateDto();
                    dto.vin = s.vin;
                    dto.emergencyBrakeActive = s.emergencyBrakeActive;
                    dto.preEmergencyBrakeActive = s.preEmergencyBrakeActive;
                    dto.updatedAt = s.updatedAt;
                    return dto;
                })
                .toList();
    }
}
