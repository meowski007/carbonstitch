"""Train and compare ML models for garment carbon footprint prediction."""

from __future__ import annotations

import json
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import RandomForestRegressor
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

from app.data.generate import generate_synthetic_dataset

CATEGORICAL_FEATURES = ["category", "main_material", "country", "transport_mode"]
NUMERICAL_FEATURES = ["weight_kg", "distance_km"]
TARGET_COLUMN = "total_kg_co2e"
MODEL_VERSION = "1.0.0"


def build_preprocessor() -> ColumnTransformer:
    """Construct feature preprocessing pipeline."""
    return ColumnTransformer(
        transformers=[
            (
                "num",
                StandardScaler(),
                NUMERICAL_FEATURES,
            ),
            (
                "cat",
                OneHotEncoder(handle_unknown="ignore", sparse_output=False),
                CATEGORICAL_FEATURES,
            ),
        ]
    )


def train_and_evaluate(
    data_path: Path | None = None,
    artifacts_dir: Path | None = None,
    random_state: int = 42,
) -> dict:
    """Train LinearRegression vs RandomForestRegressor, compare MAE, and save best model."""
    if artifacts_dir is None:
        artifacts_dir = Path(__file__).resolve().parents[2] / "artifacts"
    artifacts_dir.mkdir(parents=True, exist_ok=True)

    if data_path is None or not data_path.exists():
        data_path = artifacts_dir / "synthetic_garments.csv"
        generate_synthetic_dataset(n_samples=5000, random_seed=random_state, output_path=data_path)

    df = pd.read_csv(data_path)
    X = df[CATEGORICAL_FEATURES + NUMERICAL_FEATURES]
    y = df[TARGET_COLUMN]

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=random_state
    )

    candidate_models = {
        "LinearRegression": LinearRegression(),
        "RandomForestRegressor": RandomForestRegressor(
            n_estimators=40, max_depth=8, random_state=random_state, n_jobs=1
        ),
    }

    results = {}
    best_model_name = None
    best_mae = float("inf")
    best_pipeline = None

    for name, regressor in candidate_models.items():
        pipeline = Pipeline(
            steps=[
                ("preprocessor", build_preprocessor()),
                ("regressor", regressor),
            ]
        )

        pipeline.fit(X_train, y_train)
        y_pred = pipeline.predict(X_test)

        mae = float(mean_absolute_error(y_test, y_pred))
        rmse = float(np.sqrt(mean_squared_error(y_test, y_pred)))
        r2 = float(r2_score(y_test, y_pred))

        results[name] = {
            "mae": round(mae, 4),
            "rmse": round(rmse, 4),
            "r2": round(r2, 4),
        }

        print(f"Model: {name} -> MAE: {mae:.4f}, RMSE: {rmse:.4f}, R2: {r2:.4f}")

        if mae < best_mae:
            best_mae = mae
            best_model_name = name
            best_pipeline = pipeline

    model_path = artifacts_dir / "model.joblib"
    joblib.dump(best_pipeline, model_path, compress=3)
    print(f"Saved best model ({best_model_name}) with MAE {best_mae:.4f} to {model_path}")

    model_info = {
        "model_version": MODEL_VERSION,
        "selected_model": best_model_name,
        "best_mae": round(best_mae, 4),
        "metrics": results,
        "dataset_size": len(df),
        "test_size": len(y_test),
        "features": {
            "categorical": CATEGORICAL_FEATURES,
            "numerical": NUMERICAL_FEATURES,
        },
        "target": TARGET_COLUMN,
    }

    info_path = artifacts_dir / "model_info.json"
    with open(info_path, "w", encoding="utf-8") as f:
        json.dump(model_info, f, indent=2)

    return model_info


if __name__ == "__main__":
    train_and_evaluate()
