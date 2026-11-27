"""LangGraph workflow for incident investigation."""

from langgraph.graph import END, StateGraph
from langgraph.types import RunnableConfig

from app.agents.state import InvestigationState
from app.integrations.prometheus import prometheus_client


def investigate(state: InvestigationState) -> dict[str, object]:
    """Collect available service metrics without asserting findings.

    Prometheus is queried only when the incident contains a service name.
    Other telemetry and analysis fields remain untouched for future nodes.
    """

    incident_details = state.get("incident_details", {})
    service_name = incident_details.get("service_name")
    if not service_name:
        return {}
    return {"metrics": prometheus_client.get_service_metrics(str(service_name))}


def build_investigation_graph():
    """Build and compile the incident investigation workflow."""

    graph = StateGraph(InvestigationState)
    graph.add_node("investigate", investigate)
    graph.set_entry_point("investigate")
    graph.add_edge("investigate", END)
    return graph.compile()


# Compile once for reuse across requests.
investigation_graph = build_investigation_graph()


def run_investigation(
    initial_state: InvestigationState,
    config: RunnableConfig | None = None,
) -> InvestigationState:
    """Invoke the compiled workflow with caller-provided investigation data."""

    return investigation_graph.invoke(initial_state, config=config)
