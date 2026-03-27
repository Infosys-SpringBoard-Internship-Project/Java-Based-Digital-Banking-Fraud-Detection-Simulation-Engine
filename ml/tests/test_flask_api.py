"""
Unit tests for Flask ML API.
Tests health endpoint, predict endpoint, encoding logic, and error handling.
"""

import pytest
import json
from unittest.mock import Mock, patch, MagicMock
from pathlib import Path
import pandas as pd
import numpy as np
import sys

# Add parent directory to path to import flask_api
sys.path.insert(0, str(Path(__file__).parent.parent))
sys.path.insert(0, str(Path(__file__).parent.parent / "api"))

# Import flask_api - this will load the real models
import flask_api

# Create mock objects for testing
mock_model = MagicMock()
mock_rule_model = MagicMock()


@pytest.fixture
def client():
    """Create Flask test client"""
    flask_api.app.config["TESTING"] = True
    with flask_api.app.test_client() as client:
        yield client


@pytest.fixture(autouse=True)
def patch_models():
    """Patch the models in flask_api module for each test"""
    # Save original models
    original_model = flask_api.model
    original_rule_model = flask_api.rule_model

    # Reset mocks
    mock_model.reset_mock()
    mock_rule_model.reset_mock()

    # Set default mock returns - use numpy arrays to match sklearn behavior
    mock_model.predict_proba.return_value = np.array(
        [[0.25, 0.75]]
    )  # 75% fraud probability
    mock_rule_model.predict.return_value = np.array(
        [[1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0]]
    )

    # Replace models with mocks
    flask_api.model = mock_model
    flask_api.rule_model = mock_rule_model

    yield

    # Restore original models
    flask_api.model = original_model
    flask_api.rule_model = original_rule_model


@pytest.fixture
def mock_model_predict():
    """Return mock model references for explicit override in tests"""
    return mock_model, mock_rule_model


class TestHealthEndpoint:
    """Tests for /health endpoint"""

    def test_health_returns_status_running(self, client):
        """Should return status running"""
        response = client.get("/health")
        assert response.status_code == 200
        data = response.get_json()
        assert data["status"] == "running"

    def test_health_returns_model_info(self, client):
        """Should return model type information"""
        response = client.get("/health")
        data = response.get_json()
        assert data["model"] == "RandomForest"
        assert data["rule_model"] == "MultiLabel-RandomForest"

    def test_health_returns_artifacts_status(self, client):
        """Should return artifacts existence status"""
        response = client.get("/health")
        data = response.get_json()
        assert "artifacts" in data
        assert "fraud_model" in data["artifacts"]
        assert "rule_model" in data["artifacts"]
        assert "encoders" in data["artifacts"]


