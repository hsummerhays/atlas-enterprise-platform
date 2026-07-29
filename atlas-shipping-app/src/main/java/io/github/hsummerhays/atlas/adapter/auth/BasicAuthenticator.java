package io.github.hsummerhays.atlas.adapter.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.function.Supplier;

/**
 * CarrierAuthenticator implementation for HTTP Basic Authentication.
 */
public class BasicAuthenticator implements CarrierAuthenticator {

    private final Supplier<String> usernameSupplier;
    private final Supplier<String> passwordSupplier;

    public BasicAuthenticator(Supplier<String> usernameSupplier, Supplier<String> passwordSupplier) {
        this.usernameSupplier = usernameSupplier;
        this.passwordSupplier = passwordSupplier;
    }

    @Override
    public Map<String, String> getAuthHeaders() {
        String credentials = usernameSupplier.get() + ":" + passwordSupplier.get();
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return Map.of("Authorization", "Basic " + encoded);
    }
}
