package io.github.hsummerhays.atlas.agent;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
            @Valid @RequestBody AgentWorkItemDto workItemDto) {

        AgentRequest request = new AgentRequest(
                workItemDto.inputData(),
                workItemDto.context() != null ? workItemDto.context() : Map.of(),
                "api"
        );

        AgentResult result = coordinator.runAgent(name, request);
        return ResponseEntity.ok(result);
    }

    public record AgentWorkItemDto(
        @NotBlank String inputData,
        Map<String, Object> context
    ) {}
}
