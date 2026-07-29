package io.github.hsummerhays.atlas.agent;

import java.util.Map;

public record AgentRequest(
        String inputData,
        Map<String, Object> context,
        String requestedBy
) {}
