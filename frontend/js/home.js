import { api } from './api.js';
import { auth } from './auth.js';
import { 
    fallbackHomeSummary, 
    fallbackCrops, 
    fallbackTasks, 
    fallbackCommunitySnapshot, 
    fallbackMarketplaceSummary 
} from './data/fallbackHomeData.js';

let showingArchivedCrops = false;
let currentCropsCache = [];

function normalizeStatus(status = "") {
    return status.toLowerCase().replace(/\s+/g, "-");
}

export async function initDashboard() {
    console.log("Initializing Personalized Farmer Workspace (Phase 7)...");
    
    // 1. Hydrate Personalized Farmer Header
    await hydrateFarmerWorkspaceHeader();

    // 2. Fetch live data concurrently
    try {
        const [summary, crops, tasks, diagnoses, marketRates, schemes, community] = await Promise.all([
            api.getHomeSummary().catch(() => fallbackHomeSummary),
            api.getCrops(showingArchivedCrops).catch(() => fallbackCrops),
            api.getTodayTasks().catch(() => fallbackTasks),
            api.getCropHistory().catch(() => []),
            api.getMarketRates().catch(() => []),
            api.getSchemes().catch(() => []),
            api.getCommunitySnapshot().catch(() => fallbackCommunitySnapshot)
        ]);

        currentCropsCache = Array.isArray(crops) ? crops : [];

        renderSummary(summary);
        renderSmartCrops(currentCropsCache);
        renderTasks(tasks);
        renderDiagnosisHistory(diagnoses);
        renderMandiRates(marketRates);
        renderRecommendedSchemes(schemes, currentCropsCache);
        renderCommunitySnapshot(community);

    } catch (error) {
        console.error("Dashboard initialization error:", error);
        renderSummary(fallbackHomeSummary);
        renderSmartCrops(fallbackCrops);
        renderTasks(fallbackTasks);
        renderCommunitySnapshot(fallbackCommunitySnapshot);
    }

    // 3. Setup Interactive Crop Modal & Filter Listeners
    setupCropModalAndFilters();
}

async function hydrateFarmerWorkspaceHeader() {
    try {
        const user = await auth.getSession();
        const profile = auth.farmerProfile || user || {};

        const nameEl = document.getElementById('workspace-farmer-name');
        const idEl = document.getElementById('workspace-farmer-id');
        const locEl = document.getElementById('workspace-location');
        const landEl = document.getElementById('workspace-land');
        const langEl = document.getElementById('workspace-lang-badge');

        const displayName = profile.fullName || profile.full_name || (user?.email ? user.email.split('@')[0] : "Farmer");
        const farmerCode = profile.farmerId || profile.farmer_code || "AGV-01";
        const locationStr = (profile.city && profile.state) ? `${profile.city}, ${profile.state}` : (profile.city || profile.address || "Nashik, Maharashtra");
        const landHolding = profile.landHolding ? `${profile.landHolding} ${profile.landUnit || 'Acres'}` : (profile.land_holding ? `${profile.land_holding} Acres` : "5.0 Acres");
        
        const langCode = localStorage.getItem('agroverse_language') || 'en';
        const langMap = { en: "English", hi: "हिंदी", mr: "मराठी" };

        if (nameEl) nameEl.textContent = `Namaste, ${displayName.split(' ')[0]}! 🌱`;
        if (idEl) idEl.textContent = `ID: #${farmerCode}`;
        if (locEl) locEl.textContent = locationStr;
        if (landEl) landEl.textContent = landHolding;
        if (langEl) langEl.textContent = `Lang: ${langMap[langCode] || "English"}`;

    } catch (err) {
        console.warn("Could not hydrate workspace header:", err);
    }
}

function renderSummary(data) {
    const elActiveCrops = document.getElementById('stat-active-crops');
    const elActiveListings = document.getElementById('stat-active-listings');
    const elNotifications = document.getElementById('stat-notifications');
    const elTasks = document.getElementById('stat-tasks');

    if (elActiveCrops) elActiveCrops.textContent = data.activeCrops || 0;
    if (elActiveListings) elActiveListings.textContent = data.activeListings || 0;
    if (elNotifications) elNotifications.textContent = data.newNotifications || 0;
    if (elTasks) elTasks.textContent = data.tasksToday || 0;

    const notifBadge = document.getElementById('headerNotificationBadge');
    if (notifBadge && typeof data.newNotifications === 'number') {
        if (data.newNotifications > 0) {
            notifBadge.textContent = data.newNotifications > 9 ? '9+' : data.newNotifications;
            notifBadge.style.display = 'inline-block';
        } else {
            notifBadge.style.display = 'none';
        }
    }
}

