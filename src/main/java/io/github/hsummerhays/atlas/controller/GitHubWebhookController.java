package io.github.hsummerhays.atlas.controller;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import io.github.hsummerhays.atlas.config.GitHubProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks/github")
public class GitHubWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookController.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final GitHubProperties githubProperties;

    @Value("${aws.sqs.agent-tasks-queue-url:}")
    private String queueUrl;

    public GitHubWebhookController(SqsClient sqsClient, ObjectMapper objectMapper, GitHubProperties githubProperties) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.githubProperties = githubProperties;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "X-GitHub-Event", defaultValue = "unknown") String eventType,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signatureHeader,
            @RequestBody byte[] rawBody) {

        log.info("Received GitHub webhook event: {}", eventType);

        String webhookSecret = githubProperties.getWebhookSecret();
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("Rejecting webhook: github.webhook-secret is not configured");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Webhook receiver not configured");
        }
        if (signatureHeader == null) {
            log.warn("Rejecting webhook: missing X-Hub-Signature-256 header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing X-Hub-Signature-256 header");
        }
        if (!verifySignature(rawBody, signatureHeader, webhookSecret)) {
            log.warn("Rejecting webhook: invalid signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid webhook signature");
        }

        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(rawBody, new TypeReference<>() {});
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid JSON payload");
        }

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

    private boolean verifySignature(byte[] body, String signatureHeader, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(body);
            String expected = "sha256=" + HexFormat.of().formatHex(digest);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
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
