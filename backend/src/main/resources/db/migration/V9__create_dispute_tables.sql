-- Dispute table
CREATE TABLE disputes (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    opened_by_id BIGINT NOT NULL REFERENCES users(id),
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    admin_id BIGINT REFERENCES users(id),
    admin_notes TEXT,
    resolution VARCHAR(20),
    resolution_notes TEXT,
    chat_room_id BIGINT REFERENCES chat_rooms(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP,
    CONSTRAINT uq_disputes_order UNIQUE(order_id)
);

-- Dispute evidence table
CREATE TABLE dispute_evidence (
    id BIGSERIAL PRIMARY KEY,
    dispute_id BIGINT NOT NULL REFERENCES disputes(id) ON DELETE CASCADE,
    uploaded_by_id BIGINT NOT NULL REFERENCES users(id),
    file_url VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50),
    file_size BIGINT,
    description VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_disputes_status ON disputes(status);
CREATE INDEX idx_dispute_evidence_dispute_id ON dispute_evidence(dispute_id);
