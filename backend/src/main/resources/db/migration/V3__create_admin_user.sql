-- FreelanceKG Create Admin User
-- Version 3: Create default admin user
-- Password: admin123 (BCrypt encoded)

INSERT INTO users (email, phone, password_hash, full_name, profile_visibility, hide_from_executor_list,
                   email_verified, phone_verified, role, active, created_at)
VALUES (
    'admin@freelance.kg',
    '+996700000000',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4wOoQi1vCl.Xa3.C', -- admin123
    'System Administrator',
    'PRIVATE',
    true,
    true,
    true,
    'ADMIN',
    true,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO NOTHING;
