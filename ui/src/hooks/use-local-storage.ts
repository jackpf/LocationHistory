import {useEffect, useState} from 'react';

export function useLocalStorage<T>(key: string, defaultValue: T) {
    const [value, setValue] = useState(() => {
        try {
            const saved = window.localStorage.getItem(key);
            if (saved !== null) {
                return JSON.parse(saved);
            }
        } catch (e) {
            console.error(`Error reading localStorage key "${key}":`, e);
        }
        return defaultValue;
    });

    useEffect(() => {
        try {
            window.localStorage.setItem(key, JSON.stringify(value));
        } catch (e) {
            console.error(`Error setting localStorage key "${key}":`, e);
        }
    }, [key, value]);

    return [value, setValue];
}