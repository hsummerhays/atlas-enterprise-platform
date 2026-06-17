package com.inxpress.middleware.agent;

import com.inxpress.middleware.service.ClaudeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SecurityReviewAgent implements AIAgent {

    private static final Logger log = LoggerFactory.getLogger(SecurityReviewAgent.class);

    private final ClaudeService claudeService;

    public SecurityReviewAgent(ClaudeService claudeService) {
        this.claudeService = claudeService;
    }

    @Override
    public AgentResult run(AgentRequest request) {
        log.info("SecurityReviewAgent performing security analysis...");

        String systemPrompt = """
                You are a security engineer specializing in Java Spring Boot applications.
                Perform a security review of the provided code or description.
                Check for: OWASP Top 10 vulnerabilities, insecure JWT handling, SQL injection risks,
                exposed secrets, improper authorization, and dependency vulnerabilities.
                Return a prioritized list of findings with remediation steps.
                """;

        String userPrompt = request.inputData() != null && !request.inputData().isBlank()
                ? request.inputData()
                : "Review the InXpress middleware security posture.";

        String analysis = claudeService.analyze(systemPrompt, userPrompt);

        return new AgentResult(
                "SecurityReviewAgent",
                true,
                analysis,
                Map.of("requestedBy", request.requestedBy() != null ? request.requestedBy() : "unknown")
        );
    }
}
