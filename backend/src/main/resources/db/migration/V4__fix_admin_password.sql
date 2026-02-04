-- FreelanceKG Fix Admin Password
-- Version 4: Update admin password with working BCrypt hash
-- Password: test123456 (BCrypt strength 12)
-- IMPORTANT: Change this password immediately after first login!

UPDATE users
SET password_hash = '$2a$12$k598WwSGtXOaN7nW9/1Oa.MfyBCm8XWWjQo9Vz1kuD1LAqTcgYX3i'
WHERE email = 'admin@freelance.kg';
