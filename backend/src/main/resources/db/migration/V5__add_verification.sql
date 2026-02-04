-- FreelanceKG Executor Verification
-- Version 5: Add verification system

-- Add executor_verified to users
ALTER TABLE users ADD COLUMN executor_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Verification requests table
CREATE TABLE executor_verifications (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    passport_url VARCHAR(500),
    selfie_url VARCHAR(500),
    rejection_reason TEXT,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    reviewed_by BIGINT REFERENCES users(id)
);

CREATE INDEX idx_verifications_status ON executor_verifications(status);
CREATE INDEX idx_verifications_submitted ON executor_verifications(submitted_at DESC);
