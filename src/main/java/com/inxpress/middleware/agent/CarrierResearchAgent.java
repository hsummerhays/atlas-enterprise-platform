package com.inxpress.middleware.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CarrierResearchAgent implements AiAgent {

    private static final Logger log = LoggerFactory.getLogger(CarrierResearchAgent.class);

    @Override
    public String getAgentName() {
        return "CarrierResearchAgent";
    }

    @Override
    public AgentResponse execute(AgentRequest request) {
        log.info("CarrierResearchAgent executing task: {}", request.taskId());
        // Simulate LLM researching API changes in carrier spec docs
        String specAnalysis = """
            ## DHL Express API Update Analysis
            - DHL sandbox base URL changed to: https://express.api.dhl.com/sandbox/gse
            - Required content type is application/json
            - Added support for paperless customs declarations.
            """;

        return new AgentResponse(
                getAgentName(),
                true,
                specAnalysis,
                Map.of("targetCarrier", "DHL")
        );
    }
}
