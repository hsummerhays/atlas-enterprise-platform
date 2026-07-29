package io.github.hsummerhays.atlas.agent;

import java.util.Map;

public record AgentResult(
    String agentName,
    boolean success,
    String output,
    Map<String, Object> metadata
) {}
