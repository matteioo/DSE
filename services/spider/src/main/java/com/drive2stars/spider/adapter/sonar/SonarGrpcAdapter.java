package com.drive2stars.spider.adapter.sonar;

import com.drive2stars.grpc.sonar.GetLatestReadingsRequest;
import com.drive2stars.grpc.sonar.GetLatestReadingsResponse;
import com.drive2stars.grpc.sonar.SonarServiceGrpc;
import io.quarkus.grpc.GrpcClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class SonarGrpcAdapter {

    private static final Logger LOG = Logger.getLogger(SonarGrpcAdapter.class);

    @GrpcClient("sonar")
    SonarServiceGrpc.SonarServiceBlockingStub stub;

    public record SonarReadingDto(String vin, String direction, double distanceMeters, double distanceChangeMps) {}

    @Fallback(fallbackMethod = "getLatestReadingsFallback")
    public List<SonarReadingDto> getLatestReadings() {
        GetLatestReadingsResponse response = stub.withDeadlineAfter(5, TimeUnit.SECONDS)
                .getLatestReadings(GetLatestReadingsRequest.newBuilder().build());

        return response.getReadingsList().stream()
                .map(r -> new SonarReadingDto(r.getVin(), r.getDirection(),
                        r.getDistanceMeters(), r.getDistanceChangeMps()))
                .toList();
    }

    public List<SonarReadingDto> getLatestReadingsFallback(Throwable cause) {
        LOG.warnf("Falling back to empty sonar readings: %s", cause.getMessage());
        return List.of();
    }
}