function renderSmartCrops(crops) {
    const container = document.getElementById('home-crops-list');
    if (!container) return;

    if (!crops || crops.length === 0) {
        container.innerHTML = `
            <div class="empty-state" style="text-align: center; padding: 30px; color: var(--text-muted, #6b7280);">
                <i class="fas fa-seedling" style="font-size: 28px; margin-bottom: 8px; opacity: 0.3;"></i>
                <p style="margin: 0; font-size: 13px;">No ${showingArchivedCrops ? 'archived' : 'active'} crops recorded yet.</p>
                <button id="empty-add-crop-btn" class="btn btn-sm btn-primary" style="margin-top: 10px; font-size: 12px; padding: 6px 14px; border-radius: 6px; cursor: pointer;">
                    <i class="fas fa-plus"></i> Add Your First Crop
                </button>
            </div>
        `;
        document.getElementById('empty-add-crop-btn')?.addEventListener('click', openAddCropModal);
        return;
    }

    container.innerHTML = crops.map(crop => {
        const stage = crop.growthStage || 'Vegetative';
        const variety = crop.variety ? `(${crop.variety})` : '';
        const sowingStr = crop.sowingDate ? `Sown: ${crop.sowingDate}` : '';
        const harvestStr = crop.expectedHarvestDate ? `Est. Harvest: ${crop.expectedHarvestDate}` : '';
        const isArchived = !!crop.archived;

        // Stage Color Map
        const stageColors = {
            'Seedling': { bg: 'rgba(59, 130, 246, 0.12)', text: '#2563eb' },
            'Vegetative': { bg: 'rgba(22, 163, 74, 0.12)', text: '#16a34a' },
            'Flowering': { bg: 'rgba(234, 88, 12, 0.12)', text: '#ea580c' },
            'Fruiting': { bg: 'rgba(168, 85, 247, 0.12)', text: '#9333ea' },
            'Mature': { bg: 'rgba(202, 138, 4, 0.12)', text: '#ca8a04' },
            'Harvest-Ready': { bg: 'rgba(13, 148, 136, 0.12)', text: '#0d9488' }
        };
        const currentStageStyle = stageColors[stage] || stageColors['Vegetative'];

        return `
            <div class="crop-lifecycle-card" data-crop-id="${crop.id}" style="display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; background: var(--bg-surface-soft, #f8fafc); border: 1px solid var(--border-color, #e5e7eb); border-radius: 10px; gap: 12px; transition: transform 0.2s, box-shadow 0.2s;">
                <div style="display: flex; align-items: center; gap: 12px; flex: 1;">
                    <img src="${crop.image || 'assets/tomato.png'}" onerror="this.src='assets/favicon.png'" style="width: 44px; height: 44px; border-radius: 8px; object-fit: cover; border: 1px solid var(--border-color, #e5e7eb);">
                    <div>
                        <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
                            <strong style="font-size: 14px; color: var(--text-primary, #111827);">${crop.name} ${variety}</strong>
                            <span class="badge" style="background: ${currentStageStyle.bg}; color: ${currentStageStyle.text}; font-size: 10px; font-weight: 700; padding: 2px 8px; border-radius: 12px;">
                                ${stage}
                            </span>
                            <span style="font-size: 11px; color: var(--text-muted, #6b7280);"><i class="fas fa-map-pin"></i> ${crop.field}</span>
                        </div>
                        <div style="font-size: 12px; color: var(--text-muted, #6b7280); margin-top: 3px; display: flex; gap: 12px; flex-wrap: wrap;">
                            ${sowingStr ? `<span><i class="far fa-calendar-alt"></i> ${sowingStr}</span>` : ''}
                            ${harvestStr ? `<span style="color: var(--primary-green, #16a34a); font-weight: 600;"><i class="fas fa-hourglass-half"></i> ${harvestStr}</span>` : ''}
                            ${crop.irrigationNotes ? `<span><i class="fas fa-droplet" style="color: #3b82f6;"></i> ${crop.irrigationNotes}</span>` : ''}
                        </div>
                    </div>
                </div>

                <div style="display: flex; align-items: center; gap: 6px; flex-shrink: 0;">
                    <button class="btn btn-sm btn-select-crop" data-crop-id="${crop.id}" title="Set active crop for AI advice" style="padding: 6px 10px; font-size: 11px; background: rgba(22, 163, 74, 0.1); color: var(--primary-green, #16a34a); border: 1px solid rgba(22, 163, 74, 0.2); border-radius: 6px; cursor: pointer;">
                        <i class="fas fa-bullseye"></i> Target AI
                    </button>
                    <button class="btn btn-sm btn-edit-crop" data-crop-id="${crop.id}" title="Edit Crop" style="padding: 6px 10px; font-size: 11px; background: var(--bg-surface, #ffffff); border: 1px solid var(--border-color, #e5e7eb); border-radius: 6px; cursor: pointer;">
                        <i class="fas fa-pencil-alt"></i>
                    </button>
                    <button class="btn btn-sm btn-archive-crop" data-crop-id="${crop.id}" title="${isArchived ? 'Restore' : 'Archive'}" style="padding: 6px 10px; font-size: 11px; background: var(--bg-surface, #ffffff); border: 1px solid var(--border-color, #e5e7eb); border-radius: 6px; cursor: pointer; color: ${isArchived ? 'var(--primary-green)' : 'var(--text-muted)'};">
                        <i class="fas ${isArchived ? 'fa-box-open' : 'fa-box-archive'}"></i>
                    </button>
                </div>
            </div>
        `;
    }).join('');

    // Attach card event listeners
    container.querySelectorAll('.btn-select-crop').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            const cropId = btn.dataset.cropId;
            const crop = currentCropsCache.find(c => String(c.id) === String(cropId));
            if (crop) {
                localStorage.setItem('agroverse_active_crop', JSON.stringify(crop));
                btn.innerHTML = '<i class="fas fa-check"></i> Active';
                setTimeout(() => { btn.innerHTML = '<i class="fas fa-bullseye"></i> Target AI'; }, 2000);
            }
        });
    });

    container.querySelectorAll('.btn-edit-crop').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            const cropId = btn.dataset.cropId;
            const crop = currentCropsCache.find(c => String(c.id) === String(cropId));
            if (crop) openEditCropModal(crop);
        });
    });

    container.querySelectorAll('.btn-archive-crop').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            e.stopPropagation();
            const cropId = btn.dataset.cropId;
            try {
                await api.archiveCrop(cropId);
                const updated = await api.getCrops(showingArchivedCrops);
                currentCropsCache = updated;
                renderSmartCrops(currentCropsCache);
            } catch (err) {
                console.warn("Could not toggle archive for crop:", err);
            }
        });
    });
}

