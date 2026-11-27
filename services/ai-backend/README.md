# TraceLens AI Backend

## Prometheus configuration

The AI backend reads `PROMETHEUS_URL` when its Prometheus client is initialized.
If it is unset, the client defaults to `http://localhost:9090`.

- When running the AI backend directly on the host, use
  `PROMETHEUS_URL=http://localhost:9090` (the default for the repository's
  observability setup).
- When running the AI backend in Docker on the same Docker network as the
  Prometheus service, use `PROMETHEUS_URL=http://prometheus:9090`.
- When the containers are on separate networks, make Prometheus reachable from
  the AI backend container and configure that reachable address. With Docker
  Desktop, this is often `PROMETHEUS_URL=http://host.docker.internal:9090` when
  Prometheus publishes port 9090 on the host.

Set the variable before starting the AI backend. For example, in PowerShell on
the host:

```powershell
$env:PROMETHEUS_URL = "http://localhost:9090"
```

Or add `PROMETHEUS_URL` to the AI backend container's environment in its Docker
Compose service. The hostname `localhost` inside a container refers to that
container itself, so it should not be used to reach a Prometheus container.
