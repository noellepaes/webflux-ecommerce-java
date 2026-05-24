CREATE SCHEMA IF NOT EXISTS auth_schema;

COMMENT ON SCHEMA auth_schema IS 'Schema para autenticação (credenciais de login)';

CREATE TABLE IF NOT EXISTS auth_schema.users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(72) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    version BIGINT
);

CREATE INDEX idx_users_email ON auth_schema.users(email);
