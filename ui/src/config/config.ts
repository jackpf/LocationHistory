interface AppConfig {
    PROXY_URL: string;
    MAPTILER_API_KEY: string;
}

declare global {
    interface Window {
        APP_CONFIG: AppConfig;
    }
}

export const PROXY_URL = window.APP_CONFIG?.PROXY_URL || import.meta.env.VITE_PROXY_URL;
export const MAPTILER_API_KEY = window.APP_CONFIG?.MAPTILER_API_KEY || import.meta.env.VITE_MAPTILER_API_KEY;