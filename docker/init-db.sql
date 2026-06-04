-- Creates one database per microservice (database-per-service pattern).
-- Runs once, on first initialization of the Postgres data volume.
-- The smart-features database additionally needs the pgvector extension; it is
-- created automatically by that service's Flyway migration V6 (the image is
-- pgvector/pgvector, so the `vector` extension is available).

CREATE DATABASE pharmaflow_user_db;
CREATE DATABASE pharmaflow_product;
CREATE DATABASE pharmaflow_pharmacy_db;
CREATE DATABASE pharmaflow_order_db;
CREATE DATABASE pharmaflow_smart_features_db;
