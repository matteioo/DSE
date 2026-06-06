package com.drive2stars.sonar.grpc;

import com.drive2stars.grpc.sonar.GetLatestReadingsRequest;
import com.drive2stars.grpc.sonar.GetLatestReadingsResponse;
import com.drive2stars.grpc.sonar.SonarReading;
import com.drive2stars.grpc.sonar.SonarServiceGrpc;
import com.drive2stars.sonar.endpoint.SonarReadingDto;
import com.drive2stars.sonar.service.SonarPollingService;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;
import java.util.List;

@GrpcService
public class SonarGrpcService extends SonarServiceGrpc.SonarServiceImplBase {

    @Inject
    SonarPollingService sonarPollingService;

    @Override
    public void getLatestReadings(GetLatestReadingsRequest request,
                                   StreamObserver<GetLatestReadingsResponse> responseObserver) {
        List<SonarReading> readings = sonarPollingService.getAllLatestReadings().stream()
                .map(r -> SonarReading.newBuilder()
                        .setVin(r.vin)
                        .setDirection(r.direction)
                        .setDistanceMeters(r.distanceMeters.doubleValue())
                        .setDistanceChangeMps(r.distanceChangeMetersPerSecond.doubleValue())
                        .setMeasuredAt(r.measuredAt != null ? r.measuredAt.toString() : "")
                        .build())
                .toList();

        responseObserver.onNext(GetLatestReadingsResponse.newBuilder()
                .addAllReadings(readings)
                .build());
        responseObserver.onCompleted();
    }
}
