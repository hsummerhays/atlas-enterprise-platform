package com.inxpress.middleware.service;

import com.inxpress.middleware.config.GitHubProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GitHubIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(GitHubIntegrationService.class);

    public record FileEntry(String content, String sha) {}

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

    public FileEntry getFileEntry(String filePath) {
        log.info("Fetching file content from GitHub: {}", filePath);

        if (properties.getToken() == null || properties.getToken().isBlank()) {
            log.warn("GitHub OAuth token not configured. Returning simulated file entry for: {}", filePath);
            return new FileEntry("", "0000000000000000000000000000000000000000");
        }

        try {
            String uri = String.format("/repos/%s/%s/contents/%s", properties.getOwner(), properties.getRepo(), filePath);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(uri)
                    .header("Authorization", "token " + properties.getToken())
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new RuntimeException("Empty response from GitHub contents API");
            }

            String encodedContent = ((String) response.get("content")).replace("\n", "").replace("\r", "");
            String content = new String(Base64.getDecoder().decode(encodedContent), StandardCharsets.UTF_8);
            String sha = (String) response.get("sha");
            return new FileEntry(content, sha);
        } catch (Exception e) {
            log.error("Failed to fetch file {} from GitHub: {}", filePath, e.getMessage());
            throw new RuntimeException("GitHub file fetch failed: " + e.getMessage(), e);
        }
    }

    public boolean pushFileToGitHub(String branchName, String filePath, String content, String commitMessage) {
        return pushFileToGitHub(branchName, filePath, content, commitMessage, null);
    }

    public boolean pushFileToGitHub(String branchName, String filePath, String content, String commitMessage, String existingSha) {
        log.info("Pushing file {} to branch {} on GitHub", filePath, branchName);

        if (properties.getToken() == null || properties.getToken().isBlank()) {
            log.warn("GitHub OAuth token not configured. Simulating file push for: {}", filePath);
            return true;
        }

        try {
            String uri = String.format("/repos/%s/%s/contents/%s", properties.getOwner(), properties.getRepo(), filePath);
            String encodedContent = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));

            Map<String, Object> payload = new HashMap<>();
            payload.put("message", commitMessage);
            payload.put("content", encodedContent);
            payload.put("branch", branchName);
            if (existingSha != null) {
                payload.put("sha", existingSha);
            }

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
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 422) {
                log.info("Branch {} already exists, continuing with existing branch", branchName);
                return true;
            }
            log.error("Failed to create GitHub branch: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Failed to create GitHub branch: {}", e.getMessage());
            return false;
        }
    }

    public String createPullRequest(String title, String headBranch, String baseBranch, String body) {
        return createPullRequest(title, headBranch, baseBranch, body, List.of());
    }

    public String createPullRequest(String title, String headBranch, String baseBranch, String body, List<String> labels) {
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

            if (response == null || !response.containsKey("html_url")) {
                throw new RuntimeException("PR response missing html_url");
            }

            String prUrl = (String) response.get("html_url");
            log.info("Successfully created Pull Request: {}", prUrl);

            if (!labels.isEmpty() && response.containsKey("number")) {
                int prNumber = ((Number) response.get("number")).intValue();
                addLabelsToPullRequest(prNumber, labels);
            }

            return prUrl;
        } catch (Exception e) {
            log.error("Failed to create GitHub PR: {}", e.getMessage());
            throw new RuntimeException("GitHub PR creation failed: " + e.getMessage(), e);
        }
    }

    private void addLabelsToPullRequest(int prNumber, List<String> labels) {
        try {
            String uri = String.format("/repos/%s/%s/issues/%d/labels",
                    properties.getOwner(), properties.getRepo(), prNumber);
            restClient.post()
                    .uri(uri)
                    .header("Authorization", "token " + properties.getToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("labels", labels))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Applied labels {} to PR #{}", labels, prNumber);
        } catch (Exception e) {
            log.warn("Failed to apply labels to PR #{}: {}", prNumber, e.getMessage());
        }
    }
}
