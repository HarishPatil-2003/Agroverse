/**
 * AgroVerse Frontend Dynamic Environment Config
 * Automatically detects local vs production deployment environments.
 */
(() => {
  const isLocal = typeof window !== 'undefined' && 
    (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1');

  const defaultProductionBackend = "https://agroverse-backend.onrender.com";

  window.AGROVERSE_CONFIG = {
    API_BASE_URL: (window.ENV && window.ENV.VITE_API_BASE_URL) 
      ? window.ENV.VITE_API_BASE_URL 
      : (isLocal ? "http://localhost:8080" : defaultProductionBackend)
  };
})();
