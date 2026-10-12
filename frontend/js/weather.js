import { api } from './api.js';

/**
 * AgroVerse Weather Widget
 * - Auto-detects user location via GPS (with fallback to Nashik)
 * - Injects a live weather card into the dashboard sidebar
 * - Supports city search
 * - Refreshes every 10 minutes
 */

const WEATHER_REFRESH_MS = 10 * 60 * 1000; // 10 minutes

function getWeatherIcon(condition) {
    const c = (condition || '').toLowerCase();
    if (c.includes('clear') || c.includes('sunny')) return 'fa-sun';
    if (c.includes('cloud')) return 'fa-cloud';
    if (c.includes('rain') || c.includes('drizzle')) return 'fa-cloud-rain';
    if (c.includes('thunder') || c.includes('storm')) return 'fa-bolt';
    if (c.includes('snow')) return 'fa-snowflake';
    if (c.includes('mist') || c.includes('fog') || c.includes('haze')) return 'fa-smog';
    if (c.includes('wind')) return 'fa-wind';
    return 'fa-cloud-sun';
}

function getWeatherColor(condition) {
    const c = (condition || '').toLowerCase();
    if (c.includes('clear') || c.includes('sunny')) return '#f59e0b';
    if (c.includes('rain') || c.includes('drizzle')) return '#3b82f6';
    if (c.includes('thunder') || c.includes('storm')) return '#7c3aed';
    if (c.includes('snow')) return '#93c5fd';
    if (c.includes('cloud')) return '#64748b';
    return '#10b981';
}

function formatTime(unixTs) {
    if (!unixTs) return '--';
    const d = new Date(unixTs * 1000);
    return d.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: true });
}

function renderWeatherCard(data) {
    const condition = data.condition || 'Clear';
    const icon = data.icon
        ? `<img src="${data.icon}" alt="${condition}" style="width:64px;height:64px;">`
        : `<i class="fas ${getWeatherIcon(condition)}" style="font-size:52px;color:${getWeatherColor(condition)};"></i>`;
    const isError = data.error;
    const isPending = data.status === 'pending_activation';

    return `
        <div class="weather-widget-inner">
            <div class="weather-search-bar">
                <input type="text" id="weather-city-input" data-i18n-placeholder="weather.searchCity" placeholder="Search city..." value="${data.city || ''}">
                <button id="weather-search-btn" title="Search"><i class="fas fa-search"></i></button>
                <button id="weather-gps-btn" title="Use Live Location"><i class="fas fa-crosshairs"></i></button>
            </div>

            ${isPending ? `
                <div class="weather-pending">
                    <i class="fas fa-clock" style="font-size:36px;color:#f59e0b;display:block;margin-bottom:12px;"></i>
                    <strong>API Key Activating</strong>
                    <p>Your OpenWeatherMap key is being activated. This usually takes <strong>up to 2 hours</strong> for new accounts.</p>
                    <p style="margin-top:8px;font-size:11px;color:#94a3b8;">Auto-retrying every 2 minutes...</p>
                    <div id="weather-retry-countdown" style="font-size:12px;color:#10b981;margin-top:6px;"></div>
                </div>
            ` : isError ? `
                <div class="weather-error">
                    <i class="fas fa-exclamation-triangle"></i>
                    <p>Weather data unavailable for this location. Please check the city name or try again later.</p>
                </div>
            ` : `
                <div class="weather-main">
                    <div class="weather-location">
                        <i class="fas fa-map-marker-alt"></i>
                        <span id="weather-city-name">${data.city || 'Nashik'}</span>
                    </div>
                    <div class="weather-icon-wrap">${icon}</div>
                    <div class="weather-temp" id="weather-temp">${data.temp}°C</div>
                    <div class="weather-desc" id="weather-desc">${data.description || condition}</div>
                    <div class="weather-feels"><span data-i18n="weather.feelsLike">Feels like</span> ${data.feels_like}°C</div>
                </div>

                <div class="weather-stats-row">
                    <div class="w-stat">
                        <i class="fas fa-tint" style="color:#3b82f6;"></i>
                        <span>${data.humidity}%</span>
                        <small data-i18n="weather.humidity">Humidity</small>
                    </div>
                    <div class="w-stat">
                        <i class="fas fa-wind" style="color:#10b981;"></i>
                        <span>${data.wind_speed} km/h</span>
                        <small data-i18n="weather.wind">Wind</small>
                    </div>
                    <div class="w-stat">
                        <i class="fas fa-eye" style="color:#f59e0b;"></i>
                        <span>${data.visibility != null ? data.visibility + ' km' : '--'}</span>
                        <small data-i18n="weather.visibility">Visibility</small>
                    </div>
                </div>

                <div class="weather-sun-row">
                    <div class="sun-item">
                        <i class="fas fa-sun" style="color:#f59e0b;"></i>
                        <span data-i18n="weather.sunrise">Sunrise</span>
                        <strong>${formatTime(data.sunrise)}</strong>
                    </div>
                    <div class="sun-divider"></div>
                    <div class="sun-item">
                        <i class="fas fa-moon" style="color:#8b5cf6;"></i>
                        <span data-i18n="weather.sunset">Sunset</span>
                        <strong>${formatTime(data.sunset)}</strong>
                    </div>
                </div>

                <div class="weather-range">
                    <i class="fas fa-thermometer-half" style="color:#ef4444;"></i>
                    <span><span data-i18n="weather.today">Today</span>: ${data.temp_min}°C – ${data.temp_max}°C</span>
                </div>
            `}

            <div class="weather-refresh-row">
                <span id="weather-last-updated" data-i18n="weather.updatedJustNow">Updated just now</span>
                <button id="weather-refresh-btn" title="Refresh"><i class="fas fa-sync-alt"></i></button>
            </div>
        </div>
    `;
}

