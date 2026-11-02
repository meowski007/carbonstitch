"""FastAPI service for garment carbon footprint estimation."""

from __future__ import annotations

import logging
from collections.abc import AsyncGenerator
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.models import HealthResponse, ModelInfoResponse, PredictRequest, PredictResponse
from app.predictor import FootprintPredictor

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
)
logger = logging.getLogger("ml-service")

predictor = FootprintPredictor()


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    """Startup and shutdown lifecycle."""
    logger.info("Initializing ML Service...")
    if not predictor.is_loaded:
        logger.warning("No pre-trained model found at startup; attempting training...")
        try:
            from app.data.train import train_and_evaluate

            train_and_evaluate()
            predictor.load()
            logger.info("Model trained and loaded successfully.")
        except Exception as e:
            logger.error("Failed to train model on startup: %s", e)
    else:
        logger.info(
            "Loaded model %s (v%s)",
            predictor.info.get("selected_model"),
            predictor.info.get("model_version"),
        )
    yield
    logger.info("Shutting down ML Service...")


app = FastAPI(
    title="Carbonstitch ML Service",
    description=(
        "Machine Learning inference microservice predicting garment carbon footprints (kg CO2e)"
    ),
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health", response_model=HealthResponse, tags=["Observability"])
async def health() -> HealthResponse:
    """Service health and model readiness probe."""
    return HealthResponse(
        status="UP" if predictor.is_loaded else "DEGRADED",
        service="ml-service",
        model_loaded=predictor.is_loaded,
    )


@app.get("/model-info", response_model=ModelInfoResponse, tags=["Model Info"])
async def model_info() -> ModelInfoResponse:
    """Retrieve model training metrics, validation scores, and feature metadata."""
    if not predictor.is_loaded:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Model metadata unavailable. Model has not been trained yet.",
        )
    info = predictor.info
    return ModelInfoResponse(
        model_version=info.get("model_version", "1.0.0"),
        selected_model=info.get("selected_model", "Unknown"),
        best_mae=info.get("best_mae", 0.0),
        metrics=info.get("metrics", {}),
        features=info.get("features", {}),
        metadata={
            "dataset_size": info.get("dataset_size"),
            "test_size": info.get("test_size"),
            "target": info.get("target"),
        },
    )


@app.post("/predict", response_model=PredictResponse, tags=["Inference"])
async def predict(request: PredictRequest) -> PredictResponse:
    """Predict garment carbon footprint in kg CO2e based on garment attributes."""
    if not predictor.is_loaded:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Model is not ready for inference.",
        )
    try:
        response = predictor.predict(request)
        logger.debug(
            "Predicted %s kg CO2e for category=%s weight=%s",
            response.estimated_kg_co2e,
            request.category,
            request.weight_kg,
        )
        return response
    except Exception as e:
        logger.error("Inference failure: %s", e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Inference computation error: {e!s}",
        ) from e


@app.exception_handler(Exception)
async def generic_exception_handler(request, exc):
    logger.exception("Unhandled error processing request %s", request.url)
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={"error": "INTERNAL_SERVER_ERROR", "detail": str(exc)},
    )
