import { supabase } from './supabaseClient.js';
import { auth } from './auth.js';
import { api } from './api.js';

// --- Helpers ---
function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function escapeAttr(value) {
    return escapeHtml(value);
}

function formatRelativeTime(dateValue) {
    if (!dateValue) return "Just now";

    const date = new Date(dateValue);
    const diffMs = Date.now() - date.getTime();
    const diffMin = Math.floor(diffMs / 60000);
    const diffHr = Math.floor(diffMin / 60);
    const diffDay = Math.floor(diffHr / 24);

    if (diffMin < 1) return "Just now";
    if (diffMin < 60) return `${diffMin} min ago`;
    if (diffHr < 24) return `${diffHr} hours ago`;
    if (diffDay < 7) return `${diffDay} days ago`;

    return date.toLocaleDateString();
}

function getInitials(nameOrEmail) {
    if (!nameOrEmail) return "U";
    const cleaned = String(nameOrEmail).trim();
    if (cleaned.includes("@")) {
        return cleaned.charAt(0).toUpperCase();
    }
    const parts = cleaned.split(/\s+/).filter(Boolean);
    if (parts.length === 1) {
        return parts[0].slice(0, 2).toUpperCase();
    }
    return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
}

export function initCommunity() {
    console.log("Initializing Community Page (Supabase-First)...");
    const postsContainer = document.getElementById('community-post-feed');
    const postInput = document.getElementById('community-post-input');
    const postSubmitBtn = document.getElementById('community-post-submit');
    const triggerComposerBtn = document.getElementById('trigger-composer-btn');
    const tabLinks = document.querySelectorAll('.tab-link');
    const sortSelect = document.getElementById('sort-posts');
    const categorySelect = document.getElementById('filter-category');
    const composerAvatar = document.querySelector('.composer-avatar.user-avatar-dynamic');

    if (!postsContainer) return;

    // Set composer avatar if possible
    if (composerAvatar && auth.user) {
        const profile = auth.farmerProfile || auth.user;
        composerAvatar.src = profile.avatar_url || profile.profile_image_url || profile.profileImageUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(profile.fullName || profile.full_name || 'Farmer')}&background=16a34a&color=fff`;
    }

    // --- Sidebar Handling ---
    const rightPanel = document.querySelector('.right-panel');
    const communityRightContent = document.getElementById('community-right-sidebar-content');
    
    if (rightPanel && communityRightContent) {
        if (!window.originalRightPanelContent) {
            window.originalRightPanelContent = rightPanel.innerHTML;
        }
        rightPanel.innerHTML = communityRightContent.innerHTML;
    }

    if (triggerComposerBtn && postInput) {
        triggerComposerBtn.addEventListener('click', () => {
            postInput.focus();
            postInput.scrollIntoView({ behavior: 'smooth', block: 'center' });
        });
    }

    async function loadCommunityPosts() {
        console.log("[COMMUNITY] Loading posts...");
        postsContainer.innerHTML = `
            <div class="loading-state-modern" style="text-align: center; padding: 50px;">
                <i class="fas fa-circle-notch fa-spin" style="font-size: 30px; color: var(--primary-green);"></i>
                <p style="margin-top: 15px; color: var(--text-muted);">Fetching feed...</p>
            </div>
        `;

        try {
            let posts = [];
            const activeTab = document.querySelector('.tab-link.active')?.dataset.tab || 'all';
            const category = categorySelect?.value || 'all';

            // 1. Try Local Spring Boot API first
            try {
                console.log("[COMMUNITY] Fetching from Local API...");
                const apiPosts = await api.getPosts();
                if (apiPosts && apiPosts.length > 0) {
                    posts = apiPosts;
                    console.log("[COMMUNITY] Loaded from API:", posts.length);
                }
            } catch (apiErr) {
                console.warn("[COMMUNITY] Local API failed:", apiErr.message);
            }

            // 2. Try Supabase if API failed/empty (with timeout)
            if (posts.length === 0) {
                const isSupabaseBroken = !window.ENV?.VITE_SUPABASE_URL || window.ENV.VITE_SUPABASE_URL.includes("your-project");
                if (!isSupabaseBroken) {
                    try {
                        console.log("[COMMUNITY] Fetching from Supabase...");
                        let query = supabase.from("community_posts").select("*");
                        if (category !== 'all') query = query.eq('category', category);
                        
                        const timeoutPromise = new Promise((_, reject) => setTimeout(() => reject(new Error("TIMEOUT")), 5000));
                        const { data: sbPosts, error: sbErr } = await Promise.race([query, timeoutPromise]);
                        
                        if (!sbErr && sbPosts && sbPosts.length > 0) {
                            posts = sbPosts;
                            console.log("[COMMUNITY] Loaded from Supabase:", posts.length);
                        }
                    } catch (sbErr) {
                        console.warn("[COMMUNITY] Supabase failed/timed out:", sbErr.message);
                    }
                }
            }

            // 3. Last Resort: Hardcoded Fallback
            if (posts.length === 0) {
                console.log("[COMMUNITY] Using fallback posts.");
                posts = [
                    {
                        id: "f1",
                        author_name: "Ramesh Kumar",
                        content: "What are the best organic fertilizers for wheat in Maharashtra?",
                        category: "Tips",
                        likes_count: 12,
                        comments_count: 3,
                        created_at: new Date(Date.now() - 3600000).toISOString()
                    },
                    {
                        id: "f2",
                        author_name: "Anita Patil",
                        content: "Harvested my tomatoes today! The yield is great thanks to the new irrigation techniques.",
                        category: "Success",
                        likes_count: 45,
                        comments_count: 8,
                        created_at: new Date(Date.now() - 7200000).toISOString()
                    }
                ];
            }

            postsContainer.innerHTML = posts.map(renderCommunityPost).join("");
            bindPostActions();

        } catch (err) {
            console.error("[COMMUNITY] Critical failure:", err);
            postsContainer.innerHTML = `<p style="text-align:center; padding:20px; color:#ef4444;">Unable to load feed. Please try again later.</p>`;
        }
    }

    function renderCommunityPost(post) {
        const authorName = post.author_name || "Farmer";
        const initials = getInitials(authorName);
        const location = post.author_location || "Location not added";
        const createdAt = formatRelativeTime(post.created_at);
        const content = escapeHtml(post.content || "");
        const likes = Number(post.likes_count || 0);
        const comments = Number(post.comments_count || 0);
        const shares = Number(post.shares_count || 0);

        const avatarHtml = post.author_avatar_url
            ? `<img src="${escapeAttr(post.author_avatar_url)}" alt="${escapeAttr(authorName)}" class="post-avatar-small" onerror="this.src='https://ui-avatars.com/api/?name=${encodeURIComponent(authorName)}'">`
            : `<div class="post-avatar-small-initials" style="width: 40px; height: 40px; border-radius: 50%; background: #2ea043; color: white; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 14px; flex-shrink:0;">${escapeHtml(initials)}</div>`;

        const mediaHtml = post.image_url
            ? `<div class="post-media-wrap"><img src="${escapeAttr(post.image_url)}" alt="Post image" class="post-image-content" style="max-width: 100%; border-radius: 12px; margin-top: 10px; display: block;"></div>`
            : "";

        return `
            <div class="post-card-modern" data-id="${post.id}" style="background: white; border-radius: 16px; padding: 20px; margin-bottom: 20px; border: 1px solid var(--border-color); box-shadow: 0 2px 10px rgba(0,0,0,0.02);">
                <div class="post-header" style="display: flex; justify-content: space-between; margin-bottom: 15px;">
                    <div class="user-meta" style="display: flex; gap: 12px; align-items: center;">
                        ${avatarHtml}
                        <div class="user-info-text">
                            <h4 style="margin: 0; font-size: 15px; font-weight: 700;">${escapeHtml(authorName)}</h4>
                            <div class="badge-row" style="display: flex; gap: 8px; font-size: 11px; color: var(--text-muted); margin-top: 2px;">
                                <span class="role-tag" style="background: #e8f5e9; color: #2ea043; padding: 1px 6px; border-radius: 4px; font-weight: 600;">${escapeHtml(post.post_type === 'question' ? 'Question' : 'Farmer')}</span>
                                <span class="time-text">${escapeHtml(createdAt)}</span>
                                ${location ? `<span class="time-text">• <i class="fas fa-map-marker-alt"></i> ${escapeHtml(location)}</span>` : ''}
                            </div>
                        </div>
                    </div>
                    <button class="post-options-btn" style="background: none; border: none; color: var(--text-muted); cursor: pointer;"><i class="fas fa-ellipsis-h"></i></button>
                </div>
                
                <div class="post-body">
                    <p style="font-size: 14px; line-height: 1.6; color: var(--text-main); margin: 0;">${content}</p>
                    ${mediaHtml}
                    ${post.video_url ? `<div class="video-placeholder" style="background: #f1f5f9; padding: 20px; border-radius: 12px; text-align: center; margin-top: 10px;"><i class="fas fa-play-circle"></i> Video attached</div>` : ''}
                </div>
                
                <div class="post-footer-actions" style="margin-top: 20px; padding-top: 15px; border-top: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center;">
                    <div class="action-btns-group" style="display: flex; gap: 20px;">
                        <button class="post-action like-btn" data-post-id="${post.id}" style="background: none; border: none; display: flex; align-items: center; gap: 6px; color: var(--text-muted); cursor: pointer; font-size: 13px;">
                            <i class="far fa-thumbs-up"></i> 
                            <span>${likes}</span>
                        </button>
                        <button class="post-action comment-btn" style="background: none; border: none; display: flex; align-items: center; gap: 6px; color: var(--text-muted); cursor: pointer; font-size: 13px;">
                            <i class="far fa-comment"></i> 
                            <span>${comments}</span>
                        </button>
                        <button class="post-action share-btn" style="background: none; border: none; display: flex; align-items: center; gap: 6px; color: var(--text-muted); cursor: pointer; font-size: 13px;">
                            <i class="far fa-share-square"></i> 
                            <span>${shares}</span>
                        </button>
                    </div>
                    <a href="#" class="view-comments-link" style="font-size: 12px; color: var(--primary-green); text-decoration: none; font-weight: 600;">View all comments</a>
                </div>
            </div>
        `;
    }

    function bindPostActions() {
        document.querySelectorAll('.like-btn').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.preventDefault();
                const icon = btn.querySelector('i');
                const span = btn.querySelector('span');
                const postId = btn.dataset.postId;
                if (!postId) return;

                let count = parseInt(span.textContent) || 0;

                if (btn.classList.contains('liked')) {
                    return; // Already liked
                }

                // Optimistically update
                btn.classList.add('liked');
                if (icon) icon.classList.replace('far', 'fas');
                span.textContent = count + 1;
                btn.style.color = 'var(--primary-green, #16a34a)';

                try {
                    const res = await api.likePost(postId);
                    if (res && typeof res.likes === 'number') {
                        span.textContent = res.likes;
                    }
                } catch (err) {
                    console.error("Failed to persist like:", err);
                    btn.classList.remove('liked');
                    if (icon) icon.classList.replace('fas', 'far');
                    span.textContent = count;
                    btn.style.color = '';
                }
            });
        });
    }

    async function createCommunityPost(content) {
        try {
            const user = await auth.getSession();
            if (!user) {
                alert("Please login to create a post.");
                return;
            }

            postSubmitBtn.disabled = true;
            postSubmitBtn.innerHTML = '<i class="fas fa-circle-notch fa-spin"></i> Posting...';

            const payload = {
                title: content.length > 60 ? content.substring(0, 57).trim() + "..." : content.trim(),
                authorName: user.fullName || "Farmer",
                author_name: user.fullName || "Farmer",
                content: content.trim(),
                category: categorySelect?.value !== 'all' ? categorySelect?.value : 'General',
                post_type: document.querySelector('.tab-link.active')?.dataset.tab === 'questions' ? 'question' : 'post'
            };

            // Post via canonical Backend API (Spring Boot persistent PostgreSQL)
            await api.createPost(payload);

            postInput.value = '';
            await loadCommunityPosts();
            
        } catch (err) {
            console.error("[COMMUNITY] Post creation failed:", err);
            alert("Failed to create post: " + err.message);
        } finally {
            postSubmitBtn.disabled = false;
            postSubmitBtn.innerHTML = '<span data-i18n="community.post">Post</span> <i class="fas fa-paper-plane"></i>';
        }
    }

    if (postSubmitBtn && postInput) {
        postSubmitBtn.addEventListener('click', () => {
            const content = postInput.value.trim();
            if (content) createCommunityPost(content);
        });
    }

    tabLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            tabLinks.forEach(l => l.classList.remove('active'));
            link.classList.add('active');
            loadCommunityPosts();
        });
    });

    if (sortSelect) sortSelect.addEventListener('change', loadCommunityPosts);
    if (categorySelect) categorySelect.addEventListener('change', loadCommunityPosts);

    // Initial Load
    loadCommunityPosts();
}

window.initCommunityPage = initCommunity;