package com.inxpress.middleware.agent;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AgentCoordinator {

    private final Map<String, AiAgent> agents;

    public AgentCoordinator(List<AiAgent> agentList) {
        this.agents = agentList.stream()
                .collect(Collectors.toMap(
                        agent -> agent.getAgentName().toLowerCase(),
                        Function.identity()
                ));
    }

    public AgentResponse routeRequest(String agentName, AgentRequest request) {
        AiAgent agent = Optional.ofNullable(agents.get(agentName.toLowerCase()))
                .orElseThrow(() -> new IllegalArgumentException("Unknown AI Agent: " + agentName));
        return agent.execute(request);
    }

    public List<String> getRegisteredAgents() {
        return List.copyOf(agents.keySet());
    }
}
