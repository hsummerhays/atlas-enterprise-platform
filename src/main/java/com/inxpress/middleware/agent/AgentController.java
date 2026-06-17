package com.inxpress.middleware.agent;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agents")
public class AgentController {

    private final AgentCoordinator coordinator;

    public AgentController(AgentCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @GetMapping
    public ResponseEntity<List<String>> listAgents() {
        return ResponseEntity.ok(coordinator.getRegisteredAgents());
    }

    @PostMapping("/{name}/execute")
    public ResponseEntity<AgentResponse> executeAgent(
            @PathVariable String name,
            @RequestBody AgentRequest request) {
        AgentResponse response = coordinator.routeRequest(name, request);
        return ResponseEntity.ok(response);
    }
}
