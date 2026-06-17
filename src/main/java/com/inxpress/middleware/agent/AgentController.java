package com.inxpress.middleware.agent;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/agents")
public class AgentController {

    private final AgentCoordinator coordinator;
    private final AgentMetricsService metricsService;

    public AgentController(AgentCoordinator coordinator, AgentMetricsService metricsService) {
        this.coordinator = coordinator;
        this.metricsService = metricsService;
    }

    @GetMapping
    public ResponseEntity<List<String>> listAgents() {
        return ResponseEntity.ok(coordinator.getRegisteredAgents());
    }

    @GetMapping("/metrics")
    public ResponseEntity<List<AgentMetrics>> getMetrics() {
        return ResponseEntity.ok(metricsService.getAllMetrics());
    }

    @PostMapping("/{name}/execute")
    public ResponseEntity<AgentResult> executeAgent(
            @PathVariable String name,
            @RequestBody AgentTaskDto taskDto) {
        
        // Map DTO to AgentTask execution behavior
        AgentTask task = () -> new AgentResult(
                name,
                true,
                "Executed task with input: " + taskDto.inputData(),
                taskDto.context() != null ? taskDto.context() : Map.of()
        );
        
        AgentResult result = coordinator.runAgent(name, task);
        return ResponseEntity.ok(result);
    }

    // Task payload payload mapping
    public record AgentTaskDto(
        String inputData,
        Map<String, Object> context
    ) {}
}
