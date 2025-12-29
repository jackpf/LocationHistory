interface AppConfig {
    PROXY_URL: string;
}

declare global {
    interface Window {
        APP_CONFIG: AppConfig;
    }
}

export const PROXY_URL = window.APP_CONFIG?.PROXY_URL || import.meta.env.VITE_PROXY_URL;