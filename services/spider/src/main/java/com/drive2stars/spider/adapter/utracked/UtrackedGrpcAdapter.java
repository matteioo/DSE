package com.drive2stars.spider.adapter.utracked;

import com.drive2stars.grpc.utracked.GetVehiclePositionsRequest;
import com.drive2stars.grpc.utracked.GetVehiclePositionsResponse;
import com.drive2stars.grpc.utracked.UtrackedServiceGrpc;
import com.drive2stars.grpc.utracked.VehiclePosition;
import com.drive2stars.spider.storefront.dto.VehiclePositionDto;
import io.quarkus.grpc.GrpcClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class UtrackedGrpcAdapter {

    private static final Logger LOG = Logger.getLogger(UtrackedGrpcAdapter.class);

    @GrpcClient("utracked")
    UtrackedServiceGrpc.UtrackedServiceBlockingStub stub;

    @Fallback(fallbackMethod = "getLatestPositionsFallback")
    public List<VehiclePositionDto> getLatestPositions() {
        GetVehiclePositionsResponse positions = stub.withDeadlineAfter(5, TimeUnit.SECONDS)
                .getVehiclePositions(GetVehiclePositionsRequest.newBuilder().build());

        return positions.getVehiclePositionsList().stream()
                .map(this::toDto)
                .toList();
    }

    public List<VehiclePositionDto> getLatestPositionsFallback(Throwable cause) {
        LOG.warnf("Falling back to empty positions: %s", cause.getMessage());
        return List.of();
    }

    private VehiclePositionDto toDto(VehiclePosition pos) {
        VehiclePositionDto dto = new VehiclePositionDto();
        dto.vin = pos.getVin();
        dto.latitude = pos.getLatitude();
        dto.longitude = pos.getLongitude();
        return dto;
    }
}
