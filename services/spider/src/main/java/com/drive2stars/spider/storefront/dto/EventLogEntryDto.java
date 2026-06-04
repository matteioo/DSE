package com.drive2stars.spider.storefront.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;

@RegisterForReflection
public class EventLogEntryDto {

    public Instant timestamp;
    public String level;
    public String source;
    public String message;
}
