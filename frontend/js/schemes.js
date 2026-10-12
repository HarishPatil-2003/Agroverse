import { api } from './api.js';
import { auth } from './auth.js';
import { supabase } from './supabaseClient.js';

let allSchemes = [];
let userApplications = [];

// Fallback data for when Supabase/Backend is unavailable
function getFallbackSchemes() {
    return [
        {
            id: "pm-kisan",
            title: "Pradhan Mantri Kisan Samman Nidhi Yojana",
            description: "Financial assistance of ₹6,000 per year to eligible farmer families.",
            category: "Income Support",
            crop_type: "All Crops",
            state: "All States",
            eligibility: "Small and marginal farmers with cultivable landholding as per government rules.",
            benefits: "₹6,000 per year in three equal installments.",
            documents_required: "Aadhaar card, bank account details, land records.",
            official_url: "https://pmkisan.gov.in/",
            deadline: "2026-06-15",
            status: "active"
        },
        {
            id: "pmksy",
            title: "Pradhan Mantri Krishi Sinchai Yojana",
            description: "Support for irrigation, water conservation, and efficient water use.",
            category: "Subsidy",
            crop_type: "All Crops",
            state: "All States",
            eligibility: "Farmers requiring irrigation support and water conservation assistance.",
            benefits: "Financial support for micro-irrigation and water conservation.",
            documents_required: "Aadhaar card, land documents, bank account details.",
            official_url: "https://pmksy.gov.in/",
            deadline: "2026-06-30",
            status: "active"
        },
        {
            id: "pmfby",
            title: "Pradhan Mantri Fasal Bima Yojana",
            description: "Crop insurance scheme protecting farmers from crop loss due to natural calamities.",
            category: "Crop Insurance",
            crop_type: "All Crops",
            state: "All States",
            eligibility: "Farmers growing notified crops in notified areas.",
            benefits: "Insurance coverage against crop loss.",
            documents_required: "Aadhaar card, bank account, crop sowing certificate, land records.",
            official_url: "https://pmfby.gov.in/",
            deadline: "2026-07-10",
            status: "active"
        }
    ];
}

export async function initSchemes() {
    console.log("SCHEMES LOAD START");
    
    // --- Sidebar Handling ---
    const rightPanel = document.querySelector('.right-panel');
    const schemesRightContent = document.getElementById('schemes-right-sidebar-content');
    
    if (rightPanel && schemesRightContent) {
        if (!window.originalRightPanelContent) {
            window.originalRightPanelContent = rightPanel.innerHTML;
        }
        rightPanel.innerHTML = schemesRightContent.innerHTML;
    }

    await loadSchemesData();

    // Set up search and filter listeners
    const searchInput = document.getElementById('scheme-search');
    const cropFilter = document.getElementById('filter-crop');
    const stateFilter = document.getElementById('filter-state');
    const categoryFilter = document.getElementById('filter-category');

    const renderFiltered = () => renderSchemesList(allSchemes);

    if (searchInput) searchInput.addEventListener('input', renderFiltered);
    if (cropFilter) cropFilter.addEventListener('change', renderFiltered);
    if (stateFilter) stateFilter.addEventListener('change', renderFiltered);
    if (categoryFilter) categoryFilter.addEventListener('change', renderFiltered);

    // Setup modal close
    const modal = document.getElementById('scheme-modal');
    const closeBtn = document.getElementById('close-modal');
    if (closeBtn) {
        closeBtn.addEventListener('click', () => {
            if (modal) modal.style.display = 'none';
        });
    }
    // Close on overlay click
    if (modal) {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) modal.style.display = 'none';
        });
    }
}

