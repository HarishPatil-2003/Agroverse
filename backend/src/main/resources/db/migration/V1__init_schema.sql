-- ========================================================
-- AgroVerse Database Schema Migration V1
-- PostgreSQL and H2 Compatible DDL
-- ========================================================

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    farmer_id VARCHAR(50) UNIQUE NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    full_name VARCHAR(200),
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(30),
    date_of_birth DATE,
    gender VARCHAR(20),
    farm_type VARCHAR(50),
    land_holding DOUBLE PRECISION,
    land_unit VARCHAR(20),
    experience_years INTEGER,
    address VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(20),
    profile_image_url VARCHAR(500),
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    profile_completed BOOLEAN NOT NULL DEFAULT FALSE,
    role VARCHAR(30) NOT NULL DEFAULT 'ROLE_FARMER',
    preferred_language VARCHAR(10) DEFAULT 'en',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_farmer_id ON users(farmer_id);

-- 2. Government Schemes Table
CREATE TABLE IF NOT EXISTS government_schemes (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    short_description VARCHAR(1000),
    full_description VARCHAR(5000),
    category VARCHAR(100),
    subsidy_amount VARCHAR(100),
    deadline_date DATE,
    eligibility VARCHAR(2000),
    benefits VARCHAR(2000),
    application_process VARCHAR(2000),
    official_website VARCHAR(500),
    image_url VARCHAR(500),
    scheme_type VARCHAR(50),
    status VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS scheme_applicable_crops (
    scheme_id BIGINT NOT NULL REFERENCES government_schemes(id) ON DELETE CASCADE,
    crop_type VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS scheme_applicable_states (
    scheme_id BIGINT NOT NULL REFERENCES government_schemes(id) ON DELETE CASCADE,
    state VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS scheme_documents (
    scheme_id BIGINT NOT NULL REFERENCES government_schemes(id) ON DELETE CASCADE,
    document VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS scheme_applications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    scheme_id BIGINT NOT NULL REFERENCES government_schemes(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'APPLIED',
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_at TIMESTAMP,
    remarks VARCHAR(255),
    external_application_reference VARCHAR(100),
    CONSTRAINT uq_user_scheme UNIQUE (user_id, scheme_id)
);

-- 3. Crop Analysis History Table
CREATE TABLE IF NOT EXISTS crop_analysis_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    farmer_id VARCHAR(50) NOT NULL,
    file_name VARCHAR(255),
    crop VARCHAR(100) NOT NULL,
    disease VARCHAR(150) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    diagnostic_certainty VARCHAR(50) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    description VARCHAR(2000),
    treatments_json TEXT,
    pesticide_warning VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_crop_history_user ON crop_analysis_history(user_id);
CREATE INDEX IF NOT EXISTS idx_crop_history_farmer ON crop_analysis_history(farmer_id);

-- 4. Community Posts Table
CREATE TABLE IF NOT EXISTS community_posts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    author_name VARCHAR(150) NOT NULL,
    author_avatar VARCHAR(500),
    category VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    likes_count INTEGER NOT NULL DEFAULT 0,
    comments_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_community_posts_created ON community_posts(created_at DESC);

-- 5. Community Comments Table
CREATE TABLE IF NOT EXISTS community_comments (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    author_name VARCHAR(150) NOT NULL,
    author_avatar VARCHAR(500),
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Marketplace Listings Table
CREATE TABLE IF NOT EXISTS marketplace_listings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    seller_name VARCHAR(150) NOT NULL,
    seller_phone VARCHAR(50),
    title VARCHAR(200) NOT NULL,
    category VARCHAR(100) NOT NULL,
    condition VARCHAR(50) NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    location VARCHAR(150) NOT NULL,
    description TEXT,
    image_url VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_marketplace_category ON marketplace_listings(category);
CREATE INDEX IF NOT EXISTS idx_marketplace_status ON marketplace_listings(status);

-- 7. Farm Tasks Table
CREATE TABLE IF NOT EXISTS farm_tasks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    subtitle VARCHAR(255),
    due_time VARCHAR(50),
    task_type VARCHAR(50) NOT NULL,
    icon VARCHAR(50),
    color VARCHAR(50),
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. Crop Items Table
CREATE TABLE IF NOT EXISTS crop_items (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    field VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    last_checked VARCHAR(50),
    image VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
