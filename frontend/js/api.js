/**
 * AgroVerse Canonical HTTP Client & API Registry
 * Provides environment-based base URL, JWT bearer attachment, structured error parsing,
 * and complete typed API contracts.
 */

function getBaseUrl() {
  const baseUrl = window.AGROVERSE_CONFIG?.API_BASE_URL || window.ENV?.VITE_API_BASE_URL || "http://localhost:8080";
  return baseUrl.replace(/\/$/, "");
}

export function getStoredToken() {
  const token = localStorage.getItem('agroverse_token');
  return token && token.trim() !== '' ? token.trim() : null;
}

export async function apiFetch(endpoint, options = {}) {
  const API_BASE_URL = getBaseUrl();
  const url = `${API_BASE_URL}${endpoint}`;

  const headers = { ...(options.headers || {}) };
  const token = getStoredToken();
  if (token && !headers['Authorization']) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  // If body is FormData, let browser set Content-Type with boundary
  if (options.body instanceof FormData) {
    delete headers['Content-Type'];
  } else if (!headers['Content-Type'] && options.method && options.method !== 'GET') {
    headers['Content-Type'] = 'application/json';
  }

  const fetchOptions = {
    ...options,
    headers
  };

  let response;
  try {
    response = await fetch(url, fetchOptions);
  } catch (error) {
    console.warn(`[API] Network error communicating with ${endpoint}`);
    throw new Error("BACKEND_OFFLINE");
  }

  const contentType = response.headers.get("content-type") || "";
  let data;
  if (contentType.includes("application/json")) {
    try {
      data = await response.json();
    } catch {
      data = null;
    }
  } else {
    data = await response.text();
  }

  if (!response.ok) {
    if (response.status === 401) {
      // If unauthorized, clear stale credentials if any
      console.warn(`[API] 401 Unauthorized for ${endpoint}`);
    }
    const message = (typeof data === 'object' && data && (data.error || data.message))
      ? (data.error || data.message)
      : `Request failed with status ${response.status}`;
    const err = new Error(message);
    err.status = response.status;
    err.data = data;
    throw err;
  }

  return data;
}

export function resolveImageUrl(url) {
  if (!url) return null;
  if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("data:") || url.startsWith("blob:")) return url;
  const API_BASE_URL = getBaseUrl();
  const path = url.startsWith('/') ? url : `/${url}`;
  return `${API_BASE_URL}${path}`;
}

window.apiFetch = apiFetch;
window.resolveImageUrl = resolveImageUrl;

export const api = {
  resolveImageUrl,
  getBaseUrl,

  // Health & System
  getHealth: () => apiFetch('/api/health'),

  // Authentication & Profile
  login: (credentials) => apiFetch('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials)
  }),
  register: (payload) => apiFetch('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  getProfile: () => apiFetch('/api/profile/me'),
  updateProfile: (profileData) => apiFetch('/api/profile/me', {
    method: 'PUT',
    body: JSON.stringify(profileData)
  }),

  // Home Dashboard
  getHomeSummary: () => apiFetch('/api/home/summary'),
  getCrops: () => apiFetch('/api/crops'),
  addCrop: (crop) => apiFetch('/api/crops', {
    method: 'POST',
    body: JSON.stringify(crop)
  }),
  getTodayTasks: () => apiFetch('/api/tasks/today'),
  toggleTask: (taskId) => apiFetch(`/api/tasks/${taskId}/toggle`, {
    method: 'POST'
  }),

  // Crop Detection & ONNX Inference
  analyzeCrop: (formData) => apiFetch('/api/crop-detection/analyze', {
    method: 'POST',
    body: formData
  }),
  getCropHistory: () => apiFetch('/api/crop-detection/history'),

  // Schemes
  getSchemes: () => apiFetch('/api/schemes'),
  getSchemeDetails: (id) => apiFetch(`/api/schemes/${id}`),
  applyScheme: (schemeId) => apiFetch(`/api/schemes/${schemeId}/apply-click`, { method: 'POST' }),
  getMySchemeApplications: () => apiFetch('/api/schemes/applications/me'),

  // Community
  getPosts: () => apiFetch('/api/community/posts'),
  getCommunitySnapshot: () => apiFetch('/api/community/snapshot'),
  createPost: (post) => apiFetch('/api/community/posts', {
    method: 'POST',
    body: JSON.stringify(post)
  }),
  likePost: (postId) => apiFetch(`/api/community/posts/${postId}/like`, {
    method: 'POST'
  }),
  deletePost: (postId) => apiFetch(`/api/community/posts/${postId}`, {
    method: 'DELETE'
  }),

  // Marketplace
  getProducts: (category) => apiFetch(`/api/marketplace/products${category ? '?category=' + encodeURIComponent(category) : ''}`),
  getMarketplaceSummary: () => apiFetch('/api/marketplace/summary'),
  createListing: (listing) => apiFetch('/api/marketplace/listings', {
    method: 'POST',
    body: JSON.stringify(listing)
  }),
  deleteListing: (id) => apiFetch(`/api/marketplace/listings/${id}`, {
    method: 'DELETE'
  }),

  // Notifications
  getNotifications: () => apiFetch('/api/notifications'),
  markNotificationRead: (id) => apiFetch(`/api/notifications/${id}/read`, { method: 'POST' }),

  // Market Rates & Agricultural Data
  getMarketRates: () => apiFetch('/api/market/rates'),
  getMarketData: () => apiFetch('/api/data/market'),

  // Weather & Chatbot
  getWeather: (city = 'Nashik') => apiFetch(`/api/weather?city=${encodeURIComponent(city)}`),
  getWeatherByCoords: (lat, lon) => apiFetch(`/api/weather/coords?lat=${lat}&lon=${lon}`),
  chatbot: (message) => {
    const lang = localStorage.getItem('agroverse_language') || 'en';
    return apiFetch('/api/chat', {
      method: 'POST',
      body: JSON.stringify({ message, language: lang })
    });
  }
};
