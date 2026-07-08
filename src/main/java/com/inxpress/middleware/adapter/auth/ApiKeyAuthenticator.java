package com.inxpress.middleware.adapter.auth;

import java.util.Map;
import java.util.function.Supplier;

/**
 * CarrierAuthenticator implementation for API Key authentication.
 */
public class ApiKeyAuthenticator implements CarrierAuthenticator {

    private final String headerName;
    private final Supplier<String> apiKeySupplier;

    public ApiKeyAuthenticator(String headerName, Supplier<String> apiKeySupplier) {
        this.headerName = headerName;
        this.apiKeySupplier = apiKeySupplier;
    }

    @Override
    public Map<String, String> getAuthHeaders() {
        return Map.of(headerName, apiKeySupplier.get());
    }
}
