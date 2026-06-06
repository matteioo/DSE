package com.drive2stars.spider.adapter.orchestrator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class OrchestratorClient {

    private static final Logger LOG = Logger.getLogger(OrchestratorClient.class.getName());

    @Inject
    @RestClient
    OrchestratorRestClient client;

    public List<OrchestratorBrakeStateDto> getBrakeStates() {
        try {
            return client.getBrakeStates();
        } catch (Exception e) {
            LOG.warning("Could not reach Orchestrator for brake states: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