function injectWeatherStyles() {
    if (document.getElementById('weather-widget-styles')) return;
    const style = document.createElement('style');
    style.id = 'weather-widget-styles';
    style.textContent = `
        #live-weather-card {
            background: var(--bg-card, #fff);
            border-radius: 20px;
            border: 1px solid var(--border-color, #e2e8f0);
            padding: 20px;
            margin-bottom: 20px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.06);
        }
        #live-weather-card h3 { margin: 0 0 14px; font-size: 15px; color: var(--text-muted, #64748b); font-weight: 600; }
        .weather-search-bar { display: flex; gap: 8px; margin-bottom: 16px; }
        .weather-search-bar input {
            flex: 1; padding: 8px 14px; border-radius: 20px;
            border: 1px solid var(--border-color, #e2e8f0);
            background: var(--input-bg, #f8fafc); color: var(--text-main, #1e293b);
            font-size: 13px; outline: none;
        }
        .weather-search-bar button {
            background: #10b981; color: white; border: none;
            width: 34px; height: 34px; border-radius: 50%; cursor: pointer;
            display: flex; align-items: center; justify-content: center; font-size: 13px;
            transition: transform 0.2s, opacity 0.2s;
            flex-shrink: 0;
        }
        .weather-search-bar button:hover { transform: scale(1.1); }
        #weather-gps-btn { background: #3b82f6; }
        .weather-main { text-align: center; padding: 8px 0; }
        .weather-location { font-size: 13px; color: var(--text-muted, #64748b); margin-bottom: 8px; }
        .weather-location i { margin-right: 5px; color: #ef4444; }
        .weather-icon-wrap { margin: 8px 0; }
        .weather-temp { font-size: 52px; font-weight: 700; color: var(--text-main, #1e293b); line-height: 1; }
        .weather-desc { font-size: 15px; color: var(--text-muted, #64748b); text-transform: capitalize; margin-top: 6px; }
        .weather-feels { font-size: 12px; color: var(--text-muted, #94a3b8); margin-top: 4px; }
        .weather-stats-row { display: flex; justify-content: space-around; margin: 16px 0; padding: 12px 0; border-top: 1px solid var(--border-color, #e2e8f0); border-bottom: 1px solid var(--border-color, #e2e8f0); }
        .w-stat { display: flex; flex-direction: column; align-items: center; gap: 4px; }
        .w-stat i { font-size: 16px; }
        .w-stat span { font-size: 14px; font-weight: 600; color: var(--text-main, #1e293b); }
        .w-stat small { font-size: 11px; color: var(--text-muted, #94a3b8); }
        .weather-sun-row { display: flex; align-items: center; justify-content: center; gap: 16px; margin: 12px 0; }
        .sun-item { display: flex; flex-direction: column; align-items: center; gap: 3px; }
        .sun-item i { font-size: 18px; }
        .sun-item span { font-size: 11px; color: var(--text-muted, #94a3b8); }
        .sun-item strong { font-size: 13px; color: var(--text-main, #1e293b); }
        .sun-divider { width: 1px; height: 40px; background: var(--border-color, #e2e8f0); }
        .weather-range { text-align: center; font-size: 12px; color: var(--text-muted, #64748b); margin-top: 8px; }
        .weather-range i { margin-right: 5px; }
        .weather-refresh-row { display: flex; justify-content: space-between; align-items: center; margin-top: 14px; padding-top: 12px; border-top: 1px solid var(--border-color, #e2e8f0); }
        #weather-last-updated { font-size: 11px; color: var(--text-muted, #94a3b8); }
        #weather-refresh-btn { background: transparent; border: none; color: var(--text-muted, #94a3b8); cursor: pointer; font-size: 13px; padding: 4px; transition: transform 0.4s; }
        #weather-refresh-btn:hover { transform: rotate(180deg); color: #10b981; }
        #weather-refresh-btn.spinning { animation: spin 0.6s linear; }
        @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
        .weather-error { text-align: center; padding: 20px; color: #ef4444; }
        .weather-error i { font-size: 30px; margin-bottom: 10px; display: block; }
        .weather-error p { font-size: 13px; color: var(--text-muted, #64748b); }
        .weather-loading { text-align: center; padding: 30px; color: var(--text-muted, #94a3b8); }
        .weather-loading i { font-size: 24px; margin-bottom: 8px; display: block; color: #10b981; }
    `;
    document.head.appendChild(style);
}

