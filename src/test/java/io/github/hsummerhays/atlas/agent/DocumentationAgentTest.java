package io.github.hsummerhays.atlas.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class DocumentationAgentTest {

    @Autowired
    private DocumentationAgent documentationAgent;

    @Test
    public void testAgentDocumentationGeneration() {
        AgentRequest request = new AgentRequest("Update OpenAPI and architecture docs", Map.of(), "test");
        AgentResult result = documentationAgent.run(request);

        assertTrue(result.success());
        assertEquals("DocumentationAgent", result.agentName());
        assertTrue(result.output().contains("Successfully generated documentation"));
        assertTrue(result.metadata().containsKey("prUrl"));
    }
}
