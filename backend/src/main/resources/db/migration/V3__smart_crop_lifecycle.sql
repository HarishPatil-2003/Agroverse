-- V3__smart_crop_lifecycle.sql
-- Phase 7: Smart Crop Management & Lifecycle Fields

ALTER TABLE crop_items ADD COLUMN IF NOT EXISTS variety VARCHAR(100);
ALTER TABLE crop_items ADD COLUMN IF NOT EXISTS sowing_date DATE;
ALTER TABLE crop_items ADD COLUMN IF NOT EXISTS growth_stage VARCHAR(50);
ALTER TABLE crop_items ADD COLUMN IF NOT EXISTS expected_harvest_date DATE;
ALTER TABLE crop_items ADD COLUMN IF NOT EXISTS irrigation_notes TEXT;
ALTER TABLE crop_items ADD COLUMN IF NOT EXISTS fertilizer_notes TEXT;
ALTER TABLE crop_items ADD COLUMN IF NOT EXISTS treatment_notes TEXT;
ALTER TABLE crop_items ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_crop_items_user_archived ON crop_items(user_id, archived);
