package io.github.hsummerhays.atlas.adapter.usps;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class UspsTokenService {
    private final UspsProperties properties;
    private final RestClient restClient;

    public UspsTokenService(UspsProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).build();
    }

    public String getAccessToken() {
        // Simulated token retrieval for USPS
        return "mock-usps-token";
    }
}
