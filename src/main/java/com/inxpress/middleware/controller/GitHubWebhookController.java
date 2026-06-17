package com.inxpress.middleware.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks/github")
public class GitHubWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookController.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.agent-tasks-queue-url:}")
    private String queueUrl;

    public GitHubWebhookController(SqsClient sqsClient, ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "X-GitHub-Event", defaultValue = "unknown") String eventType,
            @RequestBody Map<String, Object> payload) {

        log.info("Received GitHub webhook event: {}", eventType);

        if (!"issues".equals(eventType)) {
            return ResponseEntity.ok("Event type not handled: " + eventType);
        }

        String action = (String) payload.getOrDefault("action", "");
        if (!"opened".equals(action)) {
            return ResponseEntity.ok("Issue action not handled: " + action);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> issue = (Map<String, Object>) payload.get("issue");
        if (issue == null) {
            return ResponseEntity.badRequest().body("Missing issue payload");
        }

        String title = (String) issue.getOrDefault("title", "");
        String body = (String) issue.getOrDefault("body", "");
        String issueNumber = String.valueOf(issue.get("number"));

        String agentName = resolveAgentForIssue(title, body);
        String inputData = String.format("GitHub Issue #%s: %s\n\n%s", issueNumber, title, body);

        try {
            enqueueAgentTask(agentName, inputData, "github-webhook");
            log.info("Enqueued agent task: agent={} issue=#{}", agentName, issueNumber);
            return ResponseEntity.ok("Enqueued agent task for issue #" + issueNumber);
        } catch (Exception e) {
            log.error("Failed to enqueue agent task for issue #{}: {}", issueNumber, e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to enqueue task: " + e.getMessage());
        }
    }

    private String resolveAgentForIssue(String title, String body) {
        String combined = (title + " " + body).toLowerCase();
        if (combined.contains("test") || combined.contains("coverage")) {
            return "testgenerationagent";
        }
        if (combined.contains("carrier") || combined.contains("adapter") || combined.contains("usps")
                || combined.contains("fedex") || combined.contains("ups") || combined.contains("dhl")) {
            return "carrieradapteragent";
        }
        if (combined.contains("doc") || combined.contains("openapi") || combined.contains("readme")) {
            return "documentationagent";
        }
        if (combined.contains("security") || combined.contains("vulnerability") || combined.contains("cve")) {
            return "securityreviewagent";
        }
        if (combined.contains("refactor") || combined.contains("performance") || combined.contains("optimize")) {
            return "refactoringagent";
        }
        return "pullrequestagent";
    }

    private void enqueueAgentTask(String agentName, String inputData, String requestedBy) throws Exception {
        if (queueUrl == null || queueUrl.isBlank()) {
            log.warn("SQS queue URL not configured. Skipping enqueue for agent {}", agentName);
            return;
        }

        Map<String, Object> taskPayload = Map.of(
                "agentName", agentName,
                "inputData", inputData,
                "requestedBy", requestedBy,
                "context", Map.of()
        );

        String messageBody = objectMapper.writeValueAsString(taskPayload);

        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody)
                .build());
    }
}
