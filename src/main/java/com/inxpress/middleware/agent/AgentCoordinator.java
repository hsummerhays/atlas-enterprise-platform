package com.inxpress.middleware.agent;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AgentCoordinator {

    private final Map<String, AIAgent> agents;

    public AgentCoordinator(List<AIAgent> agentList) {
        this.agents = agentList.stream()
                .collect(Collectors.toMap(
                        agent -> agent.getClass().getSimpleName().toLowerCase(),
                        Function.identity()
                ));
    }

    public AgentResult runAgent(String agentName, AgentTask task) {
        AIAgent agent = Optional.ofNullable(agents.get(agentName.toLowerCase()))
                .orElseThrow(() -> new IllegalArgumentException("Unknown AI Agent: " + agentName));
        return agent.run(task);
    }

    public List<String> getRegisteredAgents() {
        return List.copyOf(agents.keySet());
    }
}
