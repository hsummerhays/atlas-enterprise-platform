package com.inxpress.middleware.adapter.auth;

import java.util.Collections;
import java.util.function.Supplier;

/**
 * Factory class to resolve the correct CarrierAuthenticator implementation based on the CarrierConfiguration.
 */
public class AuthenticationFactory {

    /**
     * Creates a CarrierAuthenticator based on the configuration and an optional OAuth token supplier.
     *
     * @param config The carrier authentication configuration
     * @param tokenSupplier The supplier for OAuth access tokens (required if authType is "oauth")
     * @return Resolved CarrierAuthenticator
     */
    public static CarrierAuthenticator createAuthenticator(CarrierConfiguration config, Supplier<String> tokenSupplier) {
        if (config == null || config.authType() == null) {
            return Collections::emptyMap;
        }

        return switch (config.authType().toLowerCase()) {
            case "oauth" -> {
                if (tokenSupplier == null) {
                    throw new IllegalArgumentException("tokenSupplier is required for OAuth authentication");
                }
                yield new OAuthAuthenticator(tokenSupplier);
            }
            case "basic" -> new BasicAuthenticator(config::username, config::password);
            case "apikey" -> new ApiKeyAuthenticator(
                    config.apiKeyHeader() != null ? config.apiKeyHeader() : "X-API-KEY",
                    config::apiKey
            );
            case "mtls" -> new MtlsAuthenticator();
            default -> Collections::emptyMap;
        };
    }
}
