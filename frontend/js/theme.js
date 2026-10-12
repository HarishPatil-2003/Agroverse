function getSavedTheme() {
  return localStorage.getItem("agroverse_theme") || "light";
}

function applyTheme(theme) {
  document.body.classList.remove("light-theme", "dark-theme");
  document.body.classList.add(`${theme}-theme`);
  document.documentElement.setAttribute("data-theme", theme);
  localStorage.setItem("agroverse_theme", theme);
  updateThemeButtons(theme);
}

function updateThemeButtons(theme) {
  document.querySelectorAll("[data-theme-option]").forEach((btn) => {
    btn.classList.toggle("active", btn.dataset.themeOption === theme);
  });
}

function bindThemeButtons() {
  document.querySelectorAll("[data-theme-option]").forEach((btn) => {
    btn.onclick = () => {
      applyTheme(btn.dataset.themeOption);
    };
  });

  updateThemeButtons(getSavedTheme());
}

function initTheme() {
  applyTheme(getSavedTheme());
  bindThemeButtons();

  // Re-sync buttons when view changes (SPA navigation)
  window.addEventListener('hashchange', () => {
    setTimeout(() => {
      bindThemeButtons();
      updateThemeButtons(getSavedTheme());
    }, 100);
  });
}

// Immediate execution to prevent flash of un-themed content
(function() {
  const theme = localStorage.getItem("agroverse_theme") || "light";
  document.documentElement.setAttribute("data-theme", theme);
  document.addEventListener("DOMContentLoaded", initTheme);
})();
