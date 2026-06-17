package com.inxpress.middleware.service;

import com.inxpress.middleware.config.GitHubProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.HashMap;
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

    public String getMainBranchSha() {
        log.info("Fetching main branch SHA from GitHub");

        if (properties.getToken() == null || properties.getToken().isBlank()) {
            log.warn("GitHub OAuth token not configured. Returning simulated SHA.");
            return "0000000000000000000000000000000000000000";
        }

        try {
            String uri = String.format("/repos/%s/%s/git/ref/heads/main", properties.getOwner(), properties.getRepo());

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(uri)
                    .header("Authorization", "token " + properties.getToken())
                    .retrieve()
                    .body(Map.class);

            if (response != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> objectNode = (Map<String, Object>) response.get("object");
                if (objectNode != null) {
                    return (String) objectNode.get("sha");
                }
            }
            throw new RuntimeException("Response missing object.sha field");
        } catch (Exception e) {
            log.error("Failed to fetch main branch SHA: {}", e.getMessage());
            throw new RuntimeException("GitHub SHA lookup failed: " + e.getMessage(), e);
        }
    }

    public boolean pushFileToGitHub(String branchName, String filePath, String content, String commitMessage) {
        log.info("Pushing file {} to branch {} on GitHub", filePath, branchName);

        if (properties.getToken() == null || properties.getToken().isBlank()) {
            log.warn("GitHub OAuth token not configured. Simulating file push for: {}", filePath);
            return true;
        }

        try {
            String uri = String.format("/repos/%s/%s/contents/%s", properties.getOwner(), properties.getRepo(), filePath);
            String encodedContent = Base64.getEncoder().encodeToString(content.getBytes());

            Map<String, Object> payload = new HashMap<>();
            payload.put("message", commitMessage);
            payload.put("content", encodedContent);
            payload.put("branch", branchName);

            restClient.put()
                    .uri(uri)
                    .header("Authorization", "token " + properties.getToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully pushed {} to branch {}", filePath, branchName);
            return true;
        } catch (Exception e) {
            log.error("Failed to push file {} to GitHub: {}", filePath, e.getMessage());
            return false;
        }
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