function renderTasks(tasks) {
    const container = document.getElementById('home-tasks-list');
    if (!container) return;

    if (!tasks || tasks.length === 0) {
        container.innerHTML = '<div class="empty-state" style="padding: 24px; text-align: center; color: var(--text-muted, #6b7280); font-size: 13px;">No tasks scheduled for today. Great job!</div>';
        return;
    }

    container.innerHTML = tasks.slice(0, 4).map(task => {
        const timeStr = task.dueTime || task.time || 'Today';
        const iconClass = (task.icon && task.icon.startsWith('fa-')) ? task.icon : (task.icon ? `fa-${task.icon}` : 'fa-clipboard-check');
        const color = task.color || '#16a34a';
        const isDone = !!task.completed;

        return `
            <div class="task-item-row" data-task-id="${task.id}" style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-bottom: 1px solid var(--border-color, #e5e7eb); gap: 10px; cursor: pointer; ${isDone ? 'opacity: 0.6;' : ''}">
                <div style="display: flex; align-items: center; gap: 12px; flex: 1;">
                    <div style="width: 34px; height: 34px; border-radius: 50%; background: rgba(22, 163, 74, 0.1); color: ${color}; display: flex; align-items: center; justify-content: center; font-size: 15px; flex-shrink: 0;">
                        <i class="fas ${iconClass}"></i>
                    </div>
                    <div>
                        <h4 style="margin: 0; font-size: 13px; font-weight: 700; color: var(--text-primary, #111827); ${isDone ? 'text-decoration: line-through;' : ''}">${task.title}</h4>
                        <p style="margin: 2px 0 0; font-size: 11px; color: var(--text-muted, #6b7280);">${task.subtitle || task.type || 'Field Activity'}</p>
                    </div>
                </div>
                <div style="display: flex; align-items: center; gap: 10px;">
                    <span style="font-size: 11px; font-weight: 700; color: ${color};">${timeStr}</span>
                    <button class="btn btn-sm btn-quick-task-toggle" data-task-id="${task.id}" style="background: ${isDone ? '#9ca3af' : 'var(--primary-green, #16a34a)'}; color: white; border: none; border-radius: 6px; padding: 4px 8px; font-size: 11px; cursor: pointer;">
                        <i class="fas ${isDone ? 'fa-undo' : 'fa-check'}"></i>
                    </button>
                </div>
            </div>
        `;
    }).join('');

    container.querySelectorAll('.btn-quick-task-toggle').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            e.stopPropagation();
            const taskId = btn.dataset.taskId;
            try {
                await api.toggleTask(taskId);
                const updatedTasks = await api.getTodayTasks();
                renderTasks(updatedTasks);
            } catch (err) {
                console.warn("Could not toggle task:", err);
            }
        });
    });
}

