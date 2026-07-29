package io.github.hsummerhays.atlas.adapter.auth;

import java.util.Map;
import java.util.function.Supplier;

/**
 * CarrierAuthenticator implementation for OAuth 2.0 Client Credentials authentication flow.
 */
public class OAuthAuthenticator implements CarrierAuthenticator {

    private final Supplier<String> tokenSupplier;

    public OAuthAuthenticator(Supplier<String> tokenSupplier) {
        this.tokenSupplier = tokenSupplier;
    }

    @Override
    public Map<String, String> getAuthHeaders() {
        String token = tokenSupplier.get();
        return Map.of("Authorization", "Bearer " + token);
    }
}
