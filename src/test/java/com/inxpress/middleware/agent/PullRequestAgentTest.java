package com.inxpress.middleware.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class PullRequestAgentTest {

    @Autowired
    private PullRequestAgent pullRequestAgent;

    @Test
    public void testPullRequestAgentOrchestration() {
        AgentTask task = () -> new AgentResult("PullRequestAgent", true, "Add UPS adapter", java.util.Map.of());
        
        // Execute agent run (fallback mock branch and PR are generated safely)
        AgentResult result = pullRequestAgent.run(task);

        assertTrue(result.success());
        assertEquals("PullRequestAgent", result.agentName());
        assertTrue(result.output().contains("PR Opened"));
    }
}
