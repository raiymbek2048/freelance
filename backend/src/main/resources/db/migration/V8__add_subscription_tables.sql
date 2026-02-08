-- Subscription settings table (single row for global settings)
CREATE TABLE subscription_settings (
    id BIGINT PRIMARY KEY DEFAULT 1,
    price DECIMAL(10,2) DEFAULT 500.00,
    subscription_start_date DATE,
    trial_days INTEGER DEFAULT 7,
    announcement_message TEXT,
    announcement_enabled BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP,
    updated_by_id BIGINT REFERENCES users(id)
);

-- User subscriptions table
CREATE TABLE user_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    payment_reference VARCHAR(255),
    granted_by_admin VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_subscriptions_user ON user_subscriptions(user_id);
CREATE INDEX idx_user_subscriptions_status ON user_subscriptions(status);

-- Insert default settings
INSERT INTO subscription_settings (id, price, trial_days, announcement_enabled)
VALUES (1, 500.00, 7, FALSE);
