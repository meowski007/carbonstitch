-- V2__seed_emission_factors.sql: Seed representative emission factors
-- NOTE: ALL FACTORS BELOW ARE ILLUSTRATIVE APPROXIMATIONS FOR DEMO AND RESEARCH PURPOSES.
-- Proxies are derived from public LCA literature (DEFRA GHG Factors, Higg Materials Sustainability Index, Ecoinvent 3.8 proxies).

INSERT INTO emission_factors (factor_type, factor_key, factor_value, unit, source_reference, is_illustrative) VALUES
-- Raw Fiber / Material Factors (kg CO2e per kg material)
('MATERIAL', 'COTTON', 8.5000, 'kg CO2e/kg', 'Illustrative proxy: Conventional cotton (Higg MSI / DEFRA)', true),
('MATERIAL', 'ORGANIC_COTTON', 4.2000, 'kg CO2e/kg', 'Illustrative proxy: Organic cotton cultivation (Textile Exchange)', true),
('MATERIAL', 'POLYESTER', 6.4000, 'kg CO2e/kg', 'Illustrative proxy: Virgin PET staple fiber (Ecoinvent 3.8)', true),
('MATERIAL', 'RECYCLED_POLYESTER', 3.1000, 'kg CO2e/kg', 'Illustrative proxy: Mechanical rPET fiber (Higg MSI proxy)', true),
('MATERIAL', 'WOOL', 24.0000, 'kg CO2e/kg', 'Illustrative proxy: Greasy wool scouring & spinning (Ecoinvent 3.8)', true),
('MATERIAL', 'NYLON', 12.0000, 'kg CO2e/kg', 'Illustrative proxy: Polyamide 6 virgin polymer (Ecoinvent 3.8)', true),
('MATERIAL', 'LINEN', 5.0000, 'kg CO2e/kg', 'Illustrative proxy: European Flax retting & spinning', true),
('MATERIAL', 'SILK', 28.0000, 'kg CO2e/kg', 'Illustrative proxy: Sericulture raw silk processing (Ecoinvent)', true),
('MATERIAL', 'VISCOSE', 7.5000, 'kg CO2e/kg', 'Illustrative proxy: Wood pulp viscose staple fiber', true),
('MATERIAL', 'OTHER', 8.0000, 'kg CO2e/kg', 'Illustrative proxy: Generic apparel synthetic/natural blend', true),

-- Manufacturing & Electricity Grid Intensity Factors (kg CO2e per kg garment cut-and-sew)
-- Normalized assuming ~8.0 kWh equivalent energy per kg apparel production scaled by national grid emission factor
('COUNTRY_GRID', 'CN', 5.2000, 'kg CO2e/kg', 'Illustrative proxy: China apparel manufacturing (IEA Grid proxy ~0.65 kg/kWh)', true),
('COUNTRY_GRID', 'BD', 4.8000, 'kg CO2e/kg', 'Illustrative proxy: Bangladesh apparel manufacturing (Grid proxy ~0.60 kg/kWh)', true),
('COUNTRY_GRID', 'VN', 4.4000, 'kg CO2e/kg', 'Illustrative proxy: Vietnam apparel manufacturing (Grid proxy ~0.55 kg/kWh)', true),
('COUNTRY_GRID', 'IN', 5.7600, 'kg CO2e/kg', 'Illustrative proxy: India apparel manufacturing (Grid proxy ~0.72 kg/kWh)', true),
('COUNTRY_GRID', 'TR', 3.6000, 'kg CO2e/kg', 'Illustrative proxy: Turkey apparel manufacturing (Grid proxy ~0.45 kg/kWh)', true),
('COUNTRY_GRID', 'PT', 2.0000, 'kg CO2e/kg', 'Illustrative proxy: Portugal EU renewable grid apparel manufacturing', true),
('COUNTRY_GRID', 'IT', 2.4000, 'kg CO2e/kg', 'Illustrative proxy: Italy apparel manufacturing (Grid proxy ~0.30 kg/kWh)', true),
('COUNTRY_GRID', 'US', 3.2000, 'kg CO2e/kg', 'Illustrative proxy: United States apparel manufacturing (EPA eGRID proxy)', true),
('COUNTRY_GRID', 'DEFAULT', 4.5000, 'kg CO2e/kg', 'Illustrative proxy: Global weighted average apparel manufacturing energy', true),

-- Transport Mode Factors (kg CO2e per tonne-kilometer)
('TRANSPORT', 'AIR', 0.6020, 'kg CO2e/t-km', 'Illustrative proxy: Long-haul dedicated air freight (DEFRA 2023)', true),
('TRANSPORT', 'SEA', 0.0150, 'kg CO2e/t-km', 'Illustrative proxy: Container shipping panamax class (IMO / DEFRA 2023)', true),
('TRANSPORT', 'ROAD', 0.0960, 'kg CO2e/t-km', 'Illustrative proxy: Heavy goods vehicle articulated >33t (DEFRA 2023)', true),
('TRANSPORT', 'RAIL', 0.0280, 'kg CO2e/t-km', 'Illustrative proxy: Electric and diesel rail freight mix (DEFRA 2023)', true);
