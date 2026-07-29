package io.github.hsummerhays.atlas.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AgentCoordinator {

    private static final Logger log = LoggerFactory.getLogger(AgentCoordinator.class);

    private final Map<String, AIAgent> agents;
    private final AgentMetricsService metricsService;
    private final AgentAuditLogService auditLogService;
    private final LoadSheddingGuard loadSheddingGuard;
    private final AgentApprovalNotifier approvalNotifier;

    public AgentCoordinator(List<AIAgent> agentList, AgentMetricsService metricsService,
                             AgentAuditLogService auditLogService, LoadSheddingGuard loadSheddingGuard,
                             AgentApprovalNotifier approvalNotifier) {
        this.agents = agentList.stream()
                .collect(Collectors.toMap(
                        agent -> agent.getClass().getSimpleName().toLowerCase(),
                        Function.identity()
                ));
        this.metricsService = metricsService;
        this.auditLogService = auditLogService;
        this.loadSheddingGuard = loadSheddingGuard;
        this.approvalNotifier = approvalNotifier;
    }

    public int getActiveAgentCount() {
        return loadSheddingGuard.getActiveCount();
    }

    public int getThreshold() {
        return loadSheddingGuard.getThreshold();
    }

    public AgentResult runAgent(String agentName, AgentRequest request) {
        AIAgent agent = Optional.ofNullable(agents.get(agentName.toLowerCase()))
                .orElseThrow(() -> new IllegalArgumentException("Unknown AI Agent: " + agentName));

        int active = loadSheddingGuard.enter();
        try {
            loadSheddingGuard.enforceCapacity(active, agentName);

            String executionId = UUID.randomUUID().toString();
            MDC.put("executionId", executionId);
            try {
                long startMs = System.currentTimeMillis();
                AgentResult result = agent.run(request);
                long durationMs = System.currentTimeMillis() - startMs;

                ApprovalLevel approvalLevel = agent.getRequiredApprovalLevel();

                Map<String, Object> enrichedMetadata = new HashMap<>(result.metadata());
                enrichedMetadata.put("approvalLevel", approvalLevel.name());
                enrichedMetadata.put("executionMillis", durationMs);
                AgentResult enrichedResult = new AgentResult(
                        result.agentName(), result.success(), result.output(), Map.copyOf(enrichedMetadata));

                if (enrichedResult.metadata().containsKey("prUrl")) {
                    double timeSavedMinutes = durationMs / 60_000.0;
                    metricsService.recordPrGenerated(agentName, timeSavedMinutes);

                    if (approvalLevel == ApprovalLevel.SECURITY || approvalLevel == ApprovalLevel.ARCHITECTURE) {
                        approvalNotifier.notifyApprovalRequired(agentName, approvalLevel,
                                (String) enrichedResult.metadata().get("prUrl"), executionId);
                    }
                }

                auditLogService.logExecution(executionId, agentName, request, enrichedResult, approvalLevel);
                return enrichedResult;
            } finally {
                MDC.remove("executionId");
            }
        } finally {
            loadSheddingGuard.exit();
        }
    }

    public List<String> getRegisteredAgents() {
        return List.copyOf(agents.keySet());
    }
}
