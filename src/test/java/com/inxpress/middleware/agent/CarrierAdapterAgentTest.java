package com.inxpress.middleware.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class CarrierAdapterAgentTest {

    @Autowired
    private CarrierAdapterAgent carrierAdapterAgent;

    @Test
    public void testScaffoldExecution() {
        AgentRequest request = new AgentRequest("Scaffold USPS carrier adapter", Map.of(), "test");
        AgentResult result = carrierAdapterAgent.run(request);

        assertTrue(result.success());
        assertEquals("CarrierAdapterAgent", result.agentName());
        assertTrue(result.output().contains("Successfully scaffolded USPS"));
        assertTrue(result.metadata().containsKey("prUrl"));
    }
}
