import {useCallback, useEffect, useState} from "react";
import {adminClient, AUTH_ERROR_EVENT, type AuthErrorEvent, grpcErrorMessage} from "../utils/admin-client";
import type {LoginResponse} from "../gen/admin-service.ts";

const AUTH_TOKEN_KEY = "auth_token";

export const setTokenInStorage = (token: string | null) => {
    if (token != null) localStorage.setItem(AUTH_TOKEN_KEY, token);
    else localStorage.removeItem(AUTH_TOKEN_KEY);
};
export const getTokenFromStorage = () => {
    return localStorage.getItem(AUTH_TOKEN_KEY) || "";
};

export function useLogin() {
    const [token, setToken] = useState<string | null>(() => {
        return localStorage.getItem(AUTH_TOKEN_KEY);
    });
    const [error, setError] = useState<string | null>(null);

    const updateAndStoreToken = (token: string | null, error: string | null) => {
        setToken(token);
        setTokenInStorage(token);
        setError(error);
    }

    // Listen for auth errors from the gRPC middleware
    useEffect(() => {
        const handleAuthError = (event: AuthErrorEvent) => {
            updateAndStoreToken(null, event.detail.message);
        };

        window.addEventListener(AUTH_ERROR_EVENT, handleAuthError as EventListener);
        return () => window.removeEventListener(AUTH_ERROR_EVENT, handleAuthError as EventListener);
    }, []);

    const login = useCallback(async (password: string) => {
        try {
            const response: LoginResponse = await adminClient.login({
                password: password
            });

            updateAndStoreToken(response.token, null);
        } catch (e) {
            console.error(e);
            setError(grpcErrorMessage("Failed to log in", e));
        }
    }, []);

    const logout = useCallback(async () => {
        updateAndStoreToken(null, null);
        window.location.reload();
    }, []);


    return {
        login,
        token,
        logout,
        setError,
        error
    };
}