interface AppConfig {
    PROXY_URL: string;
    MAP_TYPE: string;
    MAPTILER_API_KEY: string;
}

declare global {
    interface Window {
        APP_CONFIG: AppConfig;
    }
}

console.log("App config:", window.APP_CONFIG);

export const PROXY_URL = window.APP_CONFIG?.PROXY_URL || import.meta.env.VITE_PROXY_URL;
export const MAP_TYPE = window.APP_CONFIG?.MAP_TYPE || import.meta.env.VITE_MAP_TYPE;
export const MAPTILER_API_KEY = window.APP_CONFIG?.MAPTILER_API_KEY || import.meta.env.VITE_MAPTILER_API_KEY;