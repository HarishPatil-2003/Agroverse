import { auth } from './auth.js';
import { initLogin } from './login.js';
import { initRegister } from './register.js';

// Expose to window for old-style init calls if needed
window.initLogin = initLogin;
window.initRegister = initRegister;

const AgroRouter = (() => {
  const routes = {
    login: {
      file: "./pages/login.html",
      title: "Login",
      init: "initLogin",
      isPublic: true
    },
    register: {
      file: "./pages/register.html",
      title: "Register",
      init: "initRegister",
      isPublic: true
    },
    home: {
      file: "./pages/home.html",
      title: "Dashboard",
      i18nTitle: "nav.home",
      subtitle: "Good morning! Let's make your farm more productive today.",
      i18nSubtitle: "home.subtitle",
      init: "initHomePage"
    },
    crop: {
      file: "./pages/crop.html",
      title: "Crop Detection",
      i18nTitle: "nav.cropDetection",
      subtitle: "Upload crop image to detect disease and get treatment suggestions.",
      i18nSubtitle: "crop.subtitle",
      init: "initCropDetectionPage"
    },
    schemes: {
      file: "./pages/schemes.html",
      title: "Govt Schemes",
      i18nTitle: "nav.govtSchemes",
      subtitle: "Find schemes by crop, state, and category.",
      i18nSubtitle: "schemes.subtitle",
      init: "initSchemesPage"
    },
    community: {
      file: "./pages/community.html",
      title: "Community",
      i18nTitle: "nav.community",
      subtitle: "Connect with farmers and share knowledge.",
      i18nSubtitle: "community.subtitle",
      init: "initCommunityPage"
    },
    marketplace: {
      file: "./pages/marketplace.html",
      title: "Marketplace",
      i18nTitle: "nav.marketplace",
      subtitle: "Buy and sell farming equipment.",
      i18nSubtitle: "marketplace.subtitle",
      init: "initMarketplacePage"
    },
    profile: {
      file: "./pages/profile.html",
      title: "Profile",
      i18nTitle: "nav.profile",
      subtitle: "Manage your profile, language, and theme.",
      i18nSubtitle: "profile.subtitle",
      init: "initProfilePage"
    }
  };

  function currentRoute() {
    const hash = window.location.hash.replace("#", "").trim();
    if (!hash) return auth.isLoggedIn() ? "home" : "login";
    return routes[hash] ? hash : (auth.isLoggedIn() ? "home" : "login");
  }

  function validatePartial(html, file) {
    const forbidden = [
      "<!doctype",
      "<html",
      "<head",
      "<body",
      "</body>",
      "</html>"
      // Removed <script and <link to allow some inline styles/scripts in partials if needed, 
      // but the main goal is to prevent full HTML documents.
    ];

    const lower = html.toLowerCase();
    const found = forbidden.find(token => lower.includes(token.toLowerCase()));

    if (found) {
      throw new Error(
        `${file} is not a valid page partial. Remove duplicated layout code: ${found}`
      );
    }
  }

  function updateLayout(routeKey) {
    const route = routes[routeKey];
    const isPublic = route.isPublic;
    
    const sidebar = document.querySelector('.app-sidebar');
    const header = document.querySelector('.top-header');
    const rightPanel = document.querySelector('.right-panel');
    const layoutWrapper = document.querySelector('.page-layout-wrapper');

    const layout = document.querySelector('.app-layout');
    if (layout) {
        if (isPublic) {
            layout.classList.add('auth-mode');
        } else {
            layout.classList.remove('auth-mode');
        }
    }

    if (isPublic) {
      if (sidebar) sidebar.style.display = 'none';
      if (header) header.style.display = 'none';
      if (rightPanel) rightPanel.style.display = 'none';
      if (layoutWrapper) {
          layoutWrapper.style.gridTemplateColumns = '';
          layoutWrapper.style.padding = '';
      }
    } else {
      if (sidebar) sidebar.style.display = '';
      if (header) header.style.display = '';
      if (rightPanel) rightPanel.style.display = '';
      if (layoutWrapper) {
          layoutWrapper.style.gridTemplateColumns = '';
          layoutWrapper.style.padding = '';
      }
    }
  }

  function updateHeader(routeKey) {
    const route = routes[routeKey];
    const title = document.getElementById("pageTitle");
    const subtitle = document.getElementById("pageSubtitle");
    
    if (subtitle && route.i18nSubtitle) {
        subtitle.setAttribute("data-i18n", route.i18nSubtitle);
        subtitle.textContent = route.subtitle; // Fallback
    }

    if (routeKey !== 'home' && title && route.i18nTitle) {
        title.setAttribute("data-i18n", route.i18nTitle);
        title.textContent = route.title; // Fallback
    }
  }

  function updateActiveNav(routeKey) {
    document.querySelectorAll(".nav-link").forEach(link => {
      link.classList.toggle("active", link.dataset.route === routeKey);
    });
  }

  async function load(routeKey) {
    console.log(`[ROUTER] Loading route: ${routeKey}`);
    const root = document.getElementById("page-root");

    if (!root) {
      console.error("[ROUTER] Critical Error: #page-root container is missing from the DOM. Redirecting to app shell.");
      window.location.href = "index.html#home";
      return;
    }

    // Auth Guard
    const route = routes[routeKey];
    if (!route.isPublic && !auth.isLoggedIn()) {
      console.warn(`[ROUTER] Access denied to ${routeKey}. Redirecting to login.`);
      window.location.hash = 'login';
      return;
    }

    // Already logged in? Don't show login/register
    if (route.isPublic && auth.isLoggedIn() && (routeKey === 'login' || routeKey === 'register')) {
      console.log(`[ROUTER] Already logged in. Redirecting to home.`);
      window.location.hash = 'home';
      return;
    }

    if (window.currentLoadingRoute === routeKey) return;
    window.currentLoadingRoute = routeKey;

    try {
      root.innerHTML = `
        <div class="page-loading">
          <div class="spinner"></div>
          <p data-i18n="common.loading">Loading ${route.title}...</p>
        </div>
      `;

      const response = await fetch(route.file, {
        cache: "no-store"
      });

      if (!response.ok) {
        throw new Error(`Cannot load ${route.file}. Status: ${response.status}`);
      }

      const html = await response.text();

      validatePartial(html, route.file);

      root.innerHTML = html;

      // Layout adjustments
      updateLayout(routeKey);

      // Restoration logic for global right panel if coming from marketplace or community or schemes
      const rightPanel = document.querySelector('.right-panel');
      if (rightPanel && window.originalRightPanelContent && routeKey !== 'marketplace' && routeKey !== 'community' && routeKey !== 'schemes' && !route.isPublic) {
          rightPanel.innerHTML = window.originalRightPanelContent;
          // Re-init weather if needed (optional but good practice)
          if (window.initWeatherWidget) window.initWeatherWidget();
      }

      updateHeader(routeKey);
      updateActiveNav(routeKey);

      if (route.init && typeof window[route.init] === "function") {
        window[route.init]();
      } else if (route.init && typeof route.init === "function") {
          route.init();
      }

      if (window.applyTranslations) {
          window.applyTranslations();
      }

      window.currentLoadingRoute = null;
      console.log(`[ROUTER] Route loaded: ${routeKey}`);
    } catch (error) {
      window.currentLoadingRoute = null;
      console.error(error);

      root.innerHTML = `
        <div class="content-card error-card">
          <h2>Page failed to load</h2>
          <p>${error.message}</p>
        </div>
      `;
    }
  }

  function navigate(routeKey) {
    if (!routes[routeKey]) routeKey = auth.isLoggedIn() ? "home" : "login";
    window.location.hash = routeKey;
  }

  function bindNavigation() {
    document.querySelectorAll(".nav-link").forEach(link => {
      link.addEventListener("click", event => {
        event.preventDefault();
        navigate(link.dataset.route);
      });
    });
  }

  function init() {
    bindNavigation();

    window.addEventListener("hashchange", () => {
      load(currentRoute());
    });

    load(currentRoute());
  }

  return {
    init,
    navigate,
    load
  };
})();

document.addEventListener("DOMContentLoaded", AgroRouter.init);
window.AgroRouter = AgroRouter;
