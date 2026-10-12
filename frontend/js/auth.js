import { api } from './api.js';
import { supabase } from './supabaseClient.js';

export function generateFarmerId() {
  const year = new Date().getFullYear();
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let suffix = "";
  for (let i = 0; i < 4; i++) {
    suffix += chars[Math.floor(Math.random() * chars.length)];
  }
  return `AGV-${year}-${suffix}`;
}

export function getInitials(nameOrEmail) {
  if (!nameOrEmail) return "FA";
  const cleaned = String(nameOrEmail).trim();
  if (cleaned.includes("@")) {
    return cleaned.charAt(0).toUpperCase();
  }
  const parts = cleaned.split(/\s+/).filter(Boolean);
  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase();
  }
  return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
}

export const auth = {
  user: null,
  farmerProfile: null,

  /**
   * Restores existing session from Supabase client or local token storage.
   * Auto-refreshes expired sessions and synchronizes farmer profile from Spring Boot.
   */
  async getSession() {
    let token = localStorage.getItem('agroverse_token');

    // 1. Check active Supabase session
    try {
      if (supabase && supabase.auth) {
        const { data: { session }, error } = await supabase.auth.getSession();
        if (session && session.access_token) {
          token = session.access_token;
          localStorage.setItem('agroverse_token', token);

          // If session is near expiry, attempt refresh
          if (session.expires_at && (session.expires_at * 1000 < Date.now() + 60000)) {
            const { data: refData } = await supabase.auth.refreshSession();
            if (refData?.session?.access_token) {
              token = refData.session.access_token;
              localStorage.setItem('agroverse_token', token);
            }
          }
        }
      }
    } catch (e) {
      console.warn("[Auth] Supabase getSession check skipped:", e.message);
    }

    if (!token || token.trim() === '') {
      this.user = null;
      this.farmerProfile = null;
      return null;
    }

    try {
      // Validate token with backend profile endpoint
      const me = await api.getProfile();
      if (me && me.id) {
        this.user = me;
        this.farmerProfile = me;
        localStorage.setItem('agroverse_user', JSON.stringify(me));
        localStorage.setItem('agroverse_auth', 'true');
        return me;
      }
    } catch (err) {
      if (err && (err.status === 401 || err.status === 403)) {
        console.warn("[Auth] Token expired or invalid, clearing session");
        localStorage.removeItem('agroverse_token');
        localStorage.removeItem('agroverse_user');
        localStorage.removeItem('agroverse_auth');
        this.user = null;
        this.farmerProfile = null;
        return null;
      }

      // If server is offline, read cached user only with existing token
      const storedUser = localStorage.getItem('agroverse_user');
      if (storedUser) {
        try {
          this.user = JSON.parse(storedUser);
          this.farmerProfile = this.user;
          return this.user;
        } catch {
          this.user = null;
          this.farmerProfile = null;
        }
      }
    }
    return this.user;
  },

  /**
   * Log in using Supabase Auth with backend profile resolution.
   */
  async loginFarmer(email, password) {
    if (!email || !password) {
      throw new Error("Email and password are required.");
    }

    let token = null;
    let authUser = null;

    // 1. Supabase Auth Login
    try {
      if (supabase && supabase.auth) {
        const { data, error } = await supabase.auth.signInWithPassword({
          email: email.trim().toLowerCase(),
          password: password
        });

        if (error) {
          if (error.message.includes("Email not confirmed")) {
            throw new Error("Please verify your email address before logging in.");
          }
          if (error.message.includes("Invalid login credentials")) {
            throw new Error("Invalid email or password.");
          }
          throw error;
        }

        if (data && data.session) {
          token = data.session.access_token;
          authUser = data.user;
        }
      }
    } catch (supaErr) {
      // If error was email verification or bad credentials, rethrow
      if (supaErr.message.includes("verify your email") || supaErr.message.includes("Invalid email")) {
        throw supaErr;
      }
      console.warn("[Auth] Supabase login error, checking backend fallback:", supaErr.message);
    }

    // Fallback: If Supabase was unreachable/offline, try local Spring Boot login
    if (!token) {
      const resp = await api.login({ email, password });
      if (!resp || !resp.token) {
        throw new Error("Invalid email or password.");
      }
      token = resp.token;
      authUser = resp.user;
    }

    localStorage.setItem('agroverse_token', token);

    // Synchronize farmer profile from backend
    let profile = null;
    try {
      profile = await api.getProfile();
    } catch (e) {
      console.warn("[Auth] Could not load profile immediately after login:", e);
      profile = authUser || {
        id: 1,
        email: email,
        fullName: email.split('@')[0],
        farmerCode: generateFarmerId()
      };
    }

    localStorage.setItem('agroverse_user', JSON.stringify(profile));
    localStorage.setItem('agroverse_auth', 'true');

    this.user = profile;
    this.farmerProfile = profile;

    if (window.refreshUserIdentityUI) {
      await window.refreshUserIdentityUI();
    }

    return profile;
  },

  /**
   * Registers a new farmer account using Supabase Auth.
   * Handles email confirmation required states safely.
   */
  async registerFarmer(userData) {
    const fullName = `${userData.firstName || ''} ${userData.lastName || ''}`.trim() || userData.fullName || "Farmer";
    const email = userData.email.trim().toLowerCase();
    const password = userData.password;

    const metadata = {
      full_name: fullName,
      phone: userData.phone || "",
      location: userData.address || userData.location || "",
      farm_type: userData.farmType || "Mixed",
      land_holding: userData.landHolding || "2.0",
      preferred_language: userData.preferredLanguage || localStorage.getItem('agroverse_language') || "en"
    };

    let supaSuccess = false;
    let supaData = null;

    // 1. Submit signup to Supabase Auth
    try {
      if (supabase && supabase.auth) {
        const { data, error } = await supabase.auth.signUp({
          email: email,
          password: password,
          options: {
            data: metadata
          }
        });

        if (error) {
          if (error.message.includes("already registered") || error.message.includes("User already exists")) {
            throw new Error("An account with this email address already exists. Please log in.");
          }
          throw error;
        }

        supaSuccess = true;
        supaData = data;
      }
    } catch (supaErr) {
      if (supaErr.message.includes("already exists") || supaErr.message.includes("Password")) {
        throw supaErr;
      }
      console.warn("[Auth] Supabase signup offline/failed, trying backend registration:", supaErr.message);
    }

    // If Supabase was available:
    if (supaSuccess && supaData) {
      const { user, session } = supaData;

      // Check if email confirmation is required
      if (!session) {
        console.log("[Auth] Email confirmation required for:", email);
        return {
          confirmationRequired: true,
          email: email,
          message: "Registration successful! A confirmation email has been sent. Please verify your email before logging in."
        };
      }

      // If session exists immediately (email confirmation disabled/auto-confirmed)
      const token = session.access_token;
      localStorage.setItem('agroverse_token', token);

      let profile = null;
      try {
        profile = await api.getProfile();
      } catch (err) {
        console.warn("[Auth] Profile initialization deferred to first login:", err);
        profile = {
          email: email,
          fullName: fullName,
          farmerCode: generateFarmerId()
        };
      }

      localStorage.setItem('agroverse_user', JSON.stringify(profile));
      localStorage.setItem('agroverse_auth', 'true');
      localStorage.setItem('agroverse_new_user', 'true');

      this.user = profile;
      this.farmerProfile = profile;

      if (window.refreshUserIdentityUI) {
        await window.refreshUserIdentityUI();
      }

      return {
        confirmationRequired: false,
        user: profile
      };
    }

    // Fallback: Direct backend registration if Supabase client is offline
    const payload = {
      fullName,
      email,
      password,
      ...metadata
    };

    const resp = await api.register(payload);
    if (!resp || !resp.token) {
      throw new Error("Registration failed: no token returned");
    }

    const token = resp.token;
    const user = resp.user || {
      id: 1,
      email: email,
      fullName: fullName,
      farmerCode: generateFarmerId()
    };

    localStorage.setItem('agroverse_token', token);
    localStorage.setItem('agroverse_user', JSON.stringify(user));
    localStorage.setItem('agroverse_auth', 'true');
    localStorage.setItem('agroverse_new_user', 'true');

    this.user = user;
    this.farmerProfile = user;

    if (window.refreshUserIdentityUI) {
      await window.refreshUserIdentityUI();
    }

    return {
      confirmationRequired: false,
      user: user
    };
  },

  async logoutFarmer() {
    try {
      if (supabase && supabase.auth) {
        await supabase.auth.signOut();
      }
    } catch (e) {
      console.warn("[Auth] Supabase signOut:", e.message);
    }

    localStorage.removeItem('agroverse_token');
    localStorage.removeItem('agroverse_user');
    localStorage.removeItem('agroverse_auth');
    localStorage.removeItem('agroverse_new_user');
    sessionStorage.clear();

    this.user = null;
    this.farmerProfile = null;

    if (window.resetUserIdentityUI) {
      window.resetUserIdentityUI();
    }

    window.location.hash = 'login';
  },

  async resetPassword(email) {
    if (!email) throw new Error("Email is required.");
    if (supabase && supabase.auth) {
      const { error } = await supabase.auth.resetPasswordForEmail(email.trim().toLowerCase());
      if (error) throw error;
      return true;
    }
    throw new Error("Password reset service is currently unavailable.");
  },

  async loadFarmerProfile() {
    try {
      const profile = await api.getProfile();
      if (profile && profile.id) {
        this.farmerProfile = profile;
        this.user = profile;
        localStorage.setItem('agroverse_user', JSON.stringify(profile));
        return profile;
      }
    } catch (err) {
      console.warn("[Auth] Could not load profile from server:", err);
    }
    const cached = localStorage.getItem('agroverse_user');
    if (cached) {
      try {
        const u = JSON.parse(cached);
        this.farmerProfile = u;
        this.user = u;
        return u;
      } catch {
        // ignore
      }
    }
    return this.user;
  },

  isLoggedIn() {
    return !!localStorage.getItem('agroverse_token') && !!localStorage.getItem('agroverse_user');
  }
};

window.auth = auth;
