package com.drive2stars.brakenow.service;

import com.drive2stars.brakenow.mq.BrakePublisher;
import com.drive2stars.brakenow.mq.SimulatorPublisher;
import com.drive2stars.shared.messaging.BrakeMessage;
import com.drive2stars.shared.messaging.SimulatorBrakeMessage;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * brake logic for a vehicle.
 * 0 = no condition, 1–3 = condition that triggered emergency brake.
 * 1 — dist < 30 m, closing > 4 m/s
 * 2 — dist < 15 m, closing > 2 m/s
 * 3 — dist < 5 m, closing > 0 m/s

 * Pre-emergency brake state:
 *   Entered when distance < 45 m.
 *   Exited  when distance >= 45 m.
 */
@ApplicationScoped
public class BrakeService {

    private static final Logger LOG = Logger.getLogger(BrakeService.class);

    static final double PRE_EMERGENCY_THRESHOLD_M = 45.0;

    static final double COND1_DIST_M  = 30.0;
    static final double COND1_RATE_MS =  4.0;
    static final double COND2_DIST_M  = 15.0;
    static final double COND2_RATE_MS =  2.0;
    static final double COND3_DIST_M  =  5.0;
    static final double COND3_RATE_MS =  0.0;


    @ConfigProperty(name = "brakenow.vin")
    String ownVin;

    private final BrakePublisher brakePublisher;

    private final ConcurrentHashMap<String, BrakeState> states = new ConcurrentHashMap<>();

    @Inject
    SimulatorPublisher simulatorPublisher;

    public BrakeService(BrakePublisher brakePublisher) {
       this.brakePublisher = brakePublisher;
     }

    /**
     * @param vin the vehicle this reading belongs to
     * @param distanceM  distance to the vehicle ahead in metres
     * @param closingMps closing rate in m/s, positive = approaching
     */
    public void processDistance(String vin, double distanceM, double closingMps) {
        boolean preEmergency = distanceM < PRE_EMERGENCY_THRESHOLD_M;
        int condition = evaluateCondition(distanceM, closingMps);
        boolean emergencyBrake = condition > 0;
        updateAndPublish(vin, distanceM, emergencyBrake, preEmergency, condition);
    }


    public BrakeState getState(String vin) {
        return states.get(vin);
    }

    public Collection<BrakeState> getAllStates() {
        return states.values();
    }

    private int evaluateCondition(double dist, double closingRate) {
        if (dist < COND3_DIST_M && closingRate > COND3_RATE_MS) return 3;
        if (dist < COND2_DIST_M && closingRate > COND2_RATE_MS) return 2;
        if (dist < COND1_DIST_M && closingRate > COND1_RATE_MS) return 1;
        return 0;
    }

    private void updateAndPublish(String vin, double dist,
                                   boolean emergencyBrake, boolean preEmergency, int condition) {
        BrakeState prev = states.get(vin);

        // Skip if nothing changed
        if (prev != null
                && prev.emergencyBrakeActive == emergencyBrake
                && prev.preEmergencyBrake    == preEmergency
                && prev.conditionTriggered   == condition) {
            return;
        }

        BrakeState next = new BrakeState(vin, emergencyBrake, preEmergency, condition, Instant.now());
        states.put(vin, next);

        if (emergencyBrake) {
            LOG.infof("EMERGENCY BRAKE vin=%s condition=%d dist=%.1fm", vin, condition, dist);
        } else if (preEmergency) {
            LOG.infof("Pre-emergency vin=%s dist=%.1fm", vin, dist);
        } else {
            LOG.infof("Normal vin=%s dist=%.1fm", vin, dist);
        }


        brakePublisher.publish(
              new BrakeMessage(vin, emergencyBrake, condition, BrakeMessage.Source.BRAKENOW, Instant.now()));
        SimulatorBrakeMessage simulatorBrakeMessage = new SimulatorBrakeMessage(vin,emergencyBrake, preEmergency, Instant.now());
        simulatorPublisher.publish(simulatorBrakeMessage);
    }

    public void processBrake(String vin, boolean active, Instant timestamp) {
      SimulatorBrakeMessage simulatorBrakeMessage = new SimulatorBrakeMessage(vin,active, false, timestamp);

      simulatorPublisher.publish(simulatorBrakeMessage);
    }
}
