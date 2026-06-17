package com.inxpress.middleware.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class InvoiceReconciliationAgent implements AiAgent {

    private static final Logger log = LoggerFactory.getLogger(InvoiceReconciliationAgent.class);

    @Override
    public String getAgentName() {
        return "InvoiceReconciliationAgent";
    }

    @Override
    public AgentResponse execute(AgentRequest request) {
        log.info("InvoiceReconciliationAgent executing task: {}", request.taskId());
        // Simulate LLM comparing billed invoices vs. database rates
        String reconciliationReport = """
            ## Billing Discrepancy Found
            - Shipment ID: 12345
            - Quoted Cost: $45.20
            - Carrier Billed Cost: $62.50
            - Reason: Fuel surcharge adjustment from FedEx.
            """;

        return new AgentResponse(
                getAgentName(),
                true,
                reconciliationReport,
                Map.of("discrepancyBilledDelta", "17.30")
        );
    }
}
