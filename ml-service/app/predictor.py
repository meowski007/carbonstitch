"""Model loader and inference engine for carbon footprint prediction."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

import joblib
import pandas as pd

from app.models import PredictRequest, PredictResponse


class FootprintPredictor:
    """Encapsulates scikit-learn model inference with artifact caching."""

    def __init__(self, artifacts_dir: Path | None = None) -> None:
        if artifacts_dir is None:
            artifacts_dir = Path(__file__).resolve().parents[1] / "artifacts"
        self.artifacts_dir = artifacts_dir
        self.model_path = artifacts_dir / "model.joblib"
        self.info_path = artifacts_dir / "model_info.json"
        self._pipeline: Any = None
        self._info: dict[str, Any] = {}
        self.load()

    def load(self) -> None:
        """Load trained pipeline and metadata from disk."""
        if self.model_path.exists():
            self._pipeline = joblib.load(self.model_path)
        if self.info_path.exists():
            with open(self.info_path, encoding="utf-8") as f:
                self._info = json.load(f)

    @property
    def is_loaded(self) -> bool:
        return self._pipeline is not None

    @property
    def info(self) -> dict[str, Any]:
        return self._info

    def predict(self, req: PredictRequest) -> PredictResponse:
        """Run ML prediction on the garment request."""
        if not self.is_loaded:
            raise RuntimeError("ML model artifact not loaded. Train the model first.")

        df = pd.DataFrame(
            [
                {
                    "category": req.category.upper(),
                    "weight_kg": float(req.weight_kg),
                    "main_material": req.main_material.upper(),
                    "country": req.country.upper(),
                    "transport_mode": req.transport_mode.upper(),
                    "distance_km": float(req.distance_km),
                }
            ]
        )

        pred = float(self._pipeline.predict(df)[0])
        # Footprint cannot be negative
        estimated_co2e = max(0.05, round(pred, 3))

        return PredictResponse(
            estimated_kg_co2e=estimated_co2e,
            model_version=self._info.get("model_version", "1.0.0"),
            model_name=self._info.get("selected_model", "RandomForestRegressor"),
        )
