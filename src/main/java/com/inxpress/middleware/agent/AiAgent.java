package com.inxpress.middleware.agent;

public interface AiAgent {
    String getAgentName();
    AgentResponse execute(AgentRequest request);
}
