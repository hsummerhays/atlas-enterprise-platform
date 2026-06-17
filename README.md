# InXpress ERP Shipping Middleware

A production-ready Shipping Middleware built using **Java 25** and **Spring Boot 4 / Jakarta EE 11** designed to standardize shipping operations for InXpress and integrate seamlessly with HubSpot CRM.

## 🎓 The Modern AI Software Development Loop
The InXpress middleware architecture is designed around the industry-moving developer operations paradigm:
```
[ Humans Define Intent ]
           │ (Issues, Spec Changes, Prompts)
           ▼
    [ AI Implements ]     (Scaffolds code, test files via AIAgents)
           │
           ▼
    [ CI Validates ]      (Automated Maven/Gradle tests & security checks)
           │
           ▼
   [ Humans Approve ]     (Manual review of PRs in GitHub)
           │
           ▼
   [ Cloud Deploys ]      (ECR Push, EKS deployment rollout)
```

## 🚀 Architecture Overview

This project implements a **Canonical Shipping Domain Model** to decouple client-facing business logic from carrier-specific payload definitions. 

```
[ Hubspot / Consumers ] 
       │ (REST APIs + JWT)
       ▼
[ Spring Security Resource Server ]
       │ 
       ▼
[ Shipment Service ]
       │ ──► [ Carrier Registry ] ──► [ Carrier Adapters ] ──► [ FedEx / UPS / DHL APIs ]
       ├──► [ RDS PostgreSQL ] (JPA & Flyway Migrations)
       └──► [ AWS SNS/SQS ] (Async Lifecycle Events)
```

### Key Design Patterns & Technical Features:
* **Canonical Domain Model**: Consolidates addresses, dimensions, weight metrics, and status enums across the systems.
* **Adapter Pattern**: Decouples and standardizes REST calls to **FedEx (OAuth 2.0)**, **UPS (OAuth 2.0)**, and **DHL Express (HTTP Basic Auth)**.
* **Flyway Migrations**: Clean, versioned schema management.
* **Spring Security OAuth 2.0**: Secures all APIs using JWT validation with custom claim-to-role mappings.
* **Modern Java Compilation**: Configured for Java 25 compiler with target compatibility release set to 21 for seamless Spring Boot ASM framework reading.

---

## 🛠️ Getting Started

### Prerequisites
* **JDK 25** (compiled output targets Java 21)
* **Gradle 9.4.1** (included wrapper script)

### Build and Test
To build the project and execute validation tests locally, run:
```bash
./gradlew build
```

### Run Locally
To run the Spring Boot server locally (by default on port `8080`):
```bash
./gradlew bootRun
```

---

## 🔌 API Documentation

| Method | Endpoint | Description | Required Role/Scope |
|---|---|---|---|
| `POST` | `/api/v1/shipments` | Creates a new shipment record | `ROLE_SHIPPING_USER` |
| `POST` | `/api/v1/shipments/{id}/book` | Books the shipment with the resolved carrier | `ROLE_SHIPPING_ADMIN` |
| `POST` | `/api/v1/shipments/rates` | Fetches a comparison of rate quotes | `SCOPE_shipments:read` |
| `GET` | `/api/v1/shipments/track/{trackingNumber}` | Tracks shipment status | `ROLE_SHIPPING_USER` |
| `POST` | `/api/v1/shipments/cancel/{trackingNumber}` | Cancels the shipment with the carrier | `ROLE_SHIPPING_ADMIN` |

---

## 🤖 AI Agent Layer (Highest Priority)

The middleware embeds an autonomic AI Agent Layer designed to automate developer operations, carrier research, and financial auditing tasks:

```
[ Agent Controller ] ──► [ Agent Coordinator ]
                                │
        ┌───────────────────────┼───────────────────────┐
        ▼                       ▼                       ▼
[DocumentationAgent]    [TestGenerationAgent]   [RefactoringAgent]
        ▼                                               ▼
[CarrierAdapterAgent]                           [SecurityReviewAgent]
```

### Agent Directory:
* **Documentation Agent**: Auto-generates, updates, and structures system architectural specifications and API markdown documentation.
* **Test Generation Agent**: Code inspector that writes JUnit integration and unit tests for carrier client interfaces.
* **Refactoring Agent**: Code scanner suggesting performance improvements (e.g., Spring Boot 4 virtual thread optimization rules).
* **Carrier Adapter Agent**: Automatically researches carrier REST specification updates and formats updates into canonical models.
* **Security Review Agent**: Performs automated security reviews, virtual thread scopes verification, and dependency auditing.

### REST Endpoints:
| Method | Endpoint | Description | Required Role |
|---|---|---|---|
| `GET` | `/api/v1/agents` | Lists all registered AI Agents | `ROLE_SHIPPING_ADMIN` |
| `POST` | `/api/v1/agents/{name}/execute` | Triggers a specific AI agent execution | `ROLE_SHIPPING_ADMIN` |

---

## 🌙 Nightly AI Workflow (10 PM Job)

