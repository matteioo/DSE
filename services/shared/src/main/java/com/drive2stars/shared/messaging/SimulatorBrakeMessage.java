package com.drive2stars.shared.messaging;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Instant;

@RegisterForReflection
public class SimulatorBrakeMessage {

  public String vin;
  public boolean brakeActive;
  public boolean preBrake;
  public Instant timestamp;

  public SimulatorBrakeMessage() {
  }

  public SimulatorBrakeMessage(String vin, boolean brakeActive, boolean preBrake, Instant timestamp) {
    this.vin = vin;
    this.brakeActive = brakeActive;
    this.preBrake = preBrake;
    this.timestamp = timestamp;
  }

}