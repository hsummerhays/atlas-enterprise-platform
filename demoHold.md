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
