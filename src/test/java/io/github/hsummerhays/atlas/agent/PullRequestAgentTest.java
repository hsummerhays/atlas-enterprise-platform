package io.github.hsummerhays.atlas.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class PullRequestAgentTest {

    @Autowired
    private PullRequestAgent pullRequestAgent;

    @Test
    public void testPullRequestAgentOrchestration() {
        AgentRequest request = new AgentRequest("Add UPS adapter", Map.of(), "test");
        AgentResult result = pullRequestAgent.run(request);

        assertTrue(result.success());
        assertEquals("PullRequestAgent", result.agentName());
        assertTrue(result.output().contains("PR Opened"));
        assertTrue(result.metadata().containsKey("prUrl"));
    }
}
