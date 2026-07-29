package io.github.hsummerhays.atlas.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class AgentMetricsServiceTest {

    @Autowired
    private AgentMetricsService metricsService;

    @Test
    public void testMetricsTracking() {
        String agentName = "test-gen-agent";

        // Record PR generation
        AgentMetrics m1 = metricsService.recordPrGenerated(agentName, 60.0);
        assertEquals(1, m1.getPrsGenerated());
        assertEquals(60.0, m1.getTimeSavedMinutes());
        assertTrue(m1.getCycleTimeReductionPercentage() > 0);

        // Record Acceptance
        AgentMetrics m2 = metricsService.recordPrAccepted(agentName);
        assertEquals(1, m2.getPrsAccepted());
        assertEquals(100.0, m2.getHumanAcceptanceRate());

        // Record bug introduced
        AgentMetrics m3 = metricsService.recordBugIntroduced(agentName);
        assertEquals(1, m3.getBugsIntroduced());
    }
}
