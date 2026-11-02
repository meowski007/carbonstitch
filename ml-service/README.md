# Carbonstitch ML Service

FastAPI-based microservice that predicts the carbon footprint (kg CO2e) of garments when detailed material breakdown is incomplete.

## Features
- **Synthetic Training Pipeline**: Generates realistic garment LCA data using illustrative emission factors and stochastic noise (`seed=42`).
- **Model Comparison**: Automatically compares `LinearRegression` against `RandomForestRegressor` and selects the model with the lowest Mean Absolute Error (MAE) on a held-out test split.
- **Async REST Endpoints**: High-throughput inference via `POST /predict`, observability via `GET /health` and `GET /model-info`.
- **Validation**: Strict validation with Pydantic v2.

## Endpoints
- `POST /predict` - Estimate carbon footprint for a garment.
- `GET /health` - Health status and model readiness.
- `GET /model-info` - Disclose training metrics (MAE, RMSE, R2) and selected architecture.

## Local Development
```bash
python -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate
pip install -r requirements.txt
python -m app.data.train
uvicorn app.main:app --port 8000 --reload
```