class TestPredictEndpoint:
    """Tests for /predict endpoint"""

    def test_predict_with_valid_transaction(self, client, mock_model_predict):
        """Should return fraud prediction for valid transaction"""
        transaction = {
            "amount": 150000.0,
            "balance": 50000.0,
            "txn_count_last_1hr": 2,
            "txn_count_last_24hr": 5,
            "avg_txn_amount_30days": 25000.0,
            "distance_from_last_txn_km": 50.0,
            "account_age_days": 365,
            "is_new_location": 0,
            "is_new_device": 0,
            "is_vpn_or_proxy": 1,
            "ip_matches_location": 1,
            "is_international": 0,
            "is_first_time_receiver": 0,
            "merchant_category": "electronics",
            "transaction_mode": "ONLINE",
            "location": "Mumbai",
            "ip_risk_tag": "VPN",
        }

        response = client.post(
            "/predict", data=json.dumps(transaction), content_type="application/json"
        )

        assert response.status_code == 200
        data = response.get_json()

        assert "fraud_probability" in data
        assert "ml_risk_level" in data
        assert "is_fraud_ml" in data
        assert "fraud_reason" in data
        assert "fired_rules" in data

    def test_predict_with_high_fraud_probability(self, client, mock_model_predict):
        """Should classify HIGH risk for probability >= 0.60"""
        mock_model.predict_proba.return_value = np.array([[0.25, 0.75]])  # 75% fraud

        transaction = {
            "amount": 200000.0,
            "balance": 100000.0,
        }

        response = client.post(
            "/predict", data=json.dumps(transaction), content_type="application/json"
        )

        data = response.get_json()
        assert data["fraud_probability"] == 0.75
        assert data["ml_risk_level"] == "HIGH"
        assert data["is_fraud_ml"] is True

    def test_predict_with_critical_fraud_probability(self, client, mock_model_predict):
        """Should classify CRITICAL risk for probability >= 0.80"""
        mock_model.predict_proba.return_value = np.array([[0.15, 0.85]])  # 85% fraud

        transaction = {"amount": 500000.0}

        response = client.post(
            "/predict", data=json.dumps(transaction), content_type="application/json"
        )

        data = response.get_json()
        assert data["fraud_probability"] == 0.85
        assert data["ml_risk_level"] == "CRITICAL"
        assert data["is_fraud_ml"] is True

    def test_predict_with_medium_risk(self, client, mock_model_predict):
        """Should classify MEDIUM risk for probability >= 0.40"""
        mock_model.predict_proba.return_value = np.array([[0.5, 0.5]])  # 50% fraud

        transaction = {"amount": 50000.0}

        response = client.post(
            "/predict", data=json.dumps(transaction), content_type="application/json"
        )

        data = response.get_json()
        assert data["fraud_probability"] == 0.5
        assert data["ml_risk_level"] == "MEDIUM"
        assert data["is_fraud_ml"] is False  # Only >= 0.60 is fraud

    def test_predict_with_low_risk(self, client, mock_model_predict):
        """Should classify NORMAL risk for probability < 0.40"""
        mock_model.predict_proba.return_value = np.array([[0.8, 0.2]])  # 20% fraud

        transaction = {"amount": 5000.0}

        response = client.post(
            "/predict", data=json.dumps(transaction), content_type="application/json"
        )

        data = response.get_json()
        assert data["fraud_probability"] == 0.2
        assert data["ml_risk_level"] == "NORMAL"
        assert data["is_fraud_ml"] is False

    def test_predict_with_invalid_json(self, client):
        """Should return 400 for invalid JSON"""
        response = client.post(
            "/predict", data="invalid json", content_type="application/json"
        )

        assert response.status_code == 400
        data = response.get_json()
        assert "error" in data
        assert "Invalid JSON" in data["error"]

    def test_predict_with_empty_body(self, client):
        """Should return 400 for empty body"""
        response = client.post("/predict", data="", content_type="application/json")

        assert response.status_code == 400

    def test_predict_with_missing_fields(self, client, mock_model_predict):
        """Should handle missing fields with defaults"""
        transaction = {}  # Empty transaction

        response = client.post(
            "/predict", data=json.dumps(transaction), content_type="application/json"
        )

        assert response.status_code == 200
        data = response.get_json()
        assert "fraud_probability" in data

    def test_predict_with_model_inference_error(self, client):
        """Should return 500 on model inference error"""
        mock_model.predict_proba.side_effect = Exception("Model inference failed")

        transaction = {"amount": 10000.0}

        response = client.post(
            "/predict", data=json.dumps(transaction), content_type="application/json"
        )

        # Restore normal behavior for other tests
        mock_model.predict_proba.side_effect = None
        mock_model.predict_proba.return_value = np.array([[0.2, 0.75]])

        assert response.status_code == 500
        data = response.get_json()
        assert "error" in data
        assert "ML inference failed" in data["error"]


class TestEncodingFunctions:
    """Tests for encoding helper functions"""

    def test_encode_with_known_value(self):
        """Should encode known categorical values"""
        mapping = {"retail": 0, "electronics": 1, "crypto": 2}
        assert flask_api.encode("retail", mapping) == 0
        assert flask_api.encode("electronics", mapping) == 1
        assert flask_api.encode("crypto", mapping) == 2

    def test_encode_with_unknown_value(self):
        """Should return default for unknown values"""
        mapping = {"retail": 0, "electronics": 1}
        assert flask_api.encode("unknown_category", mapping, default=99) == 99

    def test_to_float_with_valid_number(self):
        """Should convert valid numbers to float"""
        assert flask_api.to_float(123) == 123.0
        assert flask_api.to_float("456.78") == 456.78
        assert flask_api.to_float(0) == 0.0

    def test_to_float_with_invalid_value(self):
        """Should return default for invalid values"""
        assert flask_api.to_float("invalid", default=0.0) == 0.0
        assert flask_api.to_float(None, default=1.5) == 1.5

    def test_to_int_with_valid_number(self):
        """Should convert valid numbers to int"""
        assert flask_api.to_int(123) == 123
        assert flask_api.to_int("456") == 456
        assert flask_api.to_int(0) == 0

    def test_to_int_with_invalid_value(self):
        """Should return default for invalid values"""
        assert flask_api.to_int("invalid", default=0) == 0
        assert flask_api.to_int(None, default=5) == 5


