package com.inxpress.middleware.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class DocumentationAgentTest {

    @Autowired
    private DocumentationAgent documentationAgent;

    @Test
    public void testAgentDocumentationGeneration() {
        // Clean up files before test execution
        File openApiFile = new File("docs/openapi.yaml");
        File archFile = new File("docs/architecture.md");
        
        if (openApiFile.exists()) openApiFile.delete();
        if (archFile.exists()) archFile.delete();

        // Run agent
        AgentTask task = () -> new AgentResult("DocumentationAgent", true, "Execute mock task", java.util.Map.of());
        AgentResult result = documentationAgent.run(task);

        // Verify result
        assertTrue(result.success());
        assertEquals("DocumentationAgent", result.agentName());
        assertTrue(result.output().contains("Successfully generated documentation"));

        // Verify files were actually written to filesystem
        assertTrue(openApiFile.exists(), "openapi.yaml should exist");
        assertTrue(archFile.exists(), "architecture.md should exist");
    }
}
