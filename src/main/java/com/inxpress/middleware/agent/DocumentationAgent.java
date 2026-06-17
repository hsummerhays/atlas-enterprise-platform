package com.inxpress.middleware.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DocumentationAgent implements AiAgent {

    private static final Logger log = LoggerFactory.getLogger(DocumentationAgent.class);

    @Override
    public String getAgentName() {
        return "DocumentationAgent";
    }

    @Override
    public AgentResponse execute(AgentRequest request) {
        log.info("DocumentationAgent executing task: {}", request.taskId());
        // Simulate LLM parsing class structure and generating markdown docs
        String responseMarkdown = """
            # API Documentation Update
            Processed request: %s
            - Extracted Classes: Shipment, Address, PackageDetail
            - Exposed REST APIs: /api/v1/shipments
            - Validation rules checked.
            """.formatted(request.inputData());

        return new AgentResponse(
                getAgentName(),
                true,
                responseMarkdown,
                Map.of("processedTimeMs", 120L)
        );
    }
}
