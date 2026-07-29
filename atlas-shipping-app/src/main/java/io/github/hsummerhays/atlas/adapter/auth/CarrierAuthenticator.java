package io.github.hsummerhays.atlas.adapter.auth;

import java.util.Map;

/**
 * Strategy interface for carrier authentication.
 * Isolates how authentication credentials/tokens are resolved from the carrier client requests.
 */
public interface CarrierAuthenticator {
    /**
     * Resolves the necessary headers to authenticate request calls with the carrier API.
     *
     * @return Map of authentication headers
     */
    Map<String, String> getAuthHeaders();
}
