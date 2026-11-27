"""LangGraph workflow for incident investigation."""

from langgraph.graph import END, StateGraph
from langgraph.types import RunnableConfig

from app.agents.state import InvestigationState


def investigate(state: InvestigationState) -> dict[str, object]:
    """Pass through the supplied incident context without asserting findings.

    This node is the integration point for future telemetry retrieval and
    analysis steps. Until those integrations exist, it deliberately leaves
    state unchanged.
    """

    return {}


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
