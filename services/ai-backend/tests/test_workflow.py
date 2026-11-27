import unittest
from unittest.mock import patch

from app.agents.workflow import investigation_graph, run_investigation


class InvestigationWorkflowTests(unittest.TestCase):
    def test_compiled_graph_preserves_supplied_state(self):
        initial_state = {
            "incident_details": {"id": "incident-1", "service": "checkout"},
            "logs": [{"message": "provided log"}],
            "metrics": [{"name": "request_rate", "value": 12}],
            "traces": [{"trace_id": "trace-1"}],
            "findings": ["caller supplied finding"],
            "probable_root_cause": None,
            "recommendations": [],
        }

        self.assertEqual(investigation_graph.invoke(initial_state), initial_state)

    def test_run_investigation_does_not_invent_findings(self):
        initial_state = {"incident_details": {"id": "incident-2"}}

        self.assertEqual(run_investigation(initial_state), initial_state)

    @patch("app.agents.workflow.prometheus_client.get_service_metrics")
    def test_workflow_populates_metrics_for_incident_service(self, get_metrics):
        get_metrics.return_value = [
            {"name": "request_rate", "value": 2.5, "unit": "requests/second"}
        ]
        initial_state = {
            "incident_details": {"service_name": "checkout"},
            "metrics": [],
        }

        result = run_investigation(initial_state)

        get_metrics.assert_called_once_with("checkout")
        self.assertEqual(result["metrics"], get_metrics.return_value)


if __name__ == "__main__":
    unittest.main()
