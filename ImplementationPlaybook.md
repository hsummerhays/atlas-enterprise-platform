# InXpress Middleware: Proprietary Implementation Details & Code execution

*CONFIDENTIAL: For post-hire execution and reference.*

---

## 🛠️ Detailed Agent Architectures

### 1. The AIAgent & AgentRequest Framework
We decouple agent execution by passing structured request contexts:
```java
public record AgentRequest(
    String inputData,
    Map<String, Object> context,
    String requestedBy
) {}

public interface AIAgent {
    AgentResult run(AgentRequest request);
}
```

### 2. Specialized Agent Definitions
* **`TestGenerationAgent`**: Writes functional JUnit integrations and runs coverage validation.
* **`CarrierAdapterAgent`**: Directly edits the filesystem `Carrier.java` enum and scaffolds classes (Properties, TokenServices, Adapters, Tests).
* **`PullRequestAgent`**: Spawns sub-processes, checks Gradle compiles, stages/commits, and calls PR endpoints.
* **`SecurityReviewAgent`**: Analyzes dependency CVEs and virtual thread locks.

---

### 1. Webhook Verification & Resolution Logic
We intercept GitHub Issues webhooks, verify payload authenticity using HMAC-SHA256 signatures via the `X-Hub-Signature-256` header, and dynamically map keyword heuristics (`test`, `doc`, `carrier`, `security`) to resolve the target agent queue payload.

### 2. SQS Consumer, Long-Polling & DLQ Routing
Polling loops fetch JSON payloads from `agent-tasks-queue` utilizing SQS Long-Polling (`waitTimeSeconds(20)`) to optimize request costs. If a message fails processing more than `maxReceiveCount` (default: 3) times, it is categorized as a poison message and routed to the Dead Letter Queue (DLQ):
```java
@Scheduled(fixedDelay = 5000)
public void pollAgentTaskQueue() { ... }
```

### 3. Claude LLM Integration (Adaptive Thinking)
We invoke Claude OkHttp Java SDK (`com.anthropic:anthropic-java`) and configure `ThinkingConfigAdaptive` to perform raw file edits, prompts processing, and mapping generation.
* **Dynamic Scaffolding**: Rather than using static placeholders, agents like `TestGenerationAgent`, `DocumentationAgent`, and `CarrierAdapterAgent` now dynamically consult Claude using dedicated system prompts to generate contextually relevant JUnit tests, OpenAPI specs, architecture write-ups, and carrier code files.
* **Enum Registration**: The `CarrierAdapterAgent` automatically updates the filesystem `Carrier.java` enum by locating the closing brace and appending new carrier identifiers dynamically.

### 4. DynamoDB Execution Auditing
Logs execution metrics (`executionId`, `agentName`, `approvalLevel`, `inputData`, `requestedBy`, `success`, `output`, `timestamp`) into an immutable `agent-execution-audit` table on every execution.

### 5. Automated Gating, Labels, & SNS Notifications
* **GitHub PR Labeling**: Pull Requests opened by agents are automatically labeled with their associated `ApprovalLevel` (e.g. `documentation`, `tests`, `feature`, `security`, `architecture`) using GitHub's issue labeling API.
* **SNS Gated Alerts**: When a PR is opened requiring human approval (e.g. `FEATURE`, `SECURITY`, `ARCHITECTURE`), a JSON alert is published to the `agent-review-topic-arn` SNS topic to notify administrators or trigger external approvals.

### 6. Load Shedding & Backpressure
To protect the system from resource exhaustion (e.g. Claude API rate limits, database lock contention, memory usage), we apply load shedding based on the active agent count:
* **Active Agent Tracking**: An `AtomicInteger` in `AgentCoordinator` tracks in-flight agent executions.
* **HTTP/API Rate Limiting (429)**: Executing agents via REST endpoints when the count exceeds the threshold throws a `LoadSheddingException`, which translates to an `HTTP 429 Too Many Requests` response.
* **SQS Polling Delay**: The SQS consumer checks the active count before polling. If it meets or exceeds the threshold, polling is skipped for that cycle, allowing messages to stay in SQS and avoiding receive count increments/DLQ routing.

### 7. Kubernetes Security Hardening & Actuator Probes
To satisfy corporate compliance and security requirements in EKS:
* **Hardened Security Context**: The container runs under non-root (UID/GID 1000) settings, blocks privilege escalation (`allowPrivilegeEscalation: false`), mounts a read-only root filesystem, and drops all Linux kernel capabilities.
* **Transient Mounts**: A `/tmp` emptyDir volume is mounted to allow local temporary file creation while preserving the read-only root filesystem constraint.
* **Spring Boot Actuator Probes**: Separate HTTP endpoints are exposed for liveness (`/actuator/health/liveness`) and readiness (`/actuator/health/readiness`) checks.
* **CI/CD Templating (`envsubst`)**: Environment configuration tags `${ECR_REGISTRY}`, `${IMAGE_TAG}`, and `${ACM_CERTIFICATE_ARN}` are injected into deployment templates in the CI runner.

### 8. Strict Carrier Account Configuration Validation
* **Security Controls**: Direct API adapters (FedEx, UPS, DHL) check for valid account number inputs before initiating carrier REST integrations.
* **Validation Guards**: In case of blank configuration properties, the adapters throw `CarrierAdapterException` immediately rather than allowing dummy defaults to escape to sandbox/production environments.

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
* **Value Proposition**: Demonstrates a mature, safe developer-extension ecosystem rather than an unsafe, unmanaged AI coding tool. The goal isn't autonomous coding—it's autonomous preparation. AI does the repetitive work while engineers focus on judgment and business outcomes.

---

## 💡 Future Architectural Evolutions

To scale the AI Agent layer into a production-grade enterprise system, we anticipate the following architectural evolutions:

### 1. Strongly Typed Context (`AgentContext`)
Transition from a generic `Map<String, Object> context` to a strongly typed record to guarantee contract safety:
```java
public record AgentContext(
    String repository,
    String branch,
    String issueNumber,
    String carrier,
    List<String> changedFiles
) {}
```

### 2. End-to-End Correlation IDs
Introduce correlation/execution tracking across boundaries (webhook trigger ➔ SQS message ➔ LLM generation ➔ PR commit):
```java
public record AgentRequest(
    String inputData,
    AgentContext context,
    String requestedBy,
    UUID correlationId,
    AgentPriority priority
) {}
```

### 3. Agent Confidence Scoring & Dynamic Thresholds
Enable agents to self-report confidence levels, driving automated routing strategies:
```java
public record AgentResult(
    String agentName,
    boolean success,
    double confidence, // Range: 0.0 to 1.0
    String output,
    Map<String, Object> metadata
) {}
```
* **High Confidence (e.g. >0.85)**: Auto-open Pull Request.
* **Low Confidence (e.g. <0.85)**: Create Draft PR or request review and request interactive developer feedback via Slack/Teams webhook.


