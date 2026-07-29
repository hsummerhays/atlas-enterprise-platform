package io.github.hsummerhays.atlas.controller;

import io.github.hsummerhays.atlas.config.GitHubProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.sqs.SqsClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GitHubWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SqsClient sqsClient;

    @MockitoBean
    private GitHubProperties githubProperties;

    private static final String TEST_SECRET = "test-webhook-secret";

    @Test
    void validSignature_issueOpened_returns200() throws Exception {
        byte[] body = issuePayload("need docs", "1");
        String sig = hmacSha256(body, TEST_SECRET);
        when(githubProperties.getWebhookSecret()).thenReturn(TEST_SECRET);

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Event", "issues")
                        .header("X-Hub-Signature-256", sig)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void invalidSignature_returns401() throws Exception {
        byte[] body = issuePayload("need docs", "2");
        when(githubProperties.getWebhookSecret()).thenReturn(TEST_SECRET);

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Event", "issues")
                        .header("X-Hub-Signature-256", "sha256=" + "0".repeat(64))
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingSignature_whenSecretConfigured_returns401() throws Exception {
        when(githubProperties.getWebhookSecret()).thenReturn(TEST_SECRET);

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Event", "push")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void noSecretConfigured_rejectsRequest_returns503() throws Exception {
        when(githubProperties.getWebhookSecret()).thenReturn(null);

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Event", "push")
                        .content("{}"))
                .andExpect(status().isServiceUnavailable());
    }

    private byte[] issuePayload(String title, String number) {
        String json = "{\"action\":\"opened\",\"issue\":{\"number\":" + number
                + ",\"title\":\"" + title + "\",\"body\":\"desc\"}}";
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private String hmacSha256(byte[] body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
    }
}