function renderDiagnosisHistory(diagnoses) {
    const container = document.getElementById('home-diagnosis-list');
    if (!container) return;

    if (!diagnoses || !Array.isArray(diagnoses) || diagnoses.length === 0) {
        container.innerHTML = `
            <div style="text-align: center; padding: 24px; color: var(--text-muted, #6b7280);">
                <i class="fas fa-camera" style="font-size: 26px; margin-bottom: 8px; opacity: 0.3;"></i>
                <p style="margin: 0; font-size: 13px;">No leaf health scans recorded yet.</p>
                <button onclick="window.location.hash='#crop'" class="btn btn-sm btn-primary" style="margin-top: 10px; font-size: 12px; padding: 6px 14px; border-radius: 6px; cursor: pointer;">
                    Scan Crop Leaf Now
                </button>
            </div>
        `;
        return;
    }

    container.innerHTML = diagnoses.slice(0, 3).map(d => {
        const isHealthy = (d.disease || '').toLowerCase().includes('healthy');
        const badgeColor = isHealthy ? '#16a34a' : '#ef4444';
        const badgeBg = isHealthy ? 'rgba(22, 163, 74, 0.1)' : 'rgba(239, 68, 68, 0.1)';
        const dateStr = d.createdAt ? new Date(d.createdAt).toLocaleDateString('en-IN', { month: 'short', day: 'numeric' }) : 'Recent';

        return `
            <div style="display: flex; justify-content: space-between; align-items: center; padding: 10px 12px; background: var(--bg-surface-soft, #f8fafc); border: 1px solid var(--border-color, #e5e7eb); border-radius: 8px;">
                <div>
                    <div style="display: flex; align-items: center; gap: 8px;">
                        <strong style="font-size: 13px; color: var(--text-primary, #111827);">${d.crop || 'Crop'}</strong>
                        <span class="badge" style="background: ${badgeBg}; color: ${badgeColor}; font-size: 10px; font-weight: 700; padding: 2px 6px; border-radius: 10px;">
                            ${d.disease || 'Analyzed'}
                        </span>
                    </div>
                    <span style="font-size: 11px; color: var(--text-muted, #6b7280);">${d.diagnosticCertainty || 'ANALYZED'} &bull; ${d.confidence || 0}% confidence</span>
                </div>
                <div style="text-align: right;">
                    <span style="font-size: 11px; color: var(--text-muted, #6b7280);">${dateStr}</span>
                </div>
            </div>
        `;
    }).join('');
}

function renderMandiRates(rates) {
    const container = document.getElementById('home-mandi-rates-list');
    if (!container) return;

    if (!rates || !Array.isArray(rates) || rates.length === 0) {
        container.innerHTML = '<p style="text-align: center; padding: 20px; font-size: 12px; color: var(--text-muted);">Mandi market prices temporarily unavailable.</p>';
        return;
    }

    container.innerHTML = rates.slice(0, 4).map(r => `
        <div style="display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; background: var(--bg-surface-soft, #f8fafc); border: 1px solid var(--border-color, #e5e7eb); border-radius: 8px;">
            <div>
                <strong style="font-size: 13px; color: var(--text-primary, #111827); display: block;">${r.commodity}</strong>
                <span style="font-size: 11px; color: var(--text-muted, #6b7280);">${r.market}, ${r.state || ''}</span>
            </div>
            <div style="text-align: right;">
                <span style="font-size: 13px; font-weight: 700; color: var(--primary-green, #16a34a);">₹${r.modalPrice}/q</span>
                <span style="font-size: 10px; color: var(--text-muted, #6b7280); display: block;">Range: ₹${r.minPrice} - ₹${r.maxPrice}</span>
            </div>
        </div>
    `).join('');
}

