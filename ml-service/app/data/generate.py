"""Synthetic garment carbon footprint dataset generator.

This script generates synthetic data for research and demonstration purposes.
Emission numbers are approximations inspired by public Life Cycle Assessment (LCA)
proxies and are strictly intended for benchmarking ML models.
"""

from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
import pandas as pd

# Representative illustrative emission factors (kg CO2e per kg material)
# Approximations based on public LCA literature (e.g., Higg MSI / DEFRA proxies)
MATERIAL_EMISSION_FACTORS: dict[str, float] = {
    "COTTON": 8.5,
    "ORGANIC_COTTON": 4.2,
    "POLYESTER": 6.4,
    "RECYCLED_POLYESTER": 3.1,
    "WOOL": 24.0,
    "NYLON": 12.0,
    "LINEN": 5.0,
    "SILK": 28.0,
    "VISCOSE": 7.5,
    "OTHER": 8.0,
}

# Country grid carbon intensity proxy (kg CO2e per kWh equivalent manufacturing energy)
COUNTRY_GRID_FACTORS: dict[str, float] = {
    "CN": 0.65,  # China
    "BD": 0.60,  # Bangladesh
    "VN": 0.55,  # Vietnam
    "IN": 0.72,  # India
    "TR": 0.45,  # Turkey
    "PT": 0.25,  # Portugal
    "IT": 0.30,  # Italy
    "US": 0.40,  # United States
}

# Transport emission factors (kg CO2e per tonne-km)
TRANSPORT_FACTORS: dict[str, float] = {
    "AIR": 0.602,
    "SEA": 0.015,
    "ROAD": 0.096,
    "RAIL": 0.028,
}

CATEGORY_WEIGHT_RANGES: dict[str, tuple[float, float]] = {
    "T_SHIRT": (0.15, 0.30),
    "JEANS": (0.50, 0.90),
    "SWEATER": (0.40, 0.85),
    "JACKET": (0.70, 1.60),
    "DRESS": (0.30, 0.70),
    "SHIRT": (0.20, 0.40),
}


def generate_synthetic_dataset(
    n_samples: int = 5000,
    random_seed: int = 42,
    output_path: Path | None = None,
) -> pd.DataFrame:
    """Generate synthetic garment footprint dataset with a reproducible seed."""
    rng = np.random.RandomState(random_seed)

    categories = list(CATEGORY_WEIGHT_RANGES.keys())
    materials = list(MATERIAL_EMISSION_FACTORS.keys())
    countries = list(COUNTRY_GRID_FACTORS.keys())
    transport_modes = list(TRANSPORT_FACTORS.keys())

    cat_choices = rng.choice(categories, size=n_samples)
    weights = np.array(
        [
            rng.uniform(CATEGORY_WEIGHT_RANGES[cat][0], CATEGORY_WEIGHT_RANGES[cat][1])
            for cat in cat_choices
        ]
    ).round(3)

    mat_choices = rng.choice(materials, size=n_samples)
    country_choices = rng.choice(countries, size=n_samples)
    transport_choices = rng.choice(transport_modes, size=n_samples, p=[0.15, 0.60, 0.20, 0.05])
    distances = rng.uniform(500, 15000, size=n_samples).round(0)

    # Calculate domain-driven target with stochastic noise
    total_emissions = []
    for w, m, c, t, d in zip(
        weights, mat_choices, country_choices, transport_choices, distances, strict=True
    ):
        mat_emission = w * MATERIAL_EMISSION_FACTORS[m]
        # Assume manufacturing energy ~ 8.0 kWh per kg of garment cut-and-sew
        mfg_emission = w * 8.0 * COUNTRY_GRID_FACTORS[c]
        # Transport emission: (weight in tonnes) * distance_km * transport_factor
        t_emission = (w / 1000.0) * d * TRANSPORT_FACTORS[t]

        # Add 5% Gaussian noise to simulate real-world variance
        noise = rng.normal(0, 0.05 * (mat_emission + mfg_emission + t_emission))
        total = max(0.1, round(mat_emission + mfg_emission + t_emission + noise, 3))
        total_emissions.append(total)

    df = pd.DataFrame(
        {
            "category": cat_choices,
            "weight_kg": weights,
            "main_material": mat_choices,
            "country": country_choices,
            "transport_mode": transport_choices,
            "distance_km": distances,
            "total_kg_co2e": total_emissions,
        }
    )

    if output_path:
        output_path.parent.mkdir(parents=True, exist_ok=True)
        df.to_csv(output_path, index=False)
        print(f"Generated {n_samples} synthetic samples -> {output_path}")

    return df


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generate synthetic garment carbon footprint data")
    parser.add_argument("--samples", type=int, default=5000)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument(
        "--output",
        type=Path,
        default=Path(__file__).resolve().parents[2] / "artifacts" / "synthetic_garments.csv",
    )
    args = parser.parse_args()
    generate_synthetic_dataset(
        n_samples=args.samples, random_seed=args.seed, output_path=args.output
    )
