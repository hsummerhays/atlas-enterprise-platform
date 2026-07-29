package io.github.hsummerhays.atlas.agent;

import jakarta.persistence.*;

@Entity
@Table(name = "agent_metrics")
public class AgentMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_name", unique = true, nullable = false)
    private String agentName;

    @Column(name = "prs_generated", nullable = false)
    private int prsGenerated;

    @Column(name = "prs_accepted", nullable = false)
    private int prsAccepted;

    @Column(name = "time_saved_minutes", nullable = false)
    private double timeSavedMinutes;

    @Column(name = "bugs_introduced", nullable = false)
    private int bugsIntroduced;

    @Column(name = "cycle_time_reduction_percentage", nullable = false)
    private double cycleTimeReductionPercentage;

    public AgentMetrics() {}

    public AgentMetrics(String agentName) {
        this.agentName = agentName;
        this.prsGenerated = 0;
        this.prsAccepted = 0;
        this.timeSavedMinutes = 0.0;
        this.bugsIntroduced = 0;
        this.cycleTimeReductionPercentage = 0.0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public int getPrsGenerated() { return prsGenerated; }
    public void setPrsGenerated(int prsGenerated) { this.prsGenerated = prsGenerated; }

    public int getPrsAccepted() { return prsAccepted; }
    public void setPrsAccepted(int prsAccepted) { this.prsAccepted = prsAccepted; }

    public double getTimeSavedMinutes() { return timeSavedMinutes; }
    public void setTimeSavedMinutes(double timeSavedMinutes) { this.timeSavedMinutes = timeSavedMinutes; }

    public int getBugsIntroduced() { return bugsIntroduced; }
    public void setBugsIntroduced(int bugsIntroduced) { this.bugsIntroduced = bugsIntroduced; }

    public double getCycleTimeReductionPercentage() { return cycleTimeReductionPercentage; }
    public void setCycleTimeReductionPercentage(double cycleTimeReductionPercentage) { this.cycleTimeReductionPercentage = cycleTimeReductionPercentage; }

    @Transient
    public double getHumanAcceptanceRate() {
        if (prsGenerated == 0) return 0.0;
        return ((double) prsAccepted / prsGenerated) * 100.0;
    }
}
