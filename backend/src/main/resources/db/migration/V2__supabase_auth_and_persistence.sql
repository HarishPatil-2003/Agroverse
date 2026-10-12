-- ========================================================
-- AgroVerse Database Schema Migration V2
-- Supabase Authentication Linking & Persistent Notifications
-- Compatible with PostgreSQL and H2
-- ========================================================

-- 1. Add Supabase Provider Identity column to users
ALTER TABLE users ADD COLUMN IF NOT EXISTS supabase_user_id VARCHAR(100) UNIQUE;

-- 2. Allow password to be NULL when identity is managed by Supabase Auth
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

-- 3. Index on Supabase Provider User ID
CREATE INDEX IF NOT EXISTS idx_users_supabase_user_id ON users(supabase_user_id);

-- 4. Persistent Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    notification_type VARCHAR(50) NOT NULL DEFAULT 'INFO',
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
