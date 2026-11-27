import unittest

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


if __name__ == "__main__":
    unittest.main()