function renderRecommendedSchemes(schemes, crops) {
    const container = document.getElementById('home-recommended-schemes');
    if (!container) return;

    if (!schemes || !Array.isArray(schemes) || schemes.length === 0) {
        container.innerHTML = '<p style="text-align: center; padding: 15px; font-size: 12px; color: var(--text-muted);">No government schemes loaded.</p>';
        return;
    }

    container.innerHTML = schemes.slice(0, 2).map(s => `
        <div style="padding: 10px 12px; background: var(--bg-surface-soft, #f8fafc); border: 1px solid var(--border-color, #e5e7eb); border-radius: 8px;">
            <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 8px;">
                <strong style="font-size: 13px; color: var(--text-primary, #111827);">${s.title || s.name}</strong>
                <span class="badge" style="background: rgba(22, 163, 74, 0.1); color: var(--primary-green, #16a34a); font-size: 10px; font-weight: 700;">${s.category || 'Subsidy'}</span>
            </div>
            <p style="margin: 4px 0 0; font-size: 12px; color: var(--text-muted, #6b7280); line-height: 1.4;">${s.description || s.shortDescription || 'Direct financial support and subsidy for verified farmers.'}</p>
        </div>
    `).join('');
}

function renderCommunitySnapshot(data) {
    const container = document.getElementById('home-community-card');
    if (!container) return;

    const headerEl = container.querySelector('.panel-header');
    const header = headerEl ? headerEl.outerHTML : '<h3>Community Snapshot</h3>';
    
    container.innerHTML = `
        ${header}
        <div style="display: flex; flex-direction: column; gap: 12px; margin-top: 12px; height: 100%;">
            <div style="display: flex; gap: 10px; align-items: center;">
                <img src="${data.avatar || 'https://ui-avatars.com/api/?name=' + encodeURIComponent(data.author)}" alt="Avatar" style="width: 36px; height: 36px; border-radius: 50%;">
                <div>
                    <h4 style="margin: 0; font-size: 13px; font-weight: 700;">${data.author}</h4>
                    <span style="font-size: 11px; color: var(--text-muted);">2 hours ago</span>
                </div>
            </div>
            <p style="font-size: 13px; font-weight: 600; margin: 0; line-height: 1.4;">What is the best bio-fertilizer schedule for tomato flowering?</p>
            <div style="font-size: 11px; color: var(--text-muted);">
                ${data.likes || 14} Likes &bull; ${data.comments || 6} Answers
            </div>
            <button onclick="window.location.hash='#community'" class="btn btn-secondary w-full" style="margin-top: auto; padding: 8px 12px; font-size: 12px;">Go to Community &rarr;</button>
        </div>
    `;
}

// --- Smart Crop Lifecycle Modal Control ---