let weatherRefreshTimer = null;
let currentWeatherState = {
    city: 'Nashik',
    lat: null,
    lon: null
};

async function loadWeather(city = null, lat = null, lon = null) {
    const card = document.getElementById('live-weather-card');
    if (!card) return;

    if (lat !== null && lon !== null) {
        currentWeatherState = { city: null, lat, lon };
    } else if (city) {
        currentWeatherState = { city, lat: null, lon: null };
    }

    // Show loading spinner
    card.querySelector('.weather-widget-inner') && (card.querySelector('.weather-widget-inner').innerHTML = `
        <div class="weather-loading">
            <i class="fas fa-circle-notch fa-spin"></i>
            <p>Fetching live weather...</p>
        </div>
    `);

    try {
        let data;
        if (lat !== null && lon !== null) {
            data = await api.getWeatherByCoords(lat, lon);
        } else {
            data = await api.getWeather(city || currentWeatherState.city || 'Nashik');
        }

        if (data && data.city) {
            currentWeatherState.city = data.city;
        }

        card.innerHTML = `<h3><i class="fas fa-cloud-sun" style="color:#f59e0b;margin-right:8px;"></i><span data-i18n="weather.liveWeather">Live Weather</span></h3>` + renderWeatherCard(data);
        bindWeatherEvents(card, data);
        if (window.applyTranslations) window.applyTranslations();

    } catch (err) {
        card.innerHTML = `<h3><i class="fas fa-cloud-sun" style="color:#f59e0b;margin-right:8px;"></i><span data-i18n="weather.liveWeather">Live Weather</span></h3>` + renderWeatherCard({ error: true, city: city || currentWeatherState.city || 'Nashik' });
        bindWeatherEvents(card, { city: city || currentWeatherState.city || 'Nashik' });
        if (window.applyTranslations) window.applyTranslations();
    }
}

