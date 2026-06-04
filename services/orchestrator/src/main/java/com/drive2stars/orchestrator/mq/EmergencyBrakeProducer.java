package com.drive2stars.orchestrator.mq;

import com.drive2stars.shared.messaging.BrakeMessage;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@ApplicationScoped
public class EmergencyBrakeProducer {

  private final Emitter<BrakeMessage> brakeEmitter;

  public EmergencyBrakeProducer(@Channel("vehicle-brake") Emitter<BrakeMessage> brakeEmitter) {
    this.brakeEmitter = brakeEmitter;
  }

  public void sendBrakeMessage(BrakeMessage message) {
    brakeEmitter.send(message);
  }
}
