CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL,
    customer_type VARCHAR(20) NOT NULL,
    kyc_status VARCHAR(20) NOT NULL,
    aml_status VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    account_id VARCHAR(20) NOT NULL,
    customer_id VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    opened_at TIMESTAMPTZ NOT NULL
);
