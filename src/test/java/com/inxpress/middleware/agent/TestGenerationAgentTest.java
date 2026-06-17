package com.inxpress.middleware.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class TestGenerationAgentTest {

    @Autowired
    private TestGenerationAgent testGenerationAgent;

    @Test
    public void testAgentExecution() {
        AgentRequest request = new AgentRequest("Generate ShipmentService tests", Map.of(), "test");
        AgentResult result = testGenerationAgent.run(request);

        assertTrue(result.success());
        assertEquals("TestGenerationAgent", result.agentName());
        assertTrue(result.output().contains("Successfully generated tests"));
        assertTrue(result.metadata().containsKey("prUrl"));
    }
}
