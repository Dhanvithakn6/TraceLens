"""Shared state passed between incident investigation workflow nodes."""

from typing import Any, TypedDict


class InvestigationState(TypedDict, total=False):
    """Input and output data for an incident investigation.

    Fields are optional so callers can supply only the data they have. The
    values are intentionally provider-agnostic; adapters for Prometheus,
    Loki, and Tempo can populate telemetry fields in later workflow nodes.
    """

    incident_details: dict[str, Any]
    logs: list[Any]
    metrics: list[Any]
    traces: list[Any]
    findings: list[str]
    probable_root_cause: str | None
    recommendations: list[str]
