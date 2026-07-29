package io.github.hsummerhays.atlas.adapter.auth;

import java.util.Collections;
import java.util.Map;

/**
 * CarrierAuthenticator implementation for Mutual TLS (mTLS).
 * Since authentication occurs at the transport layer, this authenticator
 * provides no additional HTTP headers but marks the usage of mTLS.
 */
public class MtlsAuthenticator implements CarrierAuthenticator {

    @Override
    public Map<String, String> getAuthHeaders() {
        // Mutual TLS authentication details are configured directly at the
        // RestClient/HttpClient SSLContext setup, so no HTTP headers are required.
        return Collections.emptyMap();
    }
}
