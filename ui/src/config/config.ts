interface AppConfig {
    PROXY_URL: string;
    ADMIN_PASSWORD?: string;
}

declare global {
    interface Window {
        APP_CONFIG: AppConfig;
    }
}

export const PROXY_URL = window.APP_CONFIG?.PROXY_URL || import.meta.env.VITE_PROXY_URL;
export const ADMIN_PASSWORD = window.APP_CONFIG?.ADMIN_PASSWORD || import.meta.env.VITE_ADMIN_PASSWORD;