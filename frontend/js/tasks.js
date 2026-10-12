import { api } from './api.js';
import { fallbackTasks } from './data/fallbackHomeData.js';

export async function initTasks() {
    const container = document.getElementById('full-tasks-list');
    if (!container) return;

    try {
        const tasks = await api.getTodayTasks().catch(() => fallbackTasks);
        renderTasks(container, tasks);
    } catch (error) {
        console.error("Failed to load tasks:", error);
        renderTasks(container, fallbackTasks);
    }
}

function renderTasks(container, tasks) {
    if (tasks.length === 0) {
        container.innerHTML = '<div class="empty-state">No tasks scheduled for today. Take a rest!</div>';
        return;
    }

    container.innerHTML = tasks.map(task => `
        <div class="task-item" style="padding: 15px; border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center;">
            <div class="task-info" style="display: flex; gap: 15px; align-items: center;">
                <div class="task-circle" style="background: ${task.bg}; color: ${task.color}; width: 40px; height: 40px; border-radius: 50%; display: flex; align-items: center; justify-content: center;">
                    <i class="fas ${task.icon}"></i>
                </div>
                <div class="task-text">
                    <h4 style="margin: 0; font-size: 16px; color: var(--text-main);">${task.title}</h4>
                    <p style="margin: 4px 0 0; font-size: 13px; color: var(--text-muted);">${task.subtitle}</p>
                </div>
            </div>
            <div style="text-align: right;">
                <span class="task-date" style="color: ${task.color}; font-weight: 600; display: block; margin-bottom: 5px;">${task.time}</span>
                <button style="background: var(--primary); color: white; border: none; border-radius: 6px; padding: 5px 10px; font-size: 12px; cursor: pointer;">Mark Complete</button>
            </div>
        </div>
    `).join('');
}
