package com.inxpress.middleware.agent;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AgentCoordinator {

    private final Map<String, AIAgent> agents;
    private final AgentMetricsService metricsService;
    private final AgentAuditLogService auditLogService;

    public AgentCoordinator(List<AIAgent> agentList, AgentMetricsService metricsService, AgentAuditLogService auditLogService) {
        this.agents = agentList.stream()
                .collect(Collectors.toMap(
                        agent -> agent.getClass().getSimpleName().toLowerCase(),
                        Function.identity()
                ));
        this.metricsService = metricsService;
        this.auditLogService = auditLogService;
    }

    public AgentResult runAgent(String agentName, AgentRequest request) {
        AIAgent agent = Optional.ofNullable(agents.get(agentName.toLowerCase()))
                .orElseThrow(() -> new IllegalArgumentException("Unknown AI Agent: " + agentName));

        AgentResult result = agent.run(request);
        ApprovalLevel approvalLevel = agent.getRequiredApprovalLevel();

        Map<String, Object> enrichedMetadata = new HashMap<>(result.metadata());
        enrichedMetadata.put("approvalLevel", approvalLevel.name());
        AgentResult enrichedResult = new AgentResult(result.agentName(), result.success(), result.output(), Map.copyOf(enrichedMetadata));

        if (enrichedResult.metadata().containsKey("prUrl")) {
            metricsService.recordPrGenerated(agentName, 30);
        }
        auditLogService.logExecution(agentName, request, enrichedResult, approvalLevel);

        return enrichedResult;
    }

    public List<String> getRegisteredAgents() {
        return List.copyOf(agents.keySet());
    }
}