function setupCropModalAndFilters() {
    const openAddBtn = document.getElementById('open-add-crop-btn');
    const panelAddBtn = document.getElementById('btn-add-crop-panel');
    const closeBtn = document.getElementById('close-crop-modal-btn');
    const modal = document.getElementById('crop-lifecycle-modal');
    const form = document.getElementById('crop-lifecycle-form');
    const archiveBtn = document.getElementById('crop-modal-archive-btn');

    const btnFilterActive = document.getElementById('btn-filter-active-crops');
    const btnFilterArchived = document.getElementById('btn-filter-archived-crops');

    openAddBtn?.addEventListener('click', openAddCropModal);
    panelAddBtn?.addEventListener('click', openAddCropModal);

    closeBtn?.addEventListener('click', () => {
        if (modal) modal.style.display = 'none';
    });

    if (modal) {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) modal.style.display = 'none';
        });
    }

    if (btnFilterActive && btnFilterArchived) {
        btnFilterActive.addEventListener('click', async () => {
            showingArchivedCrops = false;
            btnFilterActive.style.fontWeight = '700';
            btnFilterActive.style.color = 'var(--primary-green, #16a34a)';
            btnFilterArchived.style.fontWeight = '400';
            btnFilterArchived.style.color = 'var(--text-muted, #6b7280)';
            currentCropsCache = await api.getCrops(false);
            renderSmartCrops(currentCropsCache);
        });

        btnFilterArchived.addEventListener('click', async () => {
            showingArchivedCrops = true;
            btnFilterArchived.style.fontWeight = '700';
            btnFilterArchived.style.color = 'var(--primary-green, #16a34a)';
            btnFilterActive.style.fontWeight = '400';
            btnFilterActive.style.color = 'var(--text-muted, #6b7280)';
            currentCropsCache = await api.getCrops(true);
            const onlyArchived = currentCropsCache.filter(c => c.archived);
            renderSmartCrops(onlyArchived);
        });
    }

    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const id = document.getElementById('crop-modal-id').value;
            const submitBtn = document.getElementById('crop-modal-submit-btn');

            const payload = {
                name: document.getElementById('crop-modal-name').value.trim(),
                variety: document.getElementById('crop-modal-variety').value.trim(),
                field: document.getElementById('crop-modal-field').value.trim(),
                growthStage: document.getElementById('crop-modal-stage').value,
                sowingDate: document.getElementById('crop-modal-sowing').value,
                expectedHarvestDate: document.getElementById('crop-modal-harvest').value,
                irrigationNotes: document.getElementById('crop-modal-irrigation').value.trim(),
                fertilizerNotes: document.getElementById('crop-modal-fertilizer').value.trim()
            };

            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Saving...';

            try {
                if (id) {
                    await api.updateCrop(id, payload);
                } else {
                    await api.addCrop(payload);
                }

                if (modal) modal.style.display = 'none';
                form.reset();

                // Refresh dashboard crops & stats
                const [crops, summary] = await Promise.all([
                    api.getCrops(showingArchivedCrops),
                    api.getHomeSummary()
                ]);
                currentCropsCache = crops;
                renderSmartCrops(currentCropsCache);
                renderSummary(summary);

            } catch (err) {
                console.error("Failed to save crop:", err);
                alert("Error saving crop details. Please try again.");
            } finally {
                submitBtn.disabled = false;
                submitBtn.innerHTML = 'Save Crop Record';
            }
        });
    }

    if (archiveBtn) {
        archiveBtn.addEventListener('click', async () => {
            const id = document.getElementById('crop-modal-id').value;
            if (!id) return;
            try {
                await api.archiveCrop(id);
                if (modal) modal.style.display = 'none';
                const crops = await api.getCrops(showingArchivedCrops);
                currentCropsCache = crops;
                renderSmartCrops(currentCropsCache);
            } catch (err) {
                console.warn("Archive error:", err);
            }
        });
    }
}

function openAddCropModal() {
    const modal = document.getElementById('crop-lifecycle-modal');
    const form = document.getElementById('crop-lifecycle-form');
    const titleEl = document.getElementById('crop-modal-title');
    const archiveBtn = document.getElementById('crop-modal-archive-btn');

    if (form) form.reset();
    document.getElementById('crop-modal-id').value = '';
    if (titleEl) titleEl.textContent = 'Log New Crop';
    if (archiveBtn) archiveBtn.style.display = 'none';
    if (modal) modal.style.display = 'flex';
}

function openEditCropModal(crop) {
    const modal = document.getElementById('crop-lifecycle-modal');
    const titleEl = document.getElementById('crop-modal-title');
    const archiveBtn = document.getElementById('crop-modal-archive-btn');

    document.getElementById('crop-modal-id').value = crop.id || '';
    document.getElementById('crop-modal-name').value = crop.name || '';
    document.getElementById('crop-modal-variety').value = crop.variety || '';
    document.getElementById('crop-modal-field').value = crop.field || '';
    document.getElementById('crop-modal-stage').value = crop.growthStage || 'Vegetative';
    document.getElementById('crop-modal-sowing').value = crop.sowingDate || '';
    document.getElementById('crop-modal-harvest').value = crop.expectedHarvestDate || '';
    document.getElementById('crop-modal-irrigation').value = crop.irrigationNotes || '';
    document.getElementById('crop-modal-fertilizer').value = crop.fertilizerNotes || '';

    if (titleEl) titleEl.textContent = `Edit Crop — ${crop.name}`;
    if (archiveBtn) {
        archiveBtn.style.display = 'inline-block';
        archiveBtn.textContent = crop.archived ? 'Restore Crop' : 'Archive Crop';
    }

    if (modal) modal.style.display = 'flex';
}

window.initHomePage = initDashboard;