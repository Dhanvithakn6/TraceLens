import unittest
from unittest.mock import Mock, patch

import httpx

from app.integrations.prometheus import PrometheusClient


def prometheus_response(value):
    response = Mock()
    response.raise_for_status.return_value = None
    response.json.return_value = {
        "status": "success",
        "data": {"result": [] if value is None else [{"value": ["0", str(value)]}]},
    }
    return response


class PrometheusClientTests(unittest.TestCase):
    @patch("app.integrations.prometheus.httpx.get")
    def test_returns_available_service_metrics(self, get):
        get.side_effect = [
            prometheus_response(12.5),
            prometheus_response(0.02),
            prometheus_response(0.18),
        ]
        client = PrometheusClient("http://prometheus.test", timeout=1.5)

        metrics = client.get_service_metrics("checkout")

        self.assertEqual(
            metrics,
            [
                {"name": "request_rate", "value": 12.5, "unit": "requests/second"},
                {"name": "http_5xx_error_rate", "value": 0.02, "unit": "fraction"},
                {"name": "average_request_latency", "value": 0.18, "unit": "seconds"},
            ],
        )
        queries = [call.kwargs["params"]["query"] for call in get.call_args_list]
        self.assertTrue(all('{job="checkout"}' in query for query in (queries[0], queries[2])))
        self.assertIn('status=~"5.."', queries[1])
        self.assertTrue(all(call.kwargs["timeout"] == 1.5 for call in get.call_args_list))

    @patch("app.integrations.prometheus.httpx.get")
    def test_omits_metrics_missing_from_prometheus(self, get):
        get.side_effect = [prometheus_response(4), prometheus_response(None), prometheus_response(None)]

        metrics = PrometheusClient("http://prometheus.test").get_service_metrics("order-service")

        self.assertEqual(len(metrics), 1)
        self.assertEqual(metrics[0]["name"], "request_rate")

    @patch("app.integrations.prometheus.httpx.get")
    def test_query_errors_and_timeouts_are_skipped(self, get):
        get.side_effect = [
            httpx.TimeoutException("timed out"),
            prometheus_response(0.03),
            httpx.ConnectError("unavailable"),
        ]

        metrics = PrometheusClient("http://prometheus.test").get_service_metrics("payment-service")

        self.assertEqual(
            metrics,
            [{"name": "http_5xx_error_rate", "value": 0.03, "unit": "fraction"}],
        )

    @patch("app.integrations.prometheus.httpx.get")
    def test_prometheus_query_failure_is_skipped(self, get):
        failure = prometheus_response(None)
        failure.json.return_value = {"status": "error", "error": "bad query"}
        get.return_value = failure

        self.assertEqual(
            PrometheusClient("http://prometheus.test").get_service_metrics("main-backend"),
            [],
        )

    @patch("app.integrations.prometheus.httpx.get")
    def test_malformed_sample_values_are_omitted(self, get):
        malformed_payloads = [
            {"status": "success", "data": {"result": [{"value": ["0"]}]}},
            {"status": "success", "data": {"result": [{"value": "0.5"}]}},
            {"status": "success", "data": {"result": [{"value": []}]}},
        ]
        get.side_effect = [
            self.response_with_payload(payload) for payload in malformed_payloads
        ]

        metrics = PrometheusClient("http://prometheus.test").get_service_metrics(
            "checkout"
        )

        self.assertEqual(metrics, [])
        self.assertEqual(get.call_count, 3)

    @patch("app.integrations.prometheus.httpx.get")
    def test_incomplete_response_shapes_are_omitted(self, get):
        incomplete_payloads = [
            {"status": "success"},
            {"status": "success", "data": None},
            {"status": "success", "data": {"result": [{"other": "field"}]}},
        ]
        get.side_effect = [
            self.response_with_payload(payload) for payload in incomplete_payloads
        ]

        metrics = PrometheusClient("http://prometheus.test").get_service_metrics(
            "checkout"
        )

        self.assertEqual(metrics, [])
        self.assertEqual(get.call_count, 3)

    @staticmethod
    def response_with_payload(payload):
        response = Mock()
        response.raise_for_status.return_value = None
        response.json.return_value = payload
        return response


if __name__ == "__main__":
    unittest.main()
