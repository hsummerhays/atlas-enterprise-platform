package io.github.hsummerhays.atlas.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class AgentAuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AgentAuditLogService.class);

    private final DynamoDbClient dynamoDbClient;
    private final SnsClient snsClient;
    private final String tableName;

    @Value("${aws.sns.agent-review-topic-arn:}")
    private String agentReviewTopicArn;

    public AgentAuditLogService(
            DynamoDbClient dynamoDbClient,
            SnsClient snsClient,
            @Value("${aws.dynamodb.audit-table-name:agent-execution-audit}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.snsClient = snsClient;
        this.tableName = tableName;
    }

    public void logExecution(String executionId, String agentName, AgentRequest request, AgentResult result, ApprovalLevel approvalLevel) {
        String timestamp = Instant.now().toString();

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("executionId", AttributeValue.fromS(executionId));
        item.put("agentName", AttributeValue.fromS(agentName));
        item.put("approvalLevel", AttributeValue.fromS(approvalLevel.name()));
        item.put("inputData", AttributeValue.fromS(request.inputData() != null ? request.inputData() : ""));
        item.put("requestedBy", AttributeValue.fromS(request.requestedBy() != null ? request.requestedBy() : "unknown"));
        item.put("success", AttributeValue.fromBool(result.success()));
        item.put("output", AttributeValue.fromS(result.output() != null ? result.output() : ""));
        item.put("timestamp", AttributeValue.fromS(timestamp));

        try {
            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build());
            log.info("Audit log written: executionId={} agentName={} approvalLevel={}", executionId, agentName, approvalLevel);
        } catch (Exception e) {
            log.error("Failed to write audit log for agent {} (executionId={}): {}", agentName, executionId, e.getMessage(), e);
            alertAuditFailure(executionId, agentName, e);
        }
    }

    private void alertAuditFailure(String executionId, String agentName, Exception cause) {
        if (agentReviewTopicArn == null || agentReviewTopicArn.isBlank()) {
            log.warn("agent-review-topic-arn not configured — cannot alert on audit log failure for executionId={}", executionId);
            return;
        }
        try {
            snsClient.publish(PublishRequest.builder()
                    .topicArn(agentReviewTopicArn)
                    .subject("Agent audit log write failed: " + agentName)
                    .message("Failed to persist HITL audit record. executionId=" + executionId
                            + " agentName=" + agentName + " cause=" + cause.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Failed to publish audit-log-failure alert for executionId={}: {}", executionId, e.getMessage());
        }
    }
}
