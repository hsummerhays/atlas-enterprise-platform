package com.inxpress.middleware.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class CarrierAdapterAgentTest {

    @Autowired
    private CarrierAdapterAgent carrierAdapterAgent;

    @Test
    public void testScaffoldExecution() {
        // Run agent
        AgentTask task = () -> new AgentResult("CarrierAdapterAgent", true, "Execute mock task", java.util.Map.of());
        AgentResult result = carrierAdapterAgent.run(task);

        // Verify result
        assertTrue(result.success());
        assertEquals("CarrierAdapterAgent", result.agentName());
        assertTrue(result.output().contains("Successfully scaffolded USPS"));

        // Verify files were actually written to filesystem
        assertTrue(new File("src/main/java/com/inxpress/middleware/adapter/usps/UspsProperties.java").exists());
        assertTrue(new File("src/main/java/com/inxpress/middleware/adapter/usps/UspsTokenService.java").exists());
        assertTrue(new File("src/main/java/com/inxpress/middleware/adapter/usps/UspsAdapter.java").exists());
        assertTrue(new File("src/test/java/com/inxpress/middleware/adapter/usps/UspsAdapterTest.java").exists());
    }
}
