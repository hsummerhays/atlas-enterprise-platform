package com.inxpress.middleware.agent;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AgentMetricsService {

    private final AgentMetricsRepository repository;

    public AgentMetricsService(AgentMetricsRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AgentMetrics recordPrGenerated(String agentName, double estimatedTimeSavedMinutes) {
        AgentMetrics metrics = repository.findByAgentName(agentName)
                .orElseGet(() -> new AgentMetrics(agentName));

        metrics.setPrsGenerated(metrics.getPrsGenerated() + 1);
        metrics.setTimeSavedMinutes(metrics.getTimeSavedMinutes() + estimatedTimeSavedMinutes);
        
        // Dynamically compute cycle time reduction estimate based on time saved (e.g. 5% reduction per 60 minutes saved, capped at 95%)
        double cycleReduction = Math.min(95.0, metrics.getTimeSavedMinutes() * 0.08);
        metrics.setCycleTimeReductionPercentage(cycleReduction);

        return repository.save(metrics);
    }

    @Transactional
    public AgentMetrics recordPrAccepted(String agentName) {
        AgentMetrics metrics = repository.findByAgentName(agentName)
                .orElseGet(() -> new AgentMetrics(agentName));

        metrics.setPrsAccepted(metrics.getPrsAccepted() + 1);
        return repository.save(metrics);
    }

    @Transactional
    public AgentMetrics recordBugIntroduced(String agentName) {
        AgentMetrics metrics = repository.findByAgentName(agentName)
                .orElseGet(() -> new AgentMetrics(agentName));

        metrics.setBugsIntroduced(metrics.getBugsIntroduced() + 1);
        return repository.save(metrics);
    }

    public List<AgentMetrics> getAllMetrics() {
        return repository.findAll();
    }
}
