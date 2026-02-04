-- FreelanceKG Database Schema
-- Version 1: Initial schema

-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500),
    whatsapp_link VARCHAR(500),
    profile_visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    hide_from_executor_list BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_active ON users(active);

-- Categories table (with hierarchy support)
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    icon_url VARCHAR(200),
    parent_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_categories_parent ON categories(parent_id);
CREATE INDEX idx_categories_slug ON categories(slug);
CREATE INDEX idx_categories_active ON categories(active);

-- Executor profiles table
CREATE TABLE executor_profiles (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    bio TEXT,
    specialization VARCHAR(200),
    total_orders INTEGER NOT NULL DEFAULT 0,
    completed_orders INTEGER NOT NULL DEFAULT 0,
    disputed_orders INTEGER NOT NULL DEFAULT 0,
    avg_completion_days DOUBLE PRECISION NOT NULL DEFAULT 0,
    rating DECIMAL(3,2) NOT NULL DEFAULT 0,
    review_count INTEGER NOT NULL DEFAULT 0,
    available_for_work BOOLEAN NOT NULL DEFAULT TRUE,
    last_active_at TIMESTAMP
);

CREATE INDEX idx_executor_profiles_rating ON executor_profiles(rating DESC);
CREATE INDEX idx_executor_profiles_available ON executor_profiles(available_for_work);
CREATE INDEX idx_executor_profiles_review_count ON executor_profiles(review_count DESC);

-- Junction table for executor categories (many-to-many)
CREATE TABLE executor_categories (
    executor_id BIGINT REFERENCES executor_profiles(user_id) ON DELETE CASCADE,
    category_id BIGINT REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (executor_id, category_id)
);

CREATE INDEX idx_executor_categories_category ON executor_categories(category_id);

-- Orders table
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    client_id BIGINT NOT NULL REFERENCES users(id),
    executor_id BIGINT REFERENCES users(id),
    budget_min DECIMAL(12,2),
    budget_max DECIMAL(12,2),
    agreed_price DECIMAL(12,2),
    deadline DATE,
    agreed_deadline DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    view_count INTEGER NOT NULL DEFAULT 0,
    response_count INTEGER NOT NULL DEFAULT 0,
    attachments JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_orders_client ON orders(client_id);
CREATE INDEX idx_orders_executor ON orders(executor_id);
CREATE INDEX idx_orders_category ON orders(category_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created ON orders(created_at DESC);
CREATE INDEX idx_orders_public_new ON orders(is_public, status) WHERE is_public = TRUE AND status = 'NEW';

-- Order responses (executor applications)
CREATE TABLE order_responses (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    executor_id BIGINT NOT NULL REFERENCES users(id),
    cover_letter TEXT NOT NULL,
    proposed_price DECIMAL(12,2),
    proposed_days INTEGER,
    is_selected BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(order_id, executor_id)
);

CREATE INDEX idx_order_responses_order ON order_responses(order_id);
CREATE INDEX idx_order_responses_executor ON order_responses(executor_id);

-- Reviews table
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE REFERENCES orders(id),
    client_id BIGINT NOT NULL REFERENCES users(id),
    executor_id BIGINT NOT NULL REFERENCES users(id),
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    is_moderated BOOLEAN NOT NULL DEFAULT FALSE,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    moderator_comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reviews_executor ON reviews(executor_id);
CREATE INDEX idx_reviews_client ON reviews(client_id);
CREATE INDEX idx_reviews_visible ON reviews(is_visible, is_moderated);

-- Portfolio items
CREATE TABLE portfolio_items (
    id BIGSERIAL PRIMARY KEY,
    executor_id BIGINT NOT NULL REFERENCES executor_profiles(user_id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category_id BIGINT REFERENCES categories(id),
    images JSONB,
    external_link VARCHAR(500),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_portfolio_executor ON portfolio_items(executor_id);
CREATE INDEX idx_portfolio_category ON portfolio_items(category_id);

-- Chat rooms (one per order, created when executor is selected)
CREATE TABLE chat_rooms (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE REFERENCES orders(id),
    client_id BIGINT NOT NULL REFERENCES users(id),
    executor_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_message_at TIMESTAMP
);

CREATE INDEX idx_chat_rooms_client ON chat_rooms(client_id);
CREATE INDEX idx_chat_rooms_executor ON chat_rooms(executor_id);
CREATE INDEX idx_chat_rooms_last_message ON chat_rooms(last_message_at DESC);

-- Messages
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    chat_room_id BIGINT NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    attachments JSONB,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_messages_chat_room ON messages(chat_room_id, created_at);
CREATE INDEX idx_messages_unread ON messages(chat_room_id, is_read) WHERE is_read = FALSE;

-- Verification codes (for email/phone verification)
CREATE TABLE verification_codes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code VARCHAR(10) NOT NULL,
    type VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_verification_codes_user ON verification_codes(user_id, type, used);
CREATE INDEX idx_verification_codes_expires ON verification_codes(expires_at);
