package com.inxpress.middleware.agent;

import com.inxpress.middleware.service.ClaudeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RefactoringAgent implements AIAgent {

    private static final Logger log = LoggerFactory.getLogger(RefactoringAgent.class);

    private final ClaudeService claudeService;

    public RefactoringAgent(ClaudeService claudeService) {
        this.claudeService = claudeService;
    }

    @Override
    public AgentResult run(AgentRequest request) {
        log.info("RefactoringAgent analyzing code for refactoring opportunities...");

        String systemPrompt = """
                You are a senior Java engineer specializing in Spring Boot performance and code quality.
                Analyze the provided code and suggest concrete refactoring improvements.
                Focus on: virtual thread optimization, Spring Boot 3 best practices, unnecessary complexity,
                and readability improvements. Be concise and actionable.
                """;

        String userPrompt = request.inputData() != null && !request.inputData().isBlank()
                ? request.inputData()
                : "Analyze this Spring Boot application for refactoring opportunities.";

        String analysis = claudeService.analyze(systemPrompt, userPrompt);

        return new AgentResult(
                "RefactoringAgent",
                true,
                analysis,
                Map.of("requestedBy", request.requestedBy() != null ? request.requestedBy() : "unknown")
        );
    }
}
