/**
 * Redundant Root Navigation Script
 * Note: The actual SPA routing is handled by js/router.js
 * This file is kept for compatibility but updated to match the new 'home' route.
 */
document.addEventListener('DOMContentLoaded', () => {
    const navItems = document.querySelectorAll('.nav-item');
    const currentHash = window.location.hash.replace('#', '') || 'home';

    navItems.forEach(item => {
        const route = item.getAttribute('data-route');

        // Handle Active State
        if (currentHash === route) {
            item.classList.add('active');
        } else {
            item.classList.remove('active');
        }

        // Handle Click Navigation (This will trigger hashchange which router.js listens to)
        item.addEventListener('click', (e) => {
            if (route) {
                window.location.hash = route;
            }
        });
    });
});