function bindWeatherEvents(card, currentData) {
    const searchInput = card.querySelector('#weather-city-input');
    const searchBtn = card.querySelector('#weather-search-btn');
    const gpsBtn = card.querySelector('#weather-gps-btn');
    const refreshBtn = card.querySelector('#weather-refresh-btn');

    if (searchBtn && searchInput) {
        const doSearch = () => {
            const val = searchInput.value.trim();
            if (val) loadWeather(val);
        };
        searchBtn.onclick = doSearch;
        searchInput.onkeypress = (e) => { if (e.key === 'Enter') doSearch(); };
    }

    if (gpsBtn) {
        gpsBtn.onclick = () => {
            if (!navigator.geolocation) {
                alert('Geolocation not supported by your browser.');
                return;
            }
            gpsBtn.innerHTML = '<i class="fas fa-circle-notch fa-spin"></i>';
            
            const handleGpsFailure = (errMsg) => {
                gpsBtn.innerHTML = '<i class="fas fa-crosshairs"></i>'; 
                console.warn('GPS failed/timeout:', errMsg);
                let userCity = prompt('Browser GPS is unavailable (Check Windows Location settings). Please enter your village or city manually:', 'Amalner');
                if (userCity && userCity.trim() !== '') {
                    const inputField = document.getElementById('weather-city-input');
                    if (inputField) inputField.value = userCity;
                    loadWeather(userCity.trim());
                } else {
                    loadWeather('Nashik');
                }
            };

            navigator.geolocation.getCurrentPosition(
                (pos) => loadWeather(null, pos.coords.latitude, pos.coords.longitude),
                (err) => handleGpsFailure(err.message),
                { timeout: 3000, enableHighAccuracy: true }
            );
        };
    }

    if (refreshBtn) {
        refreshBtn.onclick = () => {
            refreshBtn.classList.add('spinning');
            setTimeout(() => refreshBtn.classList.remove('spinning'), 600);
            if (currentWeatherState.lat !== null && currentWeatherState.lon !== null) {
                loadWeather(null, currentWeatherState.lat, currentWeatherState.lon);
            } else {
                loadWeather(currentWeatherState.city || currentData?.city || 'Nashik');
            }
        };
    }
}

export function initWeatherWidget(containerSelector = '.sidebar-panel') {
    injectWeatherStyles();

    // Find or create weather card slot
    let card = document.getElementById('live-weather-card');
    if (!card) {
        card = document.createElement('div');
        card.id = 'live-weather-card';
        card.innerHTML = `<h3><i class="fas fa-cloud-sun" style="color:#f59e0b;margin-right:8px;"></i><span data-i18n="weather.liveWeather">Live Weather</span></h3>
            <div class="weather-widget-inner"><div class="weather-loading"><i class="fas fa-circle-notch fa-spin"></i><p data-i18n="common.loading">Loading...</p></div></div>`;

        const container = document.querySelector(containerSelector)
            || document.querySelector('.right-sidebar')
            || document.querySelector('.sidebar');

        if (container) {
            container.insertBefore(card, container.firstChild);
        } else {
            // Fallback: inject into app-content before existing content
            const appContent = document.getElementById('app-content');
            if (appContent) appContent.insertBefore(card, appContent.firstChild);
        }
    }

    // Try GPS first, then fallback to Nashik
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            (pos) => loadWeather(null, pos.coords.latitude, pos.coords.longitude),
            (err) => {
                console.warn('Geolocation failed on load, defaulting to Nashik:', err.message);
                loadWeather('Nashik');
            },
            { timeout: 3000 }
        );
    } else {
        loadWeather('Nashik');
    }

    // Auto-refresh
    if (weatherRefreshTimer) clearInterval(weatherRefreshTimer);
    weatherRefreshTimer = setInterval(() => {
        if (currentWeatherState.lat !== null && currentWeatherState.lon !== null) {
            loadWeather(null, currentWeatherState.lat, currentWeatherState.lon);
        } else {
            loadWeather(currentWeatherState.city || 'Nashik');
        }
    }, WEATHER_REFRESH_MS);
}

window.initWeatherWidget = initWeatherWidget;
