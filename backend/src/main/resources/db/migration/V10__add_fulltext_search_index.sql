-- Full-text search GIN index on orders.title for Russian morphology
CREATE INDEX idx_orders_title_fts ON orders
    USING GIN (to_tsvector('russian', coalesce(title, '')));
