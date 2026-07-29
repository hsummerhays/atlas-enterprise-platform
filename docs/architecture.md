# Atlas Enterprise Platform Architecture

System architecture design and structural layout.

## Sequence Diagram: Booking Flow

```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant Service
    participant Registry
    participant Adapter
    participant Carrier

    Client->>Controller: POST /api/v1/shipments/{id}/book
    Controller->>Service: bookShipment(id)
    Service->>Registry: getAdapter(carrier)
    Registry-->>Service: Return Adapter
    Service->>Adapter: bookShipment(shipment)
    Adapter->>Carrier: POST (FedEx/UPS/DHL/USPS API)
    Carrier-->>Adapter: Return tracking number
    Adapter-->>Service: Return booked shipment
    Service-->>Controller: Return Shipment
    Controller-->>Client: HTTP 200 OK
```

## Carrier Authentication Strategy

Carrier adapters (`FedExAdapter`, `UpsAdapter`, `DhlAdapter`, `UspsAdapter`) never resolve credentials
themselves. Each carrier's `CarrierConfiguration` (`authType`: `oauth`, `basic`, `apikey`, or `mtls`)
is passed to `AuthenticationFactory`, which returns the matching `CarrierAuthenticator`:

```mermaid
sequenceDiagram
    participant Adapter
    participant Factory as AuthenticationFactory
    participant Auth as CarrierAuthenticator

    Adapter->>Factory: createAuthenticator(CarrierConfiguration, tokenSupplier)
    Factory-->>Adapter: OAuthAuthenticator | BasicAuthenticator | ApiKeyAuthenticator | MtlsAuthenticator
    Adapter->>Auth: getAuthHeaders()
    Auth-->>Adapter: Map of auth headers
    Adapter->>Adapter: attach headers to carrier request
```

* **`oauth`** — `OAuthAuthenticator` wraps a `Supplier<String>` token source (see `FedExTokenService`,
  `UpsTokenService`, `UspsTokenService`), which caches and refreshes bearer tokens.
* **`basic`** — `BasicAuthenticator` resolves HTTP Basic credentials from `CarrierConfiguration`.
* **`apikey`** — `ApiKeyAuthenticator` sends the configured `apiKey` under a configurable header
  (defaults to `X-API-KEY`).
* **`mtls`** — `MtlsAuthenticator` signals that authentication is handled at the TLS layer (client
  certificates), contributing no auth headers.

Adapters validate required `CarrierConfiguration` fields up front and throw `CarrierAdapterException`
rather than falling back to blank/dummy credentials.

## AI Agent Layer

```mermaid
sequenceDiagram
    participant GH as GitHub Issue
    participant Webhook as GitHubWebhookController
    participant SQS as SQS agent-tasks-queue
    participant Consumer as SqsAgentTaskConsumer
    participant Coordinator as AgentCoordinator
    participant Agent as AIAgent (e.g. TestGenerationAgent)
    participant Claude as ClaudeService
    participant Audit as AgentAuditLogService (DynamoDB)

    GH->>Webhook: POST /api/v1/webhooks/github (issue opened)
    Webhook->>Webhook: verify X-Hub-Signature-256 HMAC-SHA256
    Webhook->>Webhook: resolve target agent from title/body keywords
    Webhook->>SQS: enqueue {agentName, inputData, requestedBy}
    Consumer->>SQS: long-poll (waitTimeSeconds=20)
    SQS-->>Consumer: task message
    Consumer->>Coordinator: runAgent(agentName, request)
    Coordinator->>Coordinator: load-shed if activeAgentCount > threshold
    Coordinator->>Agent: run(request)
    Agent->>Claude: analyze(systemPrompt, taskDescription)
    Claude-->>Agent: generated code/tests/docs
    Agent-->>Coordinator: AgentResult (prUrl, success, metadata)
    Coordinator->>Audit: logExecution(...)
    Coordinator-->>Consumer: AgentResult
```

* Agents are registered by Spring as `AIAgent` beans and keyed by lowercased simple class name
  (`documentationagent`, `testgenerationagent`, `refactoringagent`, `carrieradapteragent`,
  `securityreviewagent`, `pullrequestagent`).
* Each agent declares a required `ApprovalLevel` (`DOCUMENTATION`, `TESTS`, `FEATURE`, `SECURITY`,
  `ARCHITECTURE`); PRs requiring `SECURITY` or `ARCHITECTURE` approval trigger an SNS notification to
  `agent-review-topic-arn`.
* Messages that fail more than `maxReceiveCount` times are routed to the SQS Dead Letter Queue.
* `AgentCoordinator` rejects synchronous `/api/v1/agents/{name}/execute` calls with `HTTP 429` and
  defers SQS polling while `activeAgentCount` exceeds `app.load-shedding.threshold` (default `5`).

## Component Map

```
io.github.hsummerhays.atlas
├── adapter           FedEx / UPS / DHL / USPS carrier adapters + CarrierRegistry
│   └── auth           CarrierConfiguration, AuthenticationFactory, CarrierAuthenticator impls
├── agent              AIAgent implementations, AgentCoordinator, AgentController, SQS consumer
├── config             AWS, Claude, GitHub, and Spring Security configuration
├── controller         GitHubWebhookController
├── domain
│   ├── model           Canonical Address/Shipment/PackageDetail/Carrier/ShipmentStatus
│   ├── exception        CarrierAdapterException, LoadSheddingException, ShipmentNotFoundException
│   └── repository       ShipmentRepository (JPA)
├── service            ShipmentService, ClaudeService, GitHubIntegrationService, ShipmentEventPublisher
└── web                ShipmentController, GlobalExceptionHandler
```

> This file is also written by `DocumentationAgent` when triggered via a GitHub issue or the
> `/api/v1/agents/documentationagent/execute` endpoint; agent-generated updates go out for human
> review as a Pull Request rather than overwriting `main` directly.
