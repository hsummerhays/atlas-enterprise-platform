package com.inxpress.middleware.adapter.ups;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class UpsTokenService {

    private static final Logger log = LoggerFactory.getLogger(UpsTokenService.class);

    private final UpsProperties properties;
    private final RestClient restClient;

    private String cachedToken;
    private Instant expiryTime;

    public UpsTokenService(UpsProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    public synchronized String getAccessToken() {
        if (cachedToken != null && expiryTime != null && Instant.now().isBefore(expiryTime)) {
            return cachedToken;
        }

        log.info("Requesting new UPS OAuth2 Token...");
        try {
            String credentials = properties.getClientId() + ":" + properties.getClientSecret();
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "client_credentials");

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/security/v1/oauth/token")
                    .header("Authorization", "Basic " + encodedCredentials)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("access_token")) {
                cachedToken = (String) response.get("access_token");
                int expiresIn = ((Number) response.getOrDefault("expires_in", 3600)).intValue();
                expiryTime = Instant.now().plusSeconds(expiresIn - 60);
                log.info("UPS OAuth2 Token acquired successfully. Expires in {} seconds.", expiresIn);
                return cachedToken;
            } else {
                throw new IllegalStateException("Invalid response from UPS Token endpoint");
            }
        } catch (Exception e) {
            log.error("Failed to authenticate with UPS API", e);
            throw new RuntimeException("UPS authentication failure: " + e.getMessage(), e);
        }
    }
}
