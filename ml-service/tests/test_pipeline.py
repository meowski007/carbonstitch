"""Tests for synthetic data generation and training pipeline."""

from __future__ import annotations

import tempfile
from pathlib import Path

import pandas as pd

from app.data.generate import (
    CATEGORY_WEIGHT_RANGES,
    COUNTRY_GRID_FACTORS,
    MATERIAL_EMISSION_FACTORS,
    TRANSPORT_FACTORS,
    generate_synthetic_dataset,
)
from app.data.train import train_and_evaluate


def test_synthetic_data_generation_shape_and_columns():
    """Verify generated dataset contains expected columns, rows, and valid ranges."""
    with tempfile.TemporaryDirectory() as tmpdir:
        csv_path = Path(tmpdir) / "test_data.csv"
        df = generate_synthetic_dataset(n_samples=200, random_seed=42, output_path=csv_path)

        assert len(df) == 200
        assert csv_path.exists()

        expected_columns = {
            "category",
            "weight_kg",
            "main_material",
            "country",
            "transport_mode",
            "distance_km",
            "total_kg_co2e",
        }
        assert set(df.columns) == expected_columns

        # Verify values conform to allowed domain sets
        assert set(df["category"]).issubset(set(CATEGORY_WEIGHT_RANGES.keys()))
        assert set(df["main_material"]).issubset(set(MATERIAL_EMISSION_FACTORS.keys()))
        assert set(df["country"]).issubset(set(COUNTRY_GRID_FACTORS.keys()))
        assert set(df["transport_mode"]).issubset(set(TRANSPORT_FACTORS.keys()))

        # Footprint values must be strictly positive
        assert (df["total_kg_co2e"] > 0).all()
        assert (df["weight_kg"] > 0).all()
        assert (df["distance_km"] >= 0).all()


def test_synthetic_data_reproducibility():
    """Verify identical random seed generates identical results."""
    df1 = generate_synthetic_dataset(n_samples=100, random_seed=123)
    df2 = generate_synthetic_dataset(n_samples=100, random_seed=123)
    pd.testing.assert_frame_equal(df1, df2)


def test_train_and_evaluate_pipeline():
    """Verify train_and_evaluate trains both models, picks best MAE, and outputs artifacts."""
    with tempfile.TemporaryDirectory() as tmpdir:
        artifacts_dir = Path(tmpdir)
        csv_path = artifacts_dir / "synthetic_garments.csv"
        generate_synthetic_dataset(n_samples=300, random_seed=42, output_path=csv_path)
        info = train_and_evaluate(data_path=csv_path, artifacts_dir=artifacts_dir, random_state=42)

        # Artifacts exist
        assert (artifacts_dir / "model.joblib").exists()
        assert (artifacts_dir / "model_info.json").exists()

        # Metrics are properly recorded
        assert "LinearRegression" in info["metrics"]
        assert "RandomForestRegressor" in info["metrics"]
        assert info["selected_model"] in ["LinearRegression", "RandomForestRegressor"]
        assert info["best_mae"] > 0.0

        # R2 should be high (> 0.85) on synthetic data with known physical relationships
        best_r2 = info["metrics"][info["selected_model"]]["r2"]
        assert best_r2 > 0.85