async function loadSchemesData() {
    const container = document.getElementById('schemes-container');
    if (!container) return;
        
    try {
        console.log("[SCHEMES] Starting data load...");
        
        // 1. Try Local Spring Boot API first (Most reliable in this environment)
        try {
            console.log("[SCHEMES] Fetching from Local API...");
            const apiSchemes = await api.getSchemes();
            if (apiSchemes && apiSchemes.length > 0) {
                allSchemes = apiSchemes;
                console.log("[SCHEMES] Loaded from API:", allSchemes.length);
            }
        } catch (apiErr) {
            console.warn("[SCHEMES] Local API fetch failed:", apiErr.message);
        }

        // 2. If API failed/empty, try Supabase (with timeout)
        if (allSchemes.length === 0) {
            const isSupabaseBroken = !window.ENV?.VITE_SUPABASE_URL || window.ENV.VITE_SUPABASE_URL.includes("your-project");
            if (!isSupabaseBroken) {
                try {
                    console.log("[SCHEMES] Fetching from Supabase...");
                    const sbPromise = supabase.from("schemes").select("*").eq("status", "active");
                    const timeoutPromise = new Promise((_, reject) => setTimeout(() => reject(new Error("TIMEOUT")), 5000));
                    
                    const { data: sbSchemes, error: sbErr } = await Promise.race([sbPromise, timeoutPromise]);
                    if (sbErr) throw sbErr;
                    if (sbSchemes && sbSchemes.length > 0) {
                        allSchemes = sbSchemes;
                        console.log("[SCHEMES] Loaded from Supabase:", allSchemes.length);
                    }
                } catch (sbErr) {
                    console.warn("[SCHEMES] Supabase fetch failed/timed out:", sbErr.message);
                }
            }
        }

        // 3. Last Resort: Fallback Data
        if (allSchemes.length === 0) {
            console.log("[SCHEMES] Using hardcoded fallback data.");
            allSchemes = getFallbackSchemes();
        }

        // 4. Load user applications via canonical API first
        try {
            const user = await auth.getSession();
            if (user && user.id) {
                const apiApps = await api.getMySchemeApplications().catch(() => null);
                if (apiApps && Array.isArray(apiApps) && apiApps.length > 0) {
                    userApplications = apiApps.map(a => ({
                        id: a.id,
                        applied_at: a.appliedAt || a.created_at || new Date().toISOString(),
                        application_status: a.status || "APPLIED",
                        schemes: a.scheme || { title: a.schemeTitle || "Govt Scheme" }
                    }));
                } else if (supabase && typeof supabase.from === 'function') {
                    const { data: apps } = await supabase
                        .from("scheme_applications")
                        .select("*, schemes(*)")
                        .eq("user_id", user.id);
                    userApplications = apps || [];
                }
            }
        } catch (err) {
            console.warn("[SCHEMES] Could not fetch applications:", err.message);
        }

        renderSchemesList(allSchemes);
        renderAppliedSchemesList();
        renderDeadlinesList(allSchemes);

    } catch (error) {
        console.error("[SCHEMES] Critical load error:", error);
        allSchemes = getFallbackSchemes();
        renderSchemesList(allSchemes);
    }
}

function normalizeScheme(s) {
    return {
        id: s.id,
        title: s.title || s.name || "Untitled Scheme",
        description: s.description || s.shortDescription || s.summary || "",
        category: s.category || s.scheme_category || s.type || "General",
        cropType: s.crop_type || s.cropType || s.applicableCropTypes?.[0] || "All Crops",
        state: s.state || s.applicableStates?.[0] || "All States",
        eligibility: s.eligibility || "Open to all verified farmers.",
        benefits: s.benefits || "Information available upon application.",
        documentsRequired: s.documents_required || s.documentsRequired || ["Aadhaar Card", "Land Records"],
        officialUrl: s.official_url || s.officialWebsite || "#",
        deadline: s.deadline || s.deadlineDate || null,
        imageUrl: s.image_url || s.imageUrl || ""
    };
}

