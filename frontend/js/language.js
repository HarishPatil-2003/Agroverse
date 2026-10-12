/**
 * AgroVerse Language Selector Logic
 */

document.addEventListener('change', (e) => {
    if (e.target && e.target.id === 'languageSelect') {
        const selectedLang = e.target.value;
        console.log(`Language changing to: ${selectedLang}`);
        
        // 1. Save to localStorage
        localStorage.setItem('agroverse_language', selectedLang);
        
        // 2. Change i18next language
        if (typeof i18next !== 'undefined') {
            i18next.changeLanguage(selectedLang, (err) => {
                if (err) return console.error('Error changing language', err);
                
                // 3. Update UI
                document.documentElement.lang = selectedLang;
                if (window.applyTranslations) {
                    window.applyTranslations();
                }
                
                console.log(`Language successfully switched to ${selectedLang}`);
            });
        }
    }
});

// Auto-sync selector with saved language
function syncLanguageSelector() {
    const selector = document.getElementById('languageSelect');
    if (selector) {
        const savedLang = localStorage.getItem('agroverse_language') || 'en';
        selector.value = savedLang;
        console.log(`Synced language selector to: ${savedLang}`);
    }
}

// Export to window so router can call it after rendering
window.syncLanguageSelector = syncLanguageSelector;

// Re-sync whenever the view changes
window.addEventListener('hashchange', () => {
    // Wait for the new content to be injected by the router
    setTimeout(syncLanguageSelector, 150);
});

// Initial sync
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', syncLanguageSelector);
} else {
    syncLanguageSelector();
}
