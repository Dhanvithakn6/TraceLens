import unittest
from unittest.mock import patch

from fastapi.testclient import TestClient

from app.main import app


class InvestigationApiTests(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(app)
        self.metrics_patch = patch(
            "app.agents.workflow.prometheus_client.get_service_metrics",
            return_value=[],
        )
        self.metrics_patch.start()

    def tearDown(self):
        self.metrics_patch.stop()

    def test_create_investigation_returns_structured_state(self):
        payload = {
            "incident_id": "inc-123",
            "service_name": "checkout",
            "severity": "high",
            "description": "Elevated checkout errors",
        }

        response = self.client.post("/api/investigations", json=payload)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            response.json(),
            {
                "incident_details": payload,
                "logs": [],
                "metrics": [],
                "traces": [],
                "findings": [],
                "probable_root_cause": None,
                "recommendations": [],
            },
        )

    def test_create_investigation_rejects_invalid_input(self):
        payload = {
            "incident_id": "   ",
            "service_name": "checkout",
            "severity": "high",
            "description": "Elevated checkout errors",
        }

        response = self.client.post("/api/investigations", json=payload)

        self.assertEqual(response.status_code, 422)

    def test_create_investigation_requires_all_fields(self):
        response = self.client.post(
            "/api/investigations",
            json={"incident_id": "inc-123", "service_name": "checkout"},
        )

        self.assertEqual(response.status_code, 422)


if __name__ == "__main__":
    unittest.main()
