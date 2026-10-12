/**
 * AgroVerse i18n Engine
 */

async function loadLocale(lang) {
    try {
        const response = await fetch(`./locales/${lang}.json`);
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        return await response.json();
    } catch (error) {
        console.error(`Could not load locale ${lang}`, error);
        return null;
    }
}

async function initI18n() {
    const savedLang = localStorage.getItem('agroverse_language') || 'en';
    
    // Load resources
    const resources = {};
    const langs = ['en', 'hi', 'mr'];
    
    for (const l of langs) {
        const data = await loadLocale(l);
        if (data) {
            resources[l] = { translation: data };
        }
    }

    if (typeof i18next !== 'undefined') {
        await i18next.init({
            lng: savedLang,
            fallbackLng: 'en',
            resources: resources,
            interpolation: {
                escapeValue: false
            }
        });

        window.applyTranslations();
        document.documentElement.lang = savedLang;
        console.log("i18n Initialized");
    } else {
        console.error("i18next not found!");
    }
}

function applyTranslations() {
    if (typeof i18next === 'undefined' || !i18next.isInitialized) return;

    // Translate standard text
    document.querySelectorAll('[data-i18n]').forEach(el => {
        const key = el.getAttribute('data-i18n');
        const translation = i18next.t(key);
        if (translation && translation !== key) {
            el.textContent = translation;
        }
    });

    // Translate placeholders
    document.querySelectorAll('[data-i18n-placeholder]').forEach(el => {
        const key = el.getAttribute('data-i18n-placeholder');
        const translation = i18next.t(key);
        if (translation && translation !== key) {
            el.placeholder = translation;
        }
    });

    // Translate titles
    document.querySelectorAll('[data-i18n-title]').forEach(el => {
        const key = el.getAttribute('data-i18n-title');
        const translation = i18next.t(key);
        if (translation && translation !== key) {
            el.title = translation;
        }
    });
}

// Expose to window
window.applyTranslations = applyTranslations;
window.initI18n = initI18n;

// Run initialization
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initI18n);
} else {
    initI18n();
}
