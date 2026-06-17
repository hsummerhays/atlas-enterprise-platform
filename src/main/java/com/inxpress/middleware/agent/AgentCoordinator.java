package com.inxpress.middleware.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

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
    private final SnsClient snsClient;

    @Value("${aws.sns.agent-review-topic-arn:}")
    private String agentReviewTopicArn;

    public AgentCoordinator(List<AIAgent> agentList, AgentMetricsService metricsService,
                             AgentAuditLogService auditLogService, SnsClient snsClient) {
        this.agents = agentList.stream()
                .collect(Collectors.toMap(
                        agent -> agent.getClass().getSimpleName().toLowerCase(),
                        Function.identity()
                ));
        this.metricsService = metricsService;
        this.auditLogService = auditLogService;
        this.snsClient = snsClient;
    }

    public AgentResult runAgent(String agentName, AgentRequest request) {
        AIAgent agent = Optional.ofNullable(agents.get(agentName.toLowerCase()))
                .orElseThrow(() -> new IllegalArgumentException("Unknown AI Agent: " + agentName));

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
                    notifyApprovalRequired(agentName, approvalLevel,
                            (String) enrichedResult.metadata().get("prUrl"), executionId);
                }
            }

            auditLogService.logExecution(executionId, agentName, request, enrichedResult, approvalLevel);
            return enrichedResult;
        } finally {
            MDC.remove("executionId");
        }
    }

    private void notifyApprovalRequired(String agentName, ApprovalLevel approvalLevel, String prUrl, String executionId) {
        if (agentReviewTopicArn == null || agentReviewTopicArn.isBlank()) {
            log.warn("agent-review-topic-arn not configured — skipping SNS notification for {} agent", agentName);
            return;
        }
        String message = String.format(
                "{\"executionId\":\"%s\",\"agentName\":\"%s\",\"approvalLevel\":\"%s\",\"prUrl\":\"%s\",\"action\":\"APPROVAL_REQUIRED\"}",
                executionId, agentName, approvalLevel.name(), prUrl);
        try {
            snsClient.publish(PublishRequest.builder()
                    .topicArn(agentReviewTopicArn)
                    .message(message)
                    .subject("Agent PR Requires Human Approval: " + agentName)
                    .build());
            log.info("SNS approval notification sent for {} agent PR: {}", agentName, prUrl);
        } catch (Exception e) {
            log.error("Failed to publish SNS approval notification for agent {}: {}", agentName, e.getMessage());
        }
    }

    public List<String> getRegisteredAgents() {
        return List.copyOf(agents.keySet());
    }
}
