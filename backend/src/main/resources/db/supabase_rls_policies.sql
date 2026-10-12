-- ========================================================
-- AgroVerse Supabase PostgreSQL Row Level Security (RLS)
-- Production Data Access & Ownership Policies
-- ========================================================

-- Enable Row Level Security on all core tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE crop_analysis_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE farm_tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE crop_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE marketplace_listings ENABLE ROW LEVEL SECURITY;
ALTER TABLE government_schemes ENABLE ROW LEVEL SECURITY;
ALTER TABLE scheme_applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

-- 1. Users Table Policies
-- Users can only read and update their own profile linked via supabase_user_id
CREATE POLICY "Users can read own profile" ON users
    FOR SELECT USING (auth.uid()::text = supabase_user_id);

CREATE POLICY "Users can update own profile" ON users
    FOR UPDATE USING (auth.uid()::text = supabase_user_id);

-- 2. Crop Analysis History Policies
-- Strict owner restriction: only the farmer who initiated the diagnosis can view/insert
CREATE POLICY "Farmers can read own crop history" ON crop_analysis_history
    FOR SELECT USING (auth.uid()::text = (SELECT supabase_user_id FROM users WHERE users.id = crop_analysis_history.user_id));

CREATE POLICY "Farmers can create own crop history" ON crop_analysis_history
    FOR INSERT WITH CHECK (auth.uid()::text = (SELECT supabase_user_id FROM users WHERE users.id = crop_analysis_history.user_id));

-- 3. Farm Tasks Policies
CREATE POLICY "Farmers can manage own tasks" ON farm_tasks
    FOR ALL USING (auth.uid()::text = (SELECT supabase_user_id FROM users WHERE users.id = farm_tasks.user_id));

-- 4. Crop Items Policies
CREATE POLICY "Farmers can manage own crop items" ON crop_items
    FOR ALL USING (auth.uid()::text = (SELECT supabase_user_id FROM users WHERE users.id = crop_items.user_id));

-- 5. Community Posts Policies
-- Public reads permitted for knowledge sharing; writes and deletes strictly owner-restricted
CREATE POLICY "Anyone can view community posts" ON community_posts
    FOR SELECT USING (true);

CREATE POLICY "Authenticated users can create community posts" ON community_posts
    FOR INSERT WITH CHECK (auth.uid()::text = (SELECT supabase_user_id FROM users WHERE users.id = community_posts.user_id));

CREATE POLICY "Authors can delete own community posts" ON community_posts
    FOR DELETE USING (auth.uid()::text = (SELECT supabase_user_id FROM users WHERE users.id = community_posts.user_id));

-- 6. Community Comments Policies
CREATE POLICY "Anyone can view comments" ON community_comments
    FOR SELECT USING (true);

CREATE POLICY "Authenticated users can create comments" ON community_comments
    FOR INSERT WITH CHECK (auth.uid()::text = (SELECT supabase_user_id FROM users WHERE users.id = community_comments.user_id));

-- 7. Marketplace Listings Policies
-- Public can view active listings; sellers can manage only their own listings
CREATE POLICY "Anyone can view active marketplace listings" ON marketplace_listings
    FOR SELECT USING (status = 'ACTIVE');

CREATE POLICY "Sellers can create listings" ON marketplace_listings
    FOR INSERT WITH CHECK (auth.uid()::text = (SELECT supabase_user_id FROM users WHERE users.id = marketplace_listings.user_id));

CREATE POLICY "Sellers can delete own listings" ON marketplace_listings
    FOR DELETE USING (auth.uid()::text = (SELECT supabase_user_id FROM users WHERE users.id = marketplace_listings.user_id));

CREATE POLICY "Sellers can update own listings" ON marketplace_listings
    FOR UPDATE USING (auth.uid()::text = (SELECT supabase_user_id FROM users WHERE users.id = marketplace_listings.user_id));

-- 8. Government Schemes Policies
-- Public read-only; admin-only writes handled via backend
CREATE POLICY "Public read for schemes" ON government_schemes
    FOR SELECT USING (true);

-- 9. Scheme Applications Policies
CREATE POLICY "Farmers can view own scheme applications" ON scheme_applications
    FOR SELECT USING (auth.uid()::text = (SELECT supabase_user_id FROM users WHERE users.id = scheme_applications.user_id));

CREATE POLICY "Farmers can submit scheme applications" ON scheme_applications
    FOR INSERT WITH CHECK (auth.uid()::text = (SELECT supabase_user_id FROM users WHERE users.id = scheme_applications.user_id));

-- 10. Notifications Policies
CREATE POLICY "Farmers can view own notifications" ON notifications
    FOR SELECT USING (auth.uid()::text = (SELECT supabase_user_id FROM users WHERE users.id = notifications.user_id));

CREATE POLICY "Farmers can update own notifications" ON notifications
    FOR UPDATE USING (auth.uid()::text = (SELECT supabase_user_id FROM users WHERE users.id = notifications.user_id));
