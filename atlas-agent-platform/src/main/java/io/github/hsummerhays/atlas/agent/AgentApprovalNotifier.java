package io.github.hsummerhays.atlas.agent;

import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;

/**
 * Publishes an SNS notification when an agent-generated PR requires human approval
 * (SECURITY/ARCHITECTURE approval levels).
 */
@Component
public class AgentApprovalNotifier {

    private static final Logger log = LoggerFactory.getLogger(AgentApprovalNotifier.class);

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sns.agent-review-topic-arn:}")
    private String agentReviewTopicArn;

    public AgentApprovalNotifier(SnsClient snsClient, ObjectMapper objectMapper) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
    }

    public void notifyApprovalRequired(String agentName, ApprovalLevel approvalLevel, String prUrl, String executionId) {
        if (agentReviewTopicArn == null || agentReviewTopicArn.isBlank()) {
            log.warn("agent-review-topic-arn not configured — skipping SNS notification for {} agent", agentName);
            return;
        }
        try {
            Map<String, String> payload = Map.of(
                    "executionId", executionId,
                    "agentName", agentName,
                    "approvalLevel", approvalLevel.name(),
                    "prUrl", prUrl,
                    "action", "APPROVAL_REQUIRED");
            String message = objectMapper.writeValueAsString(payload);
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
}
