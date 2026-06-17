package com.inxpress.middleware.agent;

import java.util.Map;

public record AgentRequest(
    String taskId,
    String inputData,
    Map<String, Object> context
) {}
