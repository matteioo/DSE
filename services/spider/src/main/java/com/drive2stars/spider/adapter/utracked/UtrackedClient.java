package com.drive2stars.spider.adapter.utracked;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.logging.Logger;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class UtrackedClient {

    private static final Logger LOG = Logger.getLogger(UtrackedClient.class.getName());

    @Inject
    @RestClient
    UtrackedRestClient client;

    public List<UtrackedPositionDto> getLatestPositions() {
        return client.getLatestPositions();
    }

}
