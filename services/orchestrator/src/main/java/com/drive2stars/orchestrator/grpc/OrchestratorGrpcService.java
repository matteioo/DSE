package com.drive2stars.orchestrator.grpc;

import com.drive2stars.grpc.orchestrator.BrakeEvent;
import com.drive2stars.grpc.orchestrator.GetEventsRequest;
import com.drive2stars.grpc.orchestrator.GetEventsResponse;
import com.drive2stars.grpc.orchestrator.OrchestratorServiceGrpc;
import com.drive2stars.orchestrator.persistence.EventEntity;
import com.drive2stars.orchestrator.service.EventService;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;

import java.util.List;

@GrpcService
public class OrchestratorGrpcService extends OrchestratorServiceGrpc.OrchestratorServiceImplBase {

    EventService eventService;

    public OrchestratorGrpcService(EventService eventService) {
        this.eventService = eventService;
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
                .build())
            .toList();

        responseObserver.onNext(GetEventsResponse.newBuilder().addAllEvents(events).build());
        responseObserver.onCompleted();
    }
}
