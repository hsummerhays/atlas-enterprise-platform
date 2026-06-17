package com.inxpress.middleware.service;

import com.inxpress.middleware.config.GitHubProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class GitHubIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(GitHubIntegrationService.class);

    private final GitHubProperties properties;
    private final RestClient restClient;

    public GitHubIntegrationService(GitHubProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getApiUrl())
                .build();
    }

    public boolean createBranch(String branchName, String sourceBranchSha) {
        log.info("Requesting branch creation on GitHub: {} from {}", branchName, sourceBranchSha);
        
        if (properties.getToken() == null || properties.getToken().isBlank()) {
            log.warn("GitHub OAuth token not configured. Simulating branch creation.");
            return true;
        }

        try {
            String uri = String.format("/repos/%s/%s/git/refs", properties.getOwner(), properties.getRepo());
            Map<String, Object> payload = Map.of(
                "ref", "refs/heads/" + branchName,
                "sha", sourceBranchSha
            );

            restClient.post()
                    .uri(uri)
                    .header("Authorization", "token " + properties.getToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            
            log.info("Successfully created branch: {}", branchName);
            return true;
        } catch (Exception e) {
            log.error("Failed to create GitHub branch: {}", e.getMessage());
            return false;
        }
    }

    public String createPullRequest(String title, String headBranch, String baseBranch, String body) {
        log.info("Opening GitHub PR from {} to {} with title: {}", headBranch, baseBranch, title);

        if (properties.getToken() == null || properties.getToken().isBlank()) {
            log.warn("GitHub OAuth token not configured. Simulating PR creation.");
            return "https://github.com/mock-owner/mock-repo/pull/42";
        }

        try {
            String uri = String.format("/repos/%s/%s/pulls", properties.getOwner(), properties.getRepo());
            Map<String, Object> payload = Map.of(
                "title", title,
                "head", headBranch,
                "base", baseBranch,
                "body", body
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(uri)
                    .header("Authorization", "token " + properties.getToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("html_url")) {
                String prUrl = (String) response.get("html_url");
                log.info("Successfully created Pull Request: {}", prUrl);
                return prUrl;
            }
            throw new RuntimeException("PR response missing html_url");
        } catch (Exception e) {
            log.error("Failed to create GitHub PR: {}", e.getMessage());
            throw new RuntimeException("GitHub PR creation failed: " + e.getMessage(), e);
        }
    }
}