Orchestrated using **AWS MWAA (Managed Workflows for Apache Airflow)**, the system triggers a nightly development and maintenance loop defined in [nightly_ai_workflow.py](file:///c:/HughApps/InXpressErpMiddleware/dags/nightly_ai_workflow.py):

1. **Review Carrier Adapters (`CarrierAdapterAgent`)**: Automatically scans and researches API specification updates from FedEx/UPS/DHL sandbox environments.
2. **Generate Missing Tests (`TestGenerationAgent`)**: Scans code diffs, identifies untested classes, and writes JUnit validation files.
3. **Update Documentation (`DocumentationAgent`)**: Updates the markdown documentation and code specs.
4. **Security & Refactoring Review (`SecurityReviewAgent`)**: Runs static code analysis, validates virtual thread scopes, and checks dependency vulnerabilities.
5. **Open Pull Requests**: Automatically stages modified files, opens PRs for code review, and alerts developers via Ops emails.

---

## ☁️ AWS & Infrastructure Integration

* **RDS (PostgreSQL)**: Main storage database.
* **SNS / SQS**: Broadcaster for lifecycle events (e.g. `SHIPMENT_BOOKED`, `SHIPMENT_CANCELLED`) to downstream systems.
* **Amazon DynamoDB**: High-throughput caching for carrier rate quotes and payload audit logs.
* **Amazon OpenSearch Service**: Powers fast, fuzzy queries over historical shipment metadata.
* **Amazon MWAA (Managed Airflow)**: Orchestrates daily reconciliation workflows between RDS and carrier invoices.
* **Kubernetes (AWS EKS)**: High availability multi-replica deployment configured under the `k8s/` directory.

---

## 🐙 GitHub Integration & Autonomous DevOps Loop

The middleware supports a fully automated coding and review loop by connecting to the **GitHub REST API** to execute tasks from ticket to deployment:

```
[ GitHub Issue / Webhook ]
           │
           ▼
     [ AI Agent ]
           │ (Fetch specifications / CarrierAdapterAgent)
           ▼
   [ Branch Creation ]  ◄── (GitHubIntegrationService: createBranch)
           │
           ▼
   [ Code Generation ]  ◄── (RefactoringAgent / TestGenerationAgent)
           │
           ▼
   [ Automated Tests ]  ◄── (JaCoCo Coverage & Checkstyle)
           │
           ▼
   [ Pull Request ]     ◄── (GitHubIntegrationService: createPullRequest)
```

### Supported integrations:
* **GitHub API Client**: Autonomic components invoke the [GitHubIntegrationService](file:///c:/HughApps/InXpressErpMiddleware/src/main/java/com/inxpress/middleware/service/GitHubIntegrationService.java) to dynamically query issues, build features on isolated branches, and push code suggestions.
* **Claude Code / GitHub MCP**: Future-proof integration designed to allow Claude Code agent instances to interact directly with EKS/RDS infrastructures using Model Context Protocol (MCP) tooling.
* **GitHub Actions**: Hooks agent-generated PR branches into the CI pipeline, automatically running JUnit suites and security analyzers on commit events.

---

## 🗺️ Roadmap: Phase 2 & Phase 3

### Phase 2: GitHub + AWS Hybrid Loop
Secures high-speed coordination between the GitHub workspace and runtime AWS environments:
* **GitHub Operations**: Manages source control, code generation, Pull Request reviews, and CI test orchestration.
* **AWS Deployments**: Executes EKS container runtime, SNS/SQS event publishing, RDS data storage, and CloudWatch observability.
* **Flow**: `Claude/AIAgent` ➔ `GitHub Branch` ➔ `PR Created` ➔ `GitHub Actions` ➔ `Docker Build` ➔ `ECR Push` ➔ `EKS Deploy`.

### Phase 3: Asynchronous AWS Automation (Zero-Touch Ops)
* **Queue-Driven Code Loop**: Pushes new issues or tasks onto an **AWS SQS** queue, automatically waking up an AI agent worker to generate code, run coverage checks, and publish GitHub PRs asynchronously.
* **Scheduled Self-Healing**: Uses **AWS EventBridge** schedules to run nightly code reviews at 10 PM. The agent automatically patches bugs and updates dependencies, leaving Pull Requests waiting for human approval in the morning.

---

## ⚖️ AI Governance & Security Controls

To ensure reliability, compliance, and prevent autonomous action drift, the AI Agent layer operates under strict governance guardrails:

* **Human-in-the-Loop (HITL) for PRs**: All code changes generated by agents (tests, documentation, refactorings) are pushed to isolated branches and require manual review and approval from senior engineers before merging to `main`.
* **Mandatory Security Scanning**: Every agent-triggered build initiates static application security testing (SAST) and software composition analysis (SCA) to detect code vulnerabilities.
* **Test Coverage Enforcement**: Code generated or modified by agents must meet or exceed a **85% test coverage threshold** verified automatically via JaCoCo in the build pipeline.
* **Secret Redaction**: API keys, credentials, and IAM tokens are injected at runtime via Kubernetes Secrets/AWS Secrets Manager and are strictly blacklisted from the agent context.
* **Tamper-Proof Audit Logging**: Every agent execution, input payload, and output recommendation is serialized and stored in an immutable **Amazon DynamoDB** table for audit compliance.

### 🛡️ AI Agent Permission Matrix
To maintain corporate security compliance, agent execution scopes are strictly restricted:

| Explicitly ALLOWED | Strictly FORBIDDEN |
| :--- | :--- |
| ✓ Create Git branches | ✗ Merge PRs to main / release branches |
| ✓ Commit modified code to feature branch | ✗ Deploy code directly to environments |
| ✓ Open Pull Requests | ✗ Modify/view environment secret keys |
| ✓ Post comment summaries on PR | ✗ Access production database tables directly |


---

## 🚢 CI/CD Pipeline
GitHub Actions automatically builds, compiles, tests, packages into a Docker image, pushes to Amazon ECR, and deploys/rolls out to AWS EKS on commits pushed to `main` branch.

