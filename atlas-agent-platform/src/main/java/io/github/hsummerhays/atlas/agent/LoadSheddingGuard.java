package io.github.hsummerhays.atlas.agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks concurrently running agent executions and rejects new ones once a configured
 * threshold is exceeded. Every {@link #enter()} call must be paired with an {@link #exit()}
 * in a finally block, regardless of whether {@link #enforceCapacity} rejects the execution.
 */
@Component
public class LoadSheddingGuard {

    private final AtomicInteger activeCount = new AtomicInteger(0);

    @Value("${app.load-shedding.threshold:5}")
    private int threshold;

    public int getActiveCount() {
        return activeCount.get();
    }

    public int getThreshold() {
        return threshold;
    }

    public int enter() {
        return activeCount.incrementAndGet();
    }

    public void enforceCapacity(int active, String agentName) {
        if (active > threshold) {
            throw new LoadSheddingException(String.format(
                    "System load threshold exceeded. Active agents: %d, Threshold: %d. Rejecting execution for agent: %s",
                    active - 1, threshold, agentName));
        }
    }

    public void exit() {
        activeCount.decrementAndGet();
    }
}
