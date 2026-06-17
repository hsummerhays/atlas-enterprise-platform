package com.inxpress.middleware.agent;

import java.util.Map;

public record AgentResult(
    String agentName,
    boolean success,
    String output,
    Map<String, Object> metadata
) {}
