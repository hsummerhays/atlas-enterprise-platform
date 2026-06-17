package com.inxpress.middleware.agent;

public interface AIAgent {
    AgentResult run(AgentRequest request);
    
    default ApprovalLevel getRequiredApprovalLevel() {
        return ApprovalLevel.FEATURE; // Default fallback level
    }
}
