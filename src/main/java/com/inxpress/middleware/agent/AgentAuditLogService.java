package com.inxpress.middleware.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AgentAuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AgentAuditLogService.class);

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public AgentAuditLogService(
            DynamoDbClient dynamoDbClient,
            @Value("${aws.dynamodb.audit-table-name:agent-execution-audit}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    public void logExecution(String agentName, AgentRequest request, AgentResult result) {
        String executionId = UUID.randomUUID().toString();
        String timestamp = Instant.now().toString();

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("executionId", AttributeValue.fromS(executionId));
        item.put("agentName", AttributeValue.fromS(agentName));
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
            log.info("Audit log written: executionId={} agentName={}", executionId, agentName);
        } catch (Exception e) {
            log.error("Failed to write audit log for agent {}: {}", agentName, e.getMessage());
        }
    }
}
