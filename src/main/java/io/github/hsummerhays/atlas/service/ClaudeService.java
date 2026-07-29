package io.github.hsummerhays.atlas.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import io.github.hsummerhays.atlas.config.ClaudeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ClaudeService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeService.class);

    private final AnthropicClient client;
    private final ClaudeProperties properties;

    public ClaudeService(ClaudeProperties properties) {
        this.properties = properties;
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            this.client = AnthropicOkHttpClient.builder()
                    .apiKey(properties.getApiKey())
                    .build();
            log.info("ClaudeService initialized with model {}", properties.getModel());
        } else {
            log.warn("CLAUDE_API_KEY not configured. Claude calls will return simulated responses.");
            this.client = null;
        }
    }

    public String analyze(String systemPrompt, String userPrompt) {
        if (client == null) {
            log.info("Simulating Claude analysis (no API key configured)");
            return "[SIMULATED] Claude analysis for: " + userPrompt.substring(0, Math.min(120, userPrompt.length()));
        }
        MessageCreateParams params = MessageCreateParams.builder()
                .model(properties.getModel())
                .maxTokens(properties.getMaxTokens())
                .thinking(ThinkingConfigAdaptive.builder().build())
                .system(systemPrompt)
                .addUserMessage(userPrompt)
                .build();
        Message response = client.messages().create(params);
        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(t -> t.text())
                .findFirst()
                .orElse("");
    }
}
