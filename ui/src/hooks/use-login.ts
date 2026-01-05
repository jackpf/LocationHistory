import {useCallback, useState} from 'react';
import {adminClient, grpcErrorMessage} from '../grpc/admin-client';
import type {LoginResponse} from "../gen/admin-service.ts";

export const setTokenInStorage = (token: string | null) => {
    if (token != null) localStorage.setItem('auth_token', token);
    else localStorage.removeItem('auth_token');
};
export const getTokenFromStorage = () => {
    return localStorage.getItem('auth_token') || "";
};

export function useLogin() {
    const [token, setToken] = useState<string | null>(() => {
        return localStorage.getItem('auth_token');
    });
    const [error, setError] = useState<string | null>(null);

    const login = useCallback(async (password: string) => {
        try {
            console.log("Logging in...");
            const response: LoginResponse = await adminClient.login({
                password: password
            } as any);
            console.log("LoginResponse", response);
            setToken(response.token);
            setTokenInStorage(response.token)
            setError(null);
        } catch (e) {
            console.error(e);
            setError(grpcErrorMessage("Failed to log in", e));
        }
    }, []);

    const logout = useCallback(async () => {
        console.log("Logging out...");
        setToken(null);
        setTokenInStorage(null)
        setError(null);
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