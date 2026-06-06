package com.drive2stars.utracked.grpc;

import com.drive2stars.grpc.utracked.*;
import com.drive2stars.utracked.endpoint.VehicleGPSDto;
import com.drive2stars.utracked.service.VehicleService;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;

import java.util.List;

@GrpcService
public class UtrackedGrpcService extends UtrackedServiceGrpc.UtrackedServiceImplBase {

    VehicleService vehicleService;

    public UtrackedGrpcService(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @Override
    @Blocking
    public void getVehiclePosition(GetVehiclePositionRequest request, StreamObserver<GetVehiclePositionResponse> responseObserver) {
        VehicleGPSDto position = vehicleService.getLatestPosition(request.getVin());

        VehiclePosition grpcPosition = VehiclePosition.newBuilder()
                .setVin(position.vin)
                .setLatitude(position.latitude.doubleValue())
                .setLongitude(position.longitude.doubleValue())
                .build();

        responseObserver.onNext(GetVehiclePositionResponse.newBuilder().setVehiclePosition(grpcPosition).build());
        responseObserver.onCompleted();
    }

    @Override
    @Blocking
    public void getVehiclePositions(GetVehiclePositionsRequest request, StreamObserver<GetVehiclePositionsResponse> responseObserver) {
        List<VehicleGPSDto> positions = vehicleService.getLatestPositions();

        List<VehiclePosition> grpcPositions = positions.stream()
                .map(p -> VehiclePosition.newBuilder()
                        .setVin(p.vin)
                        .setLatitude(p.latitude.doubleValue())
                        .setLongitude(p.longitude.doubleValue())
                        .build())
                .toList();

        responseObserver.onNext(GetVehiclePositionsResponse.newBuilder().addAllVehiclePositions(grpcPositions).build());
        responseObserver.onCompleted();
    }
}
