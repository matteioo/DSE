package com.drive2stars.orchestrator.service;

import com.drive2stars.shared.messaging.DistanceMessage;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class OrchestratorService {

    private final BrakeConditionService brakeConditionService;
    private final PlausibilityService plausibilityService;

    public OrchestratorService(BrakeConditionService brakeConditionService,
                               PlausibilityService plausibilityService) {
        this.brakeConditionService = brakeConditionService;
        this.plausibilityService = plausibilityService;
    }

    /**
     * Processes a distance message coming from a SONAR service. It both processes the SONAR findings
     * and checks the integrity of the detected distances by calculating the distance via the UTRACKED
     * service in parallel.
     *
     * @param msg the distance message received from the SONAR service
     */
    public void process(DistanceMessage msg) {
        // Steps 1-3: check SONAR service findings and trigger alerts if needed
        CompletableFuture<Void> sonarProcessing = brakeConditionService.checkSonarConditions(msg);

        // Steps 4: calculate distances via UTRACKED
        CompletableFuture<Void> utrackedCheck = plausibilityService.validateDistance(msg);

        // Wait for both SONAR processing and UTRACKED check to complete before proceeding
        CompletableFuture.allOf(sonarProcessing, utrackedCheck).join();
    }
}
