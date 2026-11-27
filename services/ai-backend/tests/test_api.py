import unittest

from fastapi.testclient import TestClient

from app.main import app


class InvestigationApiTests(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(app)

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
