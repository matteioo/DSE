package com.drive2stars.spider.storefront.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;
import java.util.List;

@RegisterForReflection
public class StorefrontDataDto {

    public Instant fetchedAt;
    public List<VehicleStateDto> vehicles;
    public List<EventLogEntryDto> eventLog;
}