function renderSchemesList(schemes) {
    const container = document.getElementById('schemes-container');
    const searchInput = document.getElementById('scheme-search')?.value.toLowerCase() || '';
    const cropFilter = document.getElementById('filter-crop')?.value || '';
    const stateFilter = document.getElementById('filter-state')?.value || '';
    const categoryFilter = document.getElementById('filter-category')?.value || '';

    if (!container) return;

    const filtered = schemes.map(normalizeScheme).filter(s => {
        if (searchInput && !s.title.toLowerCase().includes(searchInput) && !s.description.toLowerCase().includes(searchInput)) return false;
        if (cropFilter && s.cropType !== "All Crops" && s.cropType !== cropFilter) return false;
        if (stateFilter && s.state !== "All States" && s.state !== stateFilter) return false;
        if (categoryFilter && s.category !== categoryFilter) return false;
        return true;
    });

    if (filtered.length === 0) {
        container.innerHTML = `
            <div class="empty-state-modern" style="text-align: center; padding: 40px; grid-column: 1 / -1;">
                <i class="fas fa-search fa-2x" style="color: var(--border-color); margin-bottom: 15px;"></i>
                <h3 style="color: var(--text-dark); margin-bottom: 5px;">No schemes found</h3>
                <p style="color: var(--text-muted);">Try changing crop type, state, or category filters.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = '';

    filtered.forEach(scheme => {
        const categoryClass = (scheme.category || "General").toLowerCase().replace(/\s+/g, '-');
        const iconClass = getIconForCategory(scheme.category);
        
        const application = userApplications.find(app => app.scheme_id === scheme.id || (app.schemes && app.schemes.id === scheme.id));
        const statusBadge = application ? `<span class="status-badge ${application.application_status.toLowerCase()}">${application.application_status}</span>` : '';

        const card = document.createElement('div');
        card.className = 'scheme-card-horizontal';
        card.innerHTML = `
            <div class="s-icon-box ${categoryClass}">
                <i class="fas ${iconClass}"></i>
            </div>
            <div class="s-info-col">
                <div style="display: flex; align-items: center; gap: 8px;">
                    <h4>${scheme.title}</h4>
                    ${statusBadge}
                </div>
                <p title="${scheme.description}">${scheme.description}</p>
            </div>
            <div class="s-category-col">
                <span class="cat-badge ${categoryClass}">${scheme.category}</span>
            </div>
            <div class="s-eligibility-col">
                <span class="label">Eligibility</span>
                <span class="value">${scheme.eligibility.split('.')[0]}</span>
            </div>
            <div class="s-actions-col">
                <button class="s-apply-btn apply-btn-trigger">Apply Now</button>
                <a href="#" class="s-details-link view-details-trigger">Read More</a>
            </div>
        `;

        card.querySelector('.view-details-trigger').addEventListener('click', (e) => {
            e.preventDefault();
            openSchemeModal(scheme);
        });
        
        card.querySelector('.apply-btn-trigger').addEventListener('click', () => {
            handleSchemeApply(scheme);
        });

        container.appendChild(card);
    });
}

function getIconForCategory(cat) {
    cat = (cat || "").toLowerCase();
    if (cat.includes('subsidy') || cat.includes('mechanization')) return 'fa-leaf';
    if (cat.includes('insurance')) return 'fa-shield-halved';
    if (cat.includes('loan') || cat.includes('finance') || cat.includes('income')) return 'fa-indian-rupee-sign';
    if (cat.includes('infrastructure')) return 'fa-building-wheat';
    if (cat.includes('irrigation')) return 'fa-droplet';
    return 'fa-building-columns';
}

function renderAppliedSchemesList() {
    const container = document.getElementById('applied-schemes-container');
    if (!container) return;

    if (!userApplications || userApplications.length === 0) {
        container.innerHTML = `
            <div class="empty-state-modern" style="grid-column: 1 / -1; text-align: center; padding: 20px; color: var(--text-muted);">
                <p>No applied schemes yet.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = userApplications.map(app => {
        const schemeTitle = app.schemes?.title || "Unknown Scheme";
        return `
            <div class="applied-scheme-mini-card">
                <div class="mini-card-info">
                    <h5>${schemeTitle}</h5>
                    <p>Applied on: ${new Date(app.applied_at).toLocaleDateString()}</p>
                </div>
                <span class="status-badge ${app.application_status.toLowerCase()}">${app.application_status}</span>
            </div>
        `;
    }).join('');
}

function renderDeadlinesList(schemes) {
    const deadlineList = document.querySelector('.deadline-list');
    if (!deadlineList) return;

    const normalized = schemes.map(normalizeScheme).filter(s => s.deadline);
    
    if (normalized.length === 0) {
        deadlineList.innerHTML = '<p style="padding: 10px; font-size: 13px; color: var(--text-muted);">No upcoming deadlines.</p>';
        return;
    }

    const sorted = normalized.sort((a, b) => new Date(a.deadline) - new Date(b.deadline)).slice(0, 3);

    deadlineList.innerHTML = sorted.map(scheme => {
        const date = new Date(scheme.deadline);
        const day = date.getDate();
        const month = date.toLocaleString('default', { month: 'short' }).toUpperCase();
        
        return `
            <div class="deadline-item">
                <div class="date-block">
                    <span class="day">${day}</span>
                    <span class="month">${month}</span>
                </div>
                <div class="deadline-info">
                    <h5>${scheme.title}</h5>
                    <p>Deadline: ${date.toLocaleDateString()}</p>
                </div>
            </div>
        `;
    }).join('');
}

async function handleSchemeApply(scheme) {
    const user = await auth.getSession();
    if (!user) {
        alert("Please login to apply for this scheme.");
        window.location.hash = 'login';
        return;
    }

    if (!scheme.officialUrl || scheme.officialUrl === "#") {
        alert("Official application link is not available yet.");
        return;
    }

    try {
        console.log("Applying for scheme:", scheme.id);
        
        // 1. Record application in canonical Backend API
        let redirectUrl = scheme.officialUrl;
        try {
            const result = await api.applyScheme(scheme.id);
            if (result && result.officialUrl) {
                redirectUrl = result.officialUrl;
            }
        } catch (apiErr) {
            console.warn("[SCHEMES] Backend apply-click failed, continuing:", apiErr.message);
        }

        // 2. Optional Supabase mirror
        try {
            const isDemoId = String(scheme.id).startsWith("pm-") || String(scheme.id).startsWith("pmksy") || String(scheme.id).startsWith("pmfby");
            if (!isDemoId && supabase && typeof supabase.from === 'function') {
                await supabase
                    .from("scheme_applications")
                    .insert({
                        user_id: user.id,
                        scheme_id: scheme.id,
                        application_status: "pending"
                    });
            }
        } catch (supaErr) {
            console.warn("[SCHEMES] Supabase application mirror skipped:", supaErr.message);
        }
        
        alert("Redirecting to the official government portal...");
        window.open(redirectUrl, "_blank", "noopener,noreferrer");
        
        // Refresh apps list
        await loadSchemesData();

    } catch (error) {
        console.error("Error applying for scheme:", error);
        window.open(scheme.officialUrl, "_blank", "noopener,noreferrer");
    }
}

function openSchemeModal(scheme) {
    const modal = document.getElementById('scheme-modal');
    if (!modal) return;

    const modalImg = document.getElementById('modal-img');
    if (modalImg) {
        modalImg.src = scheme.imageUrl || 'https://images.unsplash.com/photo-1500382017468-9049fed747ef?q=80&w=800';
        modalImg.onerror = () => {
            modalImg.src = 'https://via.placeholder.com/800x400?text=Govt+Scheme';
        };
    }
    
    document.getElementById('modal-title').textContent = scheme.title;
    
    const modalBadge = document.getElementById('modal-badge');
    if (modalBadge) {
        modalBadge.textContent = scheme.category;
        modalBadge.className = `cat-badge ${(scheme.category || "subsidy").toLowerCase().replace(/\s+/g, '-')}`;
    }

    document.getElementById('modal-desc').textContent = scheme.description;
    document.getElementById('modal-benefits').textContent = scheme.benefits;
    document.getElementById('modal-eligibility').textContent = scheme.eligibility;
    
    const docsList = document.getElementById('modal-docs');
    if (docsList) {
        const docs = Array.isArray(scheme.documentsRequired) ? scheme.documentsRequired : 
                     (typeof scheme.documentsRequired === 'string' ? scheme.documentsRequired.split(',') : ["Aadhaar Card", "Land Records"]);
        docsList.innerHTML = docs.map(doc => `<li>${doc.trim()}</li>`).join('');
    }

    const modalApplyBtn = document.getElementById('modal-apply-btn');
    if (modalApplyBtn) {
        modalApplyBtn.onclick = () => {
            handleSchemeApply(scheme);
        };
    }

    modal.style.display = 'flex';
}

window.initSchemesPage = initSchemes;