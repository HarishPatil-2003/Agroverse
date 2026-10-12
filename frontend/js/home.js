import { api } from './api.js';
import { 
    fallbackHomeSummary, 
    fallbackCrops, 
    fallbackTasks, 
    fallbackCommunitySnapshot, 
    fallbackMarketplaceSummary 
} from './data/fallbackHomeData.js';

function normalizeStatus(status = "") {
    return status.toLowerCase().replace(/\s+/g, "-");
}

export async function initDashboard() {
    console.log("Initializing Dashboard...");
    try {
        // Attempt to fetch live data
        const [summary, crops, tasks, community, marketplace] = await Promise.all([
            api.getHomeSummary().catch(() => fallbackHomeSummary),
            api.getCrops().catch(() => fallbackCrops),
            api.getTodayTasks().catch(() => fallbackTasks),
            api.getCommunitySnapshot().catch(() => fallbackCommunitySnapshot),
            api.getMarketplaceSummary().catch(() => fallbackMarketplaceSummary)
        ]);

        renderSummary(summary);
        renderCrops(crops);
        renderTasks(tasks);
        renderCommunitySnapshot(community);
        renderMarketplaceSummary(marketplace);

    } catch (error) {
        console.error("Dashboard initialization error:", error);
        // Fallback rendering
        renderSummary(fallbackHomeSummary);
        renderCrops(fallbackCrops);
        renderTasks(fallbackTasks);
        renderCommunitySnapshot(fallbackCommunitySnapshot);
        renderMarketplaceSummary(fallbackMarketplaceSummary);
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
}

function renderCrops(crops) {
    const container = document.getElementById('home-crops-list');
    if (!container) return;
    
    if (crops.length === 0) {
        container.innerHTML = '<div class="empty-state">No crops found.</div>';
        return;
    }

    container.innerHTML = crops.map(crop => {
        let action = 'View Details';
        if (crop.status.toLowerCase().includes('good') || crop.status.toLowerCase().includes('healthy')) action = 'Continue regular irrigation';
        else if (crop.status.toLowerCase().includes('warning') || crop.status.toLowerCase().includes('attention')) action = 'Apply fungicide Mancozeb';
        else if (crop.status.toLowerCase().includes('critical') || crop.status.toLowerCase().includes('danger')) action = 'Use Neem-based pesticide';

        return `
        <div class="crop-item" onclick="window.location.hash='#crop'" style="display: grid; grid-template-columns: 1fr 1fr 1.2fr 1.5fr 20px; align-items: center; background: transparent; border: none; padding: 12px 10px; border-bottom: 1px solid var(--border-color); border-radius: 0; cursor:pointer;">
            <div style="display: flex; align-items: center; gap: 10px;">
                <img src="${crop.image}" onerror="this.src='assets/favicon.png'" style="width: 35px; height: 35px; border-radius: 6px; object-fit: cover;">
                <span style="font-size: 13px; font-weight: 700;">${crop.name}</span>
            </div>
            <div>
                <span class="status-badge ${normalizeStatus(crop.status)}" style="display: inline-flex; align-items: center; gap: 6px; padding: 4px 10px; font-size: 11px; text-transform: capitalize;">
                    <span style="width: 6px; height: 6px; border-radius: 50%; background: currentColor;"></span> ${crop.status}
                </span>
            </div>
            <div style="font-size: 12px;">
                <div style="font-weight: 700; color: var(--text-main); margin-bottom: 2px;">${crop.lastChecked}</div>
                <div style="color: var(--text-light); font-size: 11px;">${crop.field}</div>
            </div>
            <div style="font-size: 12px; color: var(--text-main); line-height: 1.4;">${action}</div>
            <i class="fas fa-chevron-right" style="color: var(--text-light); font-size: 12px;"></i>
        </div>
    `}).join('');
}

function renderTasks(tasks) {
    const container = document.getElementById('home-tasks-list');
    if (!container) return;

    if (tasks.length === 0) {
        container.innerHTML = '<div class="empty-state">No tasks for today.</div>';
        return;
    }

    container.innerHTML = tasks.map(task => `
        <div class="task-item" onclick="window.location.hash='#tasks'" style="display: flex; align-items: flex-start; gap: 15px; padding: 12px 0; border-bottom: 1px solid var(--border-color); cursor:pointer;">
            <div style="width: 35px; height: 35px; border-radius: 50%; background: ${task.bg}; color: ${task.color}; display: flex; align-items: center; justify-content: center; font-size: 16px; flex-shrink: 0;">
                <i class="fas ${task.icon}"></i>
            </div>
            <div style="flex: 1;">
                <h4 style="font-size: 14px; font-weight: 700; margin: 0 0 4px 0;">${task.title}</h4>
                <p style="font-size: 12px; color: var(--text-light); margin: 0;">${task.subtitle}</p>
            </div>
            <span style="font-size: 12px; font-weight: 700; color: ${task.color};">${task.time}</span>
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
        <div style="display: flex; flex-direction: column; gap: 15px; margin-top: 15px; height: 100%;">
            <div style="display: flex; gap: 12px; align-items: center;">
                <img src="${data.avatar || 'https://ui-avatars.com/api/?name=' + encodeURIComponent(data.author)}" alt="Avatar" style="width: 40px; height: 40px; border-radius: 50%;">
                <div>
                    <h4 style="margin: 0 0 4px 0; font-size: 14px; font-weight: 700;">${data.author}</h4>
                    <span style="font-size: 11px; color: var(--text-light);">2 hours ago</span>
                </div>
            </div>
            <p style="font-size: 14px; font-weight: 600; margin: 0; line-height: 1.5;">What is the best organic pesticide for tomato leaf spot?</p>
            <div style="font-size: 12px; color: var(--text-light);">
                ${data.likes} Likes &bull; ${data.comments} Comments
            </div>
            <button onclick="window.location.hash='#community'" class="btn btn-secondary w-full" style="margin-top: auto;">Go to Community &rarr;</button>
        </div>
    `;
}

function renderMarketplaceSummary(data) {
    const container = document.getElementById('home-marketplace-card');
    if (!container) return;

    const headerEl = container.querySelector('.panel-header');
    const header = headerEl ? headerEl.outerHTML : '<h3>Marketplace Summary</h3>';

    container.innerHTML = `
        ${header}
        <div style="display: flex; flex-direction: column; gap: 15px; margin-top: 15px; height: 100%;">
            <div style="display: flex; gap: 15px; align-items: center;">
                <div style="width: 45px; height: 45px; border-radius: 50%; background: var(--color-success-bg); color: var(--brand-emerald-500); display: flex; align-items: center; justify-content: center; font-size: 20px;"><i class="fas fa-tractor"></i></div>
                <div>
                    <span style="font-size: 12px; font-weight: 600; display: block; margin-bottom: 2px;">Total Products</span>
                    <strong style="font-size: 14px;">${data.totalProducts}</strong> <span style="font-size: 11px; color: var(--text-light); margin-left: 5px;">Active Listings</span>
                </div>
            </div>
            <div style="display: flex; gap: 15px; align-items: center;">
                <div style="width: 45px; height: 45px; border-radius: 50%; background: var(--color-warning-bg); color: var(--color-warning); display: flex; align-items: center; justify-content: center; font-size: 20px;"><i class="fas fa-store"></i></div>
                <div>
                    <span style="font-size: 12px; font-weight: 600; display: block; margin-bottom: 2px;">Recent Listing</span>
                    <strong style="font-size: 14px;">Organic Wheat Seeds</strong>
                    <div style="font-size: 11px; color: var(--text-light); margin-top: 2px;">2 hours ago</div>
                </div>
            </div>
            <button onclick="window.location.hash='#marketplace?openAddProduct=true'" class="btn btn-secondary w-full" style="margin-top: auto;">Add New Product &rarr;</button>
        </div>
    `;
}

window.initHomePage = initDashboard;