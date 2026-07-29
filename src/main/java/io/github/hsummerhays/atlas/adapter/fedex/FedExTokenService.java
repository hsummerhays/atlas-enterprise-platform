package io.github.hsummerhays.atlas.adapter.fedex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

@Service
public class FedExTokenService {

    private static final Logger log = LoggerFactory.getLogger(FedExTokenService.class);

    private final FedExProperties properties;
    private final RestClient restClient;

    private String cachedToken;
    private Instant expiryTime;

    public FedExTokenService(FedExProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    public synchronized String getAccessToken() {
        if (cachedToken != null && expiryTime != null && Instant.now().isBefore(expiryTime)) {
            return cachedToken;
        }

        log.info("Requesting new FedEx OAuth2 Token...");
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "client_credentials");
            formData.add("client_id", properties.getClientId());
            formData.add("client_secret", properties.getClientSecret());

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("access_token")) {
                cachedToken = (String) response.get("access_token");
                int expiresIn = ((Number) response.getOrDefault("expires_in", 3600)).intValue();
                // Buffer by 60 seconds
                expiryTime = Instant.now().plusSeconds(expiresIn - 60);
                log.info("FedEx OAuth2 Token acquired successfully. Expires in {} seconds.", expiresIn);
                return cachedToken;
            } else {
                throw new IllegalStateException("Invalid response from FedEx Token endpoint");
            }
        } catch (Exception e) {
            log.error("Failed to authenticate with FedEx API", e);
            throw new RuntimeException("FedEx authentication failure: " + e.getMessage(), e);
        }
    }
}
