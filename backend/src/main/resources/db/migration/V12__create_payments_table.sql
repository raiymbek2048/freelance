-- Payments table for FreedomPay integration
CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    amount          DECIMAL(10,2) NOT NULL,
    days            INTEGER NOT NULL,
    pg_order_id     VARCHAR(100) NOT NULL UNIQUE,
    pg_payment_id   VARCHAR(100),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_pg_order_id ON payments(pg_order_id);
CREATE INDEX idx_payments_status ON payments(status);
