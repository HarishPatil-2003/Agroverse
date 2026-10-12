/**
 * AgroVerse Navigation Helpers
 * Handles shell interactions like sidebar toggling and active states.
 */

export function initNavigation() {
    const sidebarToggle = document.querySelector('.welcome-msg i.fa-bars');
    const sidebar = document.querySelector('.sidebar');
    const userMeta = document.querySelector('.user-meta');

    // Sidebar Toggle for Mobile
    if (sidebarToggle && sidebar) {
        sidebarToggle.addEventListener('click', () => {
            sidebar.classList.toggle('collapsed');
            // Logic for a real mobile menu could go here
        });
    }

    // User Dropdown (Mock)
    if (userMeta) {
        userMeta.addEventListener('click', () => {
            console.log('User profile click');
        });
    }
}

// Automatically initialize shell navigation
document.addEventListener('DOMContentLoaded', initNavigation);