class TestFraudReasonBuilder:
    """Tests for build_fraud_reason function"""

    def test_build_fraud_reason_with_high_amount(self):
        """Should build reason for high amount rule (r01) under 100k"""
        flags = [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]  # Only r01 triggered
        data = {"amount": 75000.0}  # Under 100k = HighAmount

        reason = flask_api.build_fraud_reason(flags, data)
        assert "R01:HighAmount" in reason

    def test_build_fraud_reason_with_critical_amount(self):
        """Should build reason for critical amount >= 100k"""
        flags = [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        data = {"amount": 500000.0}

        reason = flask_api.build_fraud_reason(flags, data)
        assert "R01:CriticalAmount" in reason

    def test_build_fraud_reason_with_vpn_detection(self):
        """Should build reason for VPN/proxy detection (r08)"""
        flags = [0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0]  # Only r08 triggered
        data = {"is_vpn_or_proxy": 1, "ip_risk_tag": "VPN"}

        reason = flask_api.build_fraud_reason(flags, data)
        assert "R08a:VPN/ProxyFlag" in reason or "R08c:CommercialVPN" in reason

    def test_build_fraud_reason_with_tor_network(self):
        """Should build reason for TOR network detection"""
        flags = [0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0]
        data = {"ip_risk_tag": "TOR"}

        reason = flask_api.build_fraud_reason(flags, data)
        assert "R08c:TorNetwork" in reason

    def test_build_fraud_reason_with_frequent_transactions(self):
        """Should build reason for frequent transactions (r04)"""
        flags = [0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]  # Only r04 triggered
        data = {"txn_count_last_1hr": 10}

        reason = flask_api.build_fraud_reason(flags, data)
        assert "R04:" in reason
        assert "10txns/hr" in reason

    def test_build_fraud_reason_with_balance_drain(self):
        """Should build reason for balance drain (r03)"""
        flags = [0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]  # Only r03 triggered
        data = {"amount": 90000.0, "balance": 100000.0}

        reason = flask_api.build_fraud_reason(flags, data)
        assert "R03:BalanceDrain" in reason

    def test_build_fraud_reason_with_multiple_rules(self):
        """Should combine multiple triggered rules"""
        flags = [1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0]  # r01 and r08
        data = {"amount": 200000.0, "ip_risk_tag": "VPN"}

        reason = flask_api.build_fraud_reason(flags, data)
        assert "R01:" in reason
        assert "R08" in reason
        assert "|" in reason  # Multiple rules separated by |

    def test_build_fraud_reason_with_no_rules(self):
        """Should return 'None' when no rules triggered"""
        flags = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        data = {}

        reason = flask_api.build_fraud_reason(flags, data)
        assert reason == "None"

    def test_build_fraud_reason_with_crypto_merchant(self):
        """Should detect crypto merchant (r05)"""
        flags = [0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0]  # Only r05
        data = {"merchant_category": "crypto"}

        reason = flask_api.build_fraud_reason(flags, data)
        assert "R05:CryptoMerchant" in reason

    def test_build_fraud_reason_with_impossible_travel(self):
        """Should detect impossible travel (r06)"""
        flags = [0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0]  # Only r06
        data = {"distance_from_last_txn_km": 1500.0}

        reason = flask_api.build_fraud_reason(flags, data)
        assert (
            "R06:ImpossibleTravel" in reason or "R06:SuspiciousLocationJump" in reason
        )


class TestFeatureVectorConstruction:
    """Tests for feature vector construction"""

    def test_feature_vector_order_matches_training(self, client, mock_model_predict):
        """Should construct feature vector in correct order"""
        transaction = {
            "amount": 100000.0,
            "balance": 50000.0,
            "txn_count_last_1hr": 3,
            "txn_count_last_24hr": 10,
            "avg_txn_amount_30days": 20000.0,
            "distance_from_last_txn_km": 100.0,
            "account_age_days": 180,
            "is_new_location": 1,
            "is_new_device": 0,
            "is_vpn_or_proxy": 1,
            "ip_matches_location": 0,
            "is_international": 1,
            "is_first_time_receiver": 0,
            "merchant_category": "retail",
            "transaction_mode": "UPI",
            "location": "Delhi",
            "ip_risk_tag": "CLEAN",
        }

        response = client.post(
            "/predict", data=json.dumps(transaction), content_type="application/json"
        )

        assert response.status_code == 200
        # Verify model was called with DataFrame
        assert mock_model.predict_proba.called


class TestRuleFlagPrediction:
    """Tests for rule flag prediction"""

    def test_rule_flags_returned_in_response(self, client, mock_model_predict):
        """Should return fired rules in response"""
        transaction = {"amount": 150000.0}

        response = client.post(
            "/predict", data=json.dumps(transaction), content_type="application/json"
        )

        data = response.get_json()
        assert "fired_rules" in data
        assert isinstance(data["fired_rules"], dict)

    def test_rule_flags_match_prediction(self, client, mock_model_predict):
        """Should match rule model predictions"""
        # Mock rule model returns r01=1, r08=1
        mock_rule_model.predict.return_value = np.array(
            [[1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0]]
        )

        transaction = {"amount": 150000.0}

        response = client.post(
            "/predict", data=json.dumps(transaction), content_type="application/json"
        )

        data = response.get_json()
        assert data["fired_rules"]["r01"] == 1
        assert data["fired_rules"]["r08"] == 1
        assert data["fired_rules"]["r02"] == 0


class TestEdgeCases:
    """Tests for edge cases and boundary conditions"""

    def test_predict_with_zero_amounts(self, client, mock_model_predict):
        """Should handle zero amount transactions"""
        transaction = {"amount": 0.0, "balance": 0.0}

        response = client.post(
            "/predict", data=json.dumps(transaction), content_type="application/json"
        )

        assert response.status_code == 200

    def test_predict_with_negative_values(self, client, mock_model_predict):
        """Should handle negative values gracefully"""
        transaction = {"amount": -1000.0, "balance": -500.0}

        response = client.post(
            "/predict", data=json.dumps(transaction), content_type="application/json"
        )

        assert response.status_code == 200

    def test_predict_with_very_large_values(self, client, mock_model_predict):
        """Should handle very large values"""
        transaction = {"amount": 999999999.99, "balance": 999999999.99}

        response = client.post(
            "/predict", data=json.dumps(transaction), content_type="application/json"
        )

        assert response.status_code == 200

    def test_predict_with_string_numbers(self, client, mock_model_predict):
        """Should convert string numbers to floats"""
        transaction = {"amount": "150000.50", "balance": "50000"}

        response = client.post(
            "/predict", data=json.dumps(transaction), content_type="application/json"
        )

        assert response.status_code == 200

    def test_predict_with_unknown_merchant(self, client, mock_model_predict):
        """Should handle unknown merchant category"""
        transaction = {"merchant_category": "unknown_category_xyz"}

        response = client.post(
            "/predict", data=json.dumps(transaction), content_type="application/json"
        )

        assert response.status_code == 200

    def test_predict_with_unknown_location(self, client, mock_model_predict):
        """Should handle unknown location"""
        transaction = {"location": "UnknownCity123"}

        response = client.post(
            "/predict", data=json.dumps(transaction), content_type="application/json"
        )

        assert response.status_code == 200


class TestResponseFormat:
    """Tests for response format consistency"""

    def test_predict_response_has_required_fields(self, client, mock_model_predict):
        """Should return all required fields in predict response"""
        transaction = {"amount": 100000.0}

        response = client.post(
            "/predict", data=json.dumps(transaction), content_type="application/json"
        )

        data = response.get_json()

        required_fields = [
            "fraud_probability",
            "ml_risk_level",
            "is_fraud_ml",
            "fraud_reason",
            "fired_rules",
        ]

        for field in required_fields:
            assert field in data, f"Missing required field: {field}"

    def test_fraud_probability_is_float(self, client, mock_model_predict):
        """Should return fraud_probability as float"""
        transaction = {"amount": 100000.0}

        response = client.post(
            "/predict", data=json.dumps(transaction), content_type="application/json"
        )

        data = response.get_json()
        assert isinstance(data["fraud_probability"], float)
        assert 0.0 <= data["fraud_probability"] <= 1.0

    def test_risk_level_is_valid_enum(self, client, mock_model_predict):
        """Should return valid risk level"""
        transaction = {"amount": 100000.0}

        response = client.post(
            "/predict", data=json.dumps(transaction), content_type="application/json"
        )

        data = response.get_json()
        valid_levels = ["NORMAL", "MEDIUM", "HIGH", "CRITICAL"]
        assert data["ml_risk_level"] in valid_levels

    def test_is_fraud_ml_is_boolean(self, client, mock_model_predict):
        """Should return is_fraud_ml as boolean"""
        transaction = {"amount": 100000.0}

        response = client.post(
            "/predict", data=json.dumps(transaction), content_type="application/json"
        )

        data = response.get_json()
        assert isinstance(data["is_fraud_ml"], bool)

    def test_fired_rules_is_dict(self, client, mock_model_predict):
        """Should return fired_rules as dict"""
        transaction = {"amount": 100000.0}

        response = client.post(
            "/predict", data=json.dumps(transaction), content_type="application/json"
        )

        data = response.get_json()
        assert isinstance(data["fired_rules"], dict)
        # Should have 14 rule columns
        assert len(data["fired_rules"]) == 14
