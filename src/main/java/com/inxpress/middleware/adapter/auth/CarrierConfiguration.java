package com.inxpress.middleware.adapter.auth;

/**
 * Representation of carrier authentication configuration.
 */
public record CarrierConfiguration(
    String authType,
    String clientId,
    String clientSecret,
    String username,
    String password,
    String apiKey,
    String apiKeyHeader
) {
    public static CarrierConfiguration oauth(String clientId, String clientSecret) {
        return new CarrierConfiguration("oauth", clientId, clientSecret, null, null, null, null);
    }

    public static CarrierConfiguration basic(String username, String password) {
        return new CarrierConfiguration("basic", null, null, username, password, null, null);
    }

    public static CarrierConfiguration apiKey(String apiKeyHeader, String apiKey) {
        return new CarrierConfiguration("apikey", null, null, null, null, apiKey, apiKeyHeader);
    }

    public static CarrierConfiguration mtls() {
        return new CarrierConfiguration("mtls", null, null, null, null, null, null);
    }
}
