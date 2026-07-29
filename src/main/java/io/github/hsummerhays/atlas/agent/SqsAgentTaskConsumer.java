package io.github.hsummerhays.atlas.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.List;
import java.util.Map;

@Component
public class SqsAgentTaskConsumer {

    private static final Logger log = LoggerFactory.getLogger(SqsAgentTaskConsumer.class);

    private final SqsClient sqsClient;
    private final AgentCoordinator coordinator;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.agent-tasks-queue-url:}")
    private String queueUrl;

    @Value("${aws.sqs.agent-tasks-dlq-url:}")
    private String dlqUrl;

    @Value("${aws.sqs.agent-tasks-max-receive-count:3}")
    private int maxReceiveCount;

    public SqsAgentTaskConsumer(SqsClient sqsClient, AgentCoordinator coordinator, ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.coordinator = coordinator;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    public void pollAgentTaskQueue() {
        if (queueUrl == null || queueUrl.isBlank()) {
            return;
        }

        if (coordinator.getActiveAgentCount() >= coordinator.getThreshold()) {
            log.warn("Active agent count ({}) meets or exceeds threshold ({}). Delaying SQS polling.",
                    coordinator.getActiveAgentCount(), coordinator.getThreshold());
            return;
        }

        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(20)
                .messageSystemAttributeNames(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT)
                .build();

        List<Message> messages;
        try {
            messages = sqsClient.receiveMessage(receiveRequest).messages();
        } catch (Exception e) {
            log.error("Failed to receive messages from SQS queue {}: {}", queueUrl, e.getMessage());
            return;
        }

        for (Message message : messages) {
            String countStr = message.attributes().getOrDefault(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT, "1");
            int receiveCount = Integer.parseInt(countStr);

            if (receiveCount >= maxReceiveCount) {
                handlePoisonMessage(message, receiveCount);
                continue;
            }

            try {
                processMessage(message);
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build());
            } catch (Exception e) {
                log.error("Failed to process SQS message {}: {}", message.messageId(), e.getMessage());
            }
        }
    }

    private void handlePoisonMessage(Message message, int receiveCount) {
        log.error("Poison message detected: messageId={} receiveCount={} — routing to DLQ", message.messageId(), receiveCount);
        if (dlqUrl != null && !dlqUrl.isBlank()) {
            try {
                sqsClient.sendMessage(SendMessageRequest.builder()
                        .queueUrl(dlqUrl)
                        .messageBody(message.body())
                        .build());
                log.info("Forwarded poison message {} to DLQ", message.messageId());
            } catch (Exception e) {
                log.error("Failed to forward message {} to DLQ: {}", message.messageId(), e.getMessage());
            }
        }
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .build());
    }

    private void processMessage(Message message) throws Exception {
        Map<String, Object> body = objectMapper.readValue(message.body(), new TypeReference<>() {});

        String agentName = (String) body.getOrDefault("agentName", "");
        String inputData = (String) body.getOrDefault("inputData", "");
        String requestedBy = (String) body.getOrDefault("requestedBy", "sqs");

        @SuppressWarnings("unchecked")
        Map<String, Object> context = body.containsKey("context")
                ? (Map<String, Object>) body.get("context")
                : Map.of();

        AgentRequest request = new AgentRequest(inputData, context, requestedBy);

        log.info("Processing SQS agent task: agent={} requestedBy={}", agentName, requestedBy);
        AgentResult result = coordinator.runAgent(agentName, request);
        log.info("SQS agent task completed: agent={} success={}", agentName, result.success());
    }
}
