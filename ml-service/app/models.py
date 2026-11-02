"""Pydantic schemas for ML Service API."""

from __future__ import annotations

from typing import Any

from pydantic import BaseModel, Field


class PredictRequest(BaseModel):
    category: str = Field(
        ...,
        description="Garment category, e.g. T_SHIRT, JEANS, SWEATER, JACKET, DRESS, SHIRT",
        examples=["T_SHIRT"],
    )
    weight_kg: float = Field(
        ...,
        gt=0.0,
        le=50.0,
        description="Garment weight in kilograms",
        examples=[0.25],
    )
    main_material: str = Field(
        ...,
        description="Primary material, e.g. COTTON, POLYESTER, WOOL, NYLON, SILK, VISCOSE",
        examples=["COTTON"],
    )
    country: str = Field(
        ...,
        min_length=2,
        max_length=2,
        description="ISO 2-letter country code of manufacturing facility",
        examples=["VN"],
    )
    transport_mode: str = Field(
        default="SEA",
        description="Inbound freight transport mode: AIR, SEA, ROAD, RAIL",
        examples=["SEA"],
    )
    distance_km: float = Field(
        default=5000.0,
        ge=0.0,
        le=50000.0,
        description="Transport shipping distance in kilometers",
        examples=[8500.0],
    )


class PredictResponse(BaseModel):
    estimated_kg_co2e: float = Field(
        ...,
        description="Predicted total carbon footprint in kilograms of CO2 equivalent",
    )
    model_version: str = Field(..., description="Semantic version of the deployed model")
    model_name: str = Field(..., description="Underlying model architecture")


class HealthResponse(BaseModel):
    status: str = Field(..., examples=["UP"])
    service: str = Field(default="ml-service")
    model_loaded: bool = Field(...)


class ModelInfoResponse(BaseModel):
    model_version: str
    selected_model: str
    best_mae: float
    metrics: dict[str, dict[str, float]]
    features: dict[str, list[str]]
    metadata: dict[str, Any] | None = None
