package com.drive2stars.spider.adapter.utracked;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class UtrackedPositionDto {

    public String vin;
    public double latitude;
    public double longitude;
}
