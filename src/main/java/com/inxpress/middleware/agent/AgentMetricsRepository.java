package com.inxpress.middleware.agent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentMetricsRepository extends JpaRepository<AgentMetrics, Long> {
    Optional<AgentMetrics> findByAgentName(String agentName);
}
