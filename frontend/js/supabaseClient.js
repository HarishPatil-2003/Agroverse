// Uses the locally installed Supabase UMD library from index.html
const getSupabaseLib = () => {
  if (window.supabase) return window.supabase;
  console.warn("window.supabase not found! Retrying with fallback...");
  return { createClient: () => ({ auth: { getSession: async () => ({ data: { session: null }, error: null }) } }) };
};

const supabaseUrl = window.ENV?.VITE_SUPABASE_URL || '';
const supabaseAnonKey = window.ENV?.VITE_SUPABASE_ANON_KEY || '';

if (!supabaseUrl || !supabaseAnonKey) {
  console.error("Missing Supabase environment variables! Please configure js/env.js");
}

const lib = getSupabaseLib();
export const supabase = lib.createClient(
  supabaseUrl || "https://placeholder.supabase.co",
  supabaseAnonKey || "placeholder"
);

