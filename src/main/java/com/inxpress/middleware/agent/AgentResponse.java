package com.inxpress.middleware.agent;

import java.util.Map;

public record AgentResponse(
    String agentName,
    boolean success,
    String outputData,
    Map<String, Object> metadata
) {}
