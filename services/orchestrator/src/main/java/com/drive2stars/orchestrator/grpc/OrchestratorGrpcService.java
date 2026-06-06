package com.drive2stars.orchestrator.grpc;

import com.drive2stars.grpc.orchestrator.BrakeEvent;
import com.drive2stars.grpc.orchestrator.GetBrakeStatesRequest;
import com.drive2stars.grpc.orchestrator.GetBrakeStatesResponse;
import com.drive2stars.grpc.orchestrator.GetEventsRequest;
import com.drive2stars.grpc.orchestrator.GetEventsResponse;
import com.drive2stars.grpc.orchestrator.OrchestratorServiceGrpc;
import com.drive2stars.grpc.orchestrator.VehicleBrakeState;
import com.drive2stars.orchestrator.persistence.EventEntity;
import com.drive2stars.orchestrator.service.BrakeStateService;
import com.drive2stars.orchestrator.service.EventService;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;

import java.util.List;

@GrpcService
public class OrchestratorGrpcService extends OrchestratorServiceGrpc.OrchestratorServiceImplBase {

    EventService eventService;
    BrakeStateService brakeStateService;

    public OrchestratorGrpcService(EventService eventService, BrakeStateService brakeStateService) {
        this.eventService = eventService;
        this.brakeStateService = brakeStateService;
    }

    @Override
    @Blocking
    public void getEvents(GetEventsRequest request, StreamObserver<GetEventsResponse> responseObserver) {
        List<EventEntity> entities = eventService.getRecentEvents(request.getLimit());

        List<BrakeEvent> events = entities.stream()
            .map(e -> BrakeEvent.newBuilder()
                .setVin(e.vin)
                .setTimestamp(e.timestamp.toString())
                .setEventType(e.eventType.name())
                .setTriggerCondition(e.triggerCondition != null ? e.triggerCondition : 0)
                .setTriggeredBy(e.triggeredBy.name())
                .build())
            .toList();

        responseObserver.onNext(GetEventsResponse.newBuilder().addAllEvents(events).build());
        responseObserver.onCompleted();
    }

    @Override
    @Blocking
    public void getBrakeStates(GetBrakeStatesRequest request, StreamObserver<GetBrakeStatesResponse> responseObserver) {
        List<VehicleBrakeState> states = brakeStateService.getAllStates().stream()
                .map(s -> VehicleBrakeState.newBuilder()
                        .setVin(s.vin)
                        .setEmergencyBrakeActive(s.emergencyBrakeActive)
                        .setPreEmergencyBrakeActive(s.preEmergencyBrakeActive)
                        .build())
                .toList();

        responseObserver.onNext(GetBrakeStatesResponse.newBuilder().addAllStates(states).build());
        responseObserver.onCompleted();
    }
}
