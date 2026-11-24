# TraceLens

TraceLens is an agentic AI platform for detecting production incidents, investigating their causes, recommending resolutions, and verifying recovery after human approval.

## Monorepo layout

```text
apps/
  web/                             React and TypeScript frontend
services/
  main-backend/                    Java 21 and Spring Boot APIs
  ai-backend/                      Python 3.12 and FastAPI agent workflows
  user-service/                    Simulated User Service
  order-service/                   Simulated Order Service
  payment-service/                 Simulated Payment Service
packages/
  api-contracts/                   Shared API and event contracts
  telemetry-conventions/           Shared telemetry conventions
infra/
  compose/                         Local Docker Compose support
  postgres/                        PostgreSQL and pgvector
  kafka/                           Incident events and asynchronous messaging
  observability/
    otel/                           OpenTelemetry configuration
    prometheus/                     Metrics
    loki/                           Logs
    tempo/                          Traces
    grafana/                        Observability dashboards
docs/
  architecture/
  api/
  runbooks/
.github/
  workflows/                       CI workflows
```

## Service responsibilities

- The simulated business services generate normal traffic and configurable failures.
- OpenTelemetry exports traces, metrics, and logs to Tempo, Prometheus, and Loki respectively.
- Kafka is used for incident events and asynchronous service communication, not telemetry collection.
- The Spring Boot backend owns REST APIs, authentication, and incident lifecycle and approval state.
- The FastAPI AI backend investigates incidents, retrieves relevant historical incidents, and returns recommendations.
- The frontend presents service health, incidents, investigation results, approvals, and recovery status.

## Local development

Docker Compose is the local development and integration environment. The repository will add Compose configuration as implementation proceeds. No application code or dependencies are included yet.
