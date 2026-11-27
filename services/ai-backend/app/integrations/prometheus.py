"""Small client for service-level metrics from the Prometheus HTTP API."""

import logging
import os
from typing import Any

import httpx

logger = logging.getLogger(__name__)


class PrometheusClient:
    """Fetch request rate, 5xx fraction, and average latency for one service."""

    def __init__(self, base_url: str | None = None, timeout: float = 3.0) -> None:
        configured_url = (
            base_url
            if base_url is not None
            else os.getenv("PROMETHEUS_URL", "http://localhost:9090")
        )
        self.base_url = configured_url.strip().rstrip("/")
        self.timeout = timeout

    def get_service_metrics(self, service_name: str) -> list[dict[str, Any]]:
        """Return available metrics; unavailable values are omitted."""

        if not self.base_url or not service_name.strip():
            return []

        # Escape PromQL string-label contents so service names cannot alter a query.
        escaped_service = service_name.replace("\\", "\\\\").replace('"', '\\"')
        selector = f'job="{escaped_service}"'
        queries = (
            (
                "request_rate",
                f"sum(rate(http_server_requests_seconds_count{{{selector}}}[5m]))",
                "requests/second",
            ),
            (
                "http_5xx_error_rate",
                f"sum(rate(http_server_requests_seconds_count{{{selector},status=~\"5..\"}}[5m])) "
                f"/ sum(rate(http_server_requests_seconds_count{{{selector}}}[5m]))",
                "fraction",
            ),
            (
                "average_request_latency",
                f"sum(rate(http_server_requests_seconds_sum{{{selector}}}[5m])) "
                f"/ sum(rate(http_server_requests_seconds_count{{{selector}}}[5m]))",
                "seconds",
            ),
        )
        metrics: list[dict[str, Any]] = []
        for name, query, unit in queries:
            value = self._query_value(query)
            if value is not None:
                metrics.append({"name": name, "value": value, "unit": unit})
        return metrics

    def _query_value(self, query: str) -> float | None:
        try:
            response = httpx.get(
                f"{self.base_url}/api/v1/query",
                params={"query": query},
                timeout=self.timeout,
            )
            response.raise_for_status()
            payload = response.json()
            if not isinstance(payload, dict) or payload.get("status") != "success":
                return None

            data = payload.get("data")
            if not isinstance(data, dict):
                return None

            results = data.get("result")
            if not isinstance(results, list) or not results:
                return None

            sample = results[0]
            if not isinstance(sample, dict):
                return None

            sample_value = sample.get("value")
            if not isinstance(sample_value, (list, tuple)) or len(sample_value) != 2:
                return None

            raw_value = sample_value[1]
            value = float(raw_value)
            # Prometheus can return NaN/Inf when the denominator is zero.
            if value != value or value in (float("inf"), float("-inf")):
                return None
            return value
        except (httpx.HTTPError, ValueError, TypeError, KeyError, AttributeError) as exc:
            logger.info("Prometheus query unavailable: %s", exc)
            return None


prometheus_client = PrometheusClient()
