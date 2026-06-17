package com.inxpress.middleware.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PrGenerationAgent implements AiAgent {

    private static final Logger log = LoggerFactory.getLogger(PrGenerationAgent.class);

    @Override
    public String getAgentName() {
        return "PrGenerationAgent";
    }

    @Override
    public AgentResponse execute(AgentRequest request) {
        log.info("PrGenerationAgent executing task: {}", request.taskId());
        // Simulate LLM parsing git diff to output PR body
        String prDescription = """
            ## Summary of Changes
            This PR implements the core shipping adapters (FedEx, UPS, DHL) and hooks them into the canonical domain API.
            
            ### Checklist:
            - [x] Unit tests pass
            - [x] Security properties configured
            """;

        return new AgentResponse(
                getAgentName(),
                true,
                prDescription,
                Map.of("targetBranch", "main")
        );
    }
}
