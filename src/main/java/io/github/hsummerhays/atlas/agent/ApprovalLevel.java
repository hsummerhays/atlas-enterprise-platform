package io.github.hsummerhays.atlas.agent;

public enum ApprovalLevel {
    DOCUMENTATION,   // Auto-merge / low-risk
    TESTS,           // Auto-open / low-risk verification
    FEATURE,         // Requires peer review
    SECURITY,        // Gated: strict security review required
    ARCHITECTURE     // Gated: manual architecture board review required
}
