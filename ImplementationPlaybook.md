# InXpress Middleware: Proprietary Implementation Details & Code execution

*CONFIDENTIAL: For post-hire execution and reference.*

---

## 🛠️ Detailed Agent Architectures

### 1. The AIAgent & AgentTask Framework
We decouple code generation using a task execution contract, routing actions through a coordinator:
```java
public interface AgentTask {
    AgentResult execute();
}

public interface AIAgent {
    AgentResult run(AgentTask task);
}
```

### 2. Specialized Agent Definitions
* **`TestGenerationAgent`**: Writes functional JUnit integrations and runs coverage validation.
* **`CarrierAdapterAgent`**: Directly edits the filesystem `Carrier.java` enum and scaffolds classes (Properties, TokenServices, Adapters, Tests).
* **`PullRequestAgent`**: Spawns sub-processes, checks Gradle compiles, stages/commits, and calls PR endpoints.
* **`SecurityReviewAgent`**: Analyzes dependency CVEs and virtual thread locks.

---

## ⚡ Asynchronous Webhook & SQS Ingestion Details

### 1. Webhook Resolution Logic
We intercept GitHub Issues webhooks and dynamically map keyword heuristics (`test`, `doc`, `carrier`, `security`) to resolve the target agent queue payload.

### 2. SQS Consumer & Poller
Polling loops fetch JSON payloads from `agent-tasks-queue` on a 5-second interval:
```java
@Scheduled(fixedDelay = 5000)
public void pollAgentTaskQueue() { ... }
```

### 3. Claude LLM Integration (Adaptive Thinking)
We invoke Claude OkHttp Java SDK (`com.anthropic:anthropic-java`) and configure `ThinkingConfigAdaptive` to perform raw file edits, prompts processing, and mapping generation.

### 4. DynamoDB Execution Auditing
Logs execution metrics (`executionId`, `agentName`, `inputData`, `requestedBy`, `success`, `output`, `timestamp`) into an immutable `agent-execution-audit` table on every execution.

---

## 🎯 Post-Hire Implementation Roadmap

### Phase 1: Get ONE Agent Fully Working (The Baseline)
Establish a reliable, end-to-end flow for **one single agent** first.
* **Target Agent**: `TestGenerationAgent`
* **Trigger Input**: Generate tests for `ShipmentService`
* **Workflow Loop**:
  ```
  GitHub Issue ➔ Webhook ➔ SQS ➔ Claude ➔ Generate JUnit tests ➔ Create branch ➔ Commit ➔ Open PR (verified by CI/CD)
  ```
* **Why**: High safety, low risk of syntax bugs in core business logic, and high visual validation value.

### Phase 2: Add an Evaluation Loop (Self-Correction)
Enhance the reliability of the generated code before it hits remote source control.
* **Feedback Loop**:
  ```
  CodeGeneratorAgent ➔ ReviewAgent (Self-Correction) ➔ Local Build / Test ➔ GitHub PR
  ```
* **Why**: Prevents obvious compile or test failures from cluttering PR histories.

### Phase 3: Add Semantic Memory ("Engineering Memory")
Inject historical codebase standards, designs, and decisions directly into Claude's context window.
* **Architecture**: Leverage a **Vector Store** containing:
  - Previous successful PRs
  - Carrier specification documentation (FedEx/UPS/DHL)
  - Coding style guides and architectural guidelines
* **Why**: Produces highly context-aware adaptations that mimic human conventions.

### Phase 4: Add Policy Enforcement (Governance Guardrails)
Add a programmatic policy validation layer before PR submission:
* **Engine**: `PolicyEngine.validate(agentOutput)`
* **Gated Rules**:
  - Zero hardcoded secrets / API tokens
  - Zero direct production database interactions
  - Test coverage threshold validation (e.g. >85% via JaCoCo)
  - No introduced dependency vulnerabilities

### Phase 5: Add a PullRequestAgent (Flagship Demo)
Implement the flagship autonomic agent that coordinate multiple sub-agents to resolve feature tickets.
* **Loop**:
  ```
  GitHub Issue ➔ PullRequestAgent ➔ Sub-Agent delegation ➔ Consolidated PR
  ```

---

## ⚠️ What to Postpone (Out of Initial Scope)
Avoid excessive scope creep by keeping these items deferred:
1. **Multi-agent collaboration** (autonomous negotiations, consensus algorithms)
2. **Autonomous merging** (always gate merges behind senior human review)
3. **Direct production deployments**
4. **Full self-healing loops** in production runtimes

---

## 🎭 The "Wow" Demo Story for Shabab
* **Scenario**: Issue #42: "Add DHL rate cache"
* **Chronology**:
  - **10:00 PM**: AI agent automatically picks up the issue, generates the cache code, creates JUnit tests, and opens PR #57.
  - **08:00 AM**: Senior engineer reviews the PR, sees green CI builds/tests, and merges it with a single click.
* **Value Proposition**: Demonstrates a mature, safe developer-extension ecosystem rather than an unsafe, unmanaged AI coding tool.

