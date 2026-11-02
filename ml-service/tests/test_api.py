"""Unit and integration tests for FastAPI endpoints and Pydantic validation."""

from __future__ import annotations

import asyncio

import pytest
from pydantic import ValidationError

from app.main import health, model_info, predict, predictor
from app.models import HealthResponse, ModelInfoResponse, PredictRequest, PredictResponse


@pytest.fixture(scope="module", autouse=True)
def ensure_model_loaded():
    """Ensure ML predictor has model artifact loaded before running tests."""
    if not predictor.is_loaded:
        predictor.load()


def test_health_endpoint():
    """Test health endpoint returns UP and model_loaded true."""
    response = asyncio.run(health())
    assert isinstance(response, HealthResponse)
    assert response.status == "UP"
    assert response.model_loaded is True
    assert response.service == "ml-service"


def test_model_info_endpoint():
    """Test model-info endpoint returns training metrics and version."""
    response = asyncio.run(model_info())
    assert isinstance(response, ModelInfoResponse)
    assert response.model_version == "1.0.0"
    assert response.selected_model in ["LinearRegression", "RandomForestRegressor"]
    assert response.best_mae > 0.0
    assert "LinearRegression" in response.metrics
    assert "RandomForestRegressor" in response.metrics


def test_predict_endpoint_valid():
    """Test predict endpoint computes realistic kg CO2e estimate."""
    request = PredictRequest(
        category="T_SHIRT",
        weight_kg=0.22,
        main_material="COTTON",
        country="BD",
        transport_mode="SEA",
        distance_km=8000.0,
    )
    response = asyncio.run(predict(request))
    assert isinstance(response, PredictResponse)
    assert response.estimated_kg_co2e > 0.0
    assert response.model_version == "1.0.0"
    assert response.model_name in ["LinearRegression", "RandomForestRegressor"]


@pytest.mark.parametrize(
    "invalid_kwargs,expected_loc",
    [
        (
            {
                "category": "T_SHIRT",
                "weight_kg": -0.5,  # Invalid: negative weight
                "main_material": "COTTON",
                "country": "VN",
            },
            "weight_kg",
        ),
        (
            {
                "category": "T_SHIRT",
                "weight_kg": 0.0,  # Invalid: 0 weight
                "main_material": "COTTON",
                "country": "VN",
            },
            "weight_kg",
        ),
        (
            {
                "category": "T_SHIRT",
                "weight_kg": 0.25,
                "main_material": "COTTON",
                "country": "VIETNAM",  # Invalid: not 2-letter ISO code
            },
            "country",
        ),
    ],
)
def test_predict_request_validation_errors(invalid_kwargs, expected_loc):
    """Test PredictRequest validates boundary constraints and types."""
    with pytest.raises(ValidationError) as exc_info:
        PredictRequest(**invalid_kwargs)
    error_locs = [e["loc"][-1] for e in exc_info.value.errors()]
    assert expected_loc in error_locs
