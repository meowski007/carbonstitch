-- V1__init_schema.sql: Initial schema for Carbonstitch footprint-service

CREATE TABLE garments (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    weight_kg NUMERIC(8,3) NOT NULL CHECK (weight_kg > 0),
    country VARCHAR(2) NOT NULL,
    transport_mode VARCHAR(32) NOT NULL,
    distance_km NUMERIC(10,2) NOT NULL CHECK (distance_km >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE material_compositions (
    id BIGSERIAL PRIMARY KEY,
    garment_id BIGINT NOT NULL REFERENCES garments(id) ON DELETE CASCADE,
    material VARCHAR(64) NOT NULL,
    percentage NUMERIC(5,2) NOT NULL CHECK (percentage > 0 AND percentage <= 100)
);

CREATE TABLE footprint_results (
    id BIGSERIAL PRIMARY KEY,
    garment_id BIGINT NOT NULL UNIQUE REFERENCES garments(id) ON DELETE CASCADE,
    total_kg_co2e NUMERIC(10,4) NOT NULL,
    material_kg_co2e NUMERIC(10,4) NOT NULL,
    manufacturing_kg_co2e NUMERIC(10,4) NOT NULL,
    transport_kg_co2e NUMERIC(10,4) NOT NULL,
    primary_emission_driver VARCHAR(64),
    calculation_method VARCHAR(32) NOT NULL,
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE emission_factors (
    id BIGSERIAL PRIMARY KEY,
    factor_type VARCHAR(32) NOT NULL,
    factor_key VARCHAR(64) NOT NULL,
    factor_value NUMERIC(10,4) NOT NULL CHECK (factor_value >= 0),
    unit VARCHAR(32) NOT NULL,
    source_reference VARCHAR(255),
    is_illustrative BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_factor_type_key UNIQUE (factor_type, factor_key)
);

CREATE TABLE calculation_jobs (
    id VARCHAR(64) PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    total_items INT NOT NULL DEFAULT 0,
    processed_items INT NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_garments_sku ON garments(sku);
CREATE INDEX idx_mat_comp_garment_id ON material_compositions(garment_id);
CREATE INDEX idx_footprint_total_co2e ON footprint_results(total_kg_co2e DESC);
CREATE INDEX idx_emission_factor_lookup ON emission_factors(factor_type, factor_key);
