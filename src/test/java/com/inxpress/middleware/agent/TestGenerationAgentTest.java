package com.inxpress.middleware.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class TestGenerationAgentTest {

    @Autowired
    private TestGenerationAgent testGenerationAgent;

    @Test
    public void testAgentExecution() {
        // Ensure generated test file does not exist before run or is cleaned up
        File testFile = new File("src/test/java/com/inxpress/middleware/service/ShipmentServiceGeneratedTest.java");
        if (testFile.exists()) {
            testFile.delete();
        }

        // Create a mock task
        AgentTask task = () -> new AgentResult("TestGenerationAgent", true, "Execute mock task", java.util.Map.of());

        // Execute agent
        AgentResult result = testGenerationAgent.run(task);

        // Verify results
        assertTrue(result.success());
        assertEquals("TestGenerationAgent", result.agentName());
        assertTrue(result.output().contains("Successfully generated tests"));
        assertTrue(result.metadata().containsKey("prUrl"));

        // Verify file is actually generated on the filesystem
        assertTrue(testFile.exists(), "The JUnit test file should have been written by the agent");
    }
}
