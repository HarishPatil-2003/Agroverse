import { api } from './api.js';
import { fallbackTasks } from './data/fallbackHomeData.js';

export async function initTasks() {
    const container = document.getElementById('full-tasks-list');
    if (!container) return;

    try {
        const res = await api.getTodayTasks();
        const tasks = (Array.isArray(res) && res.length > 0) ? res : fallbackTasks;
        renderTasks(container, tasks);
    } catch (error) {
        console.warn("Failed to load tasks from API, using fallback data:", error);
        renderTasks(container, fallbackTasks);
    }
}

function renderTasks(container, tasks) {
    if (!tasks || tasks.length === 0) {
        container.innerHTML = '<div class="empty-state" style="text-align: center; padding: 40px; color: var(--text-light, #6b7280);">No tasks scheduled for today. Great job keeping your farm organized!</div>';
        return;
    }

    container.innerHTML = tasks.map(task => {
        const timeStr = task.dueTime || task.time || 'Scheduled';
        const iconClass = (task.icon && task.icon.startsWith('fa-')) ? task.icon : (task.icon ? `fa-${task.icon}` : 'fa-check');
        const isCompleted = !!task.completed;
        const color = task.color || '#16a34a';
        const bg = task.bg || 'rgba(22, 163, 74, 0.1)';

        return `
            <div class="task-card-row" id="task-row-${task.id}" style="padding: 16px; border-bottom: 1px solid var(--border-color, #e5e7eb); display: flex; justify-content: space-between; align-items: center; gap: 15px; transition: opacity 0.2s; ${isCompleted ? 'opacity: 0.6;' : ''}">
                <div class="task-info" style="display: flex; gap: 14px; align-items: center; flex: 1;">
                    <div class="task-circle" style="background: ${bg}; color: ${color}; width: 42px; height: 42px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 18px; flex-shrink: 0;">
                        <i class="fas ${iconClass}"></i>
                    </div>
                    <div class="task-text">
                        <h4 style="margin: 0; font-size: 15px; font-weight: 700; color: var(--text-main, #111827); ${isCompleted ? 'text-decoration: line-through;' : ''}">${task.title}</h4>
                        <p style="margin: 4px 0 0; font-size: 13px; color: var(--text-light, #6b7280);">${task.subtitle || task.type || 'Field Activity'}</p>
                    </div>
                </div>
                <div style="display: flex; align-items: center; gap: 12px; flex-shrink: 0;">
                    <span class="task-date" style="color: ${color}; font-weight: 700; font-size: 13px;">${timeStr}</span>
                    <button class="task-toggle-btn btn btn-sm" data-task-id="${task.id}" style="background: ${isCompleted ? '#6b7280' : 'var(--primary-green, #16a34a)'}; color: white; border: none; border-radius: 6px; padding: 6px 12px; font-size: 12px; font-weight: 600; cursor: pointer;">
                        ${isCompleted ? '<i class="fas fa-undo"></i> Reopen' : '<i class="fas fa-check"></i> Complete'}
                    </button>
                </div>
            </div>
        `;
    }).join('');

    // Attach toggle listeners
    container.querySelectorAll('.task-toggle-btn').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            const taskId = btn.dataset.taskId;
            if (!taskId) return;
            btn.disabled = true;
            try {
                await api.toggleTask(taskId);
                await initTasks();
            } catch (err) {
                console.warn("Could not toggle task on backend, toggling locally:", err);
                const row = document.getElementById(`task-row-${taskId}`);
                if (row) {
                    const isNowDone = !row.style.opacity || row.style.opacity === '1';
                    row.style.opacity = isNowDone ? '0.6' : '1';
                    btn.innerHTML = isNowDone ? '<i class="fas fa-undo"></i> Reopen' : '<i class="fas fa-check"></i> Complete';
                    btn.style.background = isNowDone ? '#6b7280' : 'var(--primary-green, #16a34a)';
                }
            } finally {
                btn.disabled = false;
            }
        });
    });
}

window.initTasksPage = initTasks;
