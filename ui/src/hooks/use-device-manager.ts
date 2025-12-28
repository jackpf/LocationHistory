import { useState, useEffect } from 'react';
import { adminClient } from '../grpc/admin-client';
import { StoredDevice, Location } from '../gen/common';

export function useDeviceManager() {
    const [devices, setDevices] = useState<StoredDevice[]>([]);
    const [selectedDeviceId, setSelectedDeviceId] = useState<string | null>(null);
    const [history, setHistory] = useState<Location[]>([]);
    const [lastUpdated, setLastUpdated] = useState<Date>(new Date());
    const [error, setError] = useState<string | null>(null);

    // 1. Poll Device List
    useEffect(() => {
        const fetchDevices = async () => {
            try {
                const response = await adminClient.listDevices({});
                setDevices(response.devices);
                setError(null);
            } catch (e) {
                console.error(e);
                setError("Failed to fetch devices. Is the proxy running?");
            }
        };

        fetchDevices();
        const interval = setInterval(fetchDevices, 10000);
        return () => clearInterval(interval);
    }, []);

    // 2. Poll Locations for Selected Device
    useEffect(() => {
        if (!selectedDeviceId) {
            setHistory([]);
            return;
        }

        const fetchLocations = async () => {
            try {
                const response = await adminClient.listLocations({
                    device: { id: selectedDeviceId }
                });
                setHistory(response.locations);
                setLastUpdated(new Date());
            } catch (e) {
                console.error(e);
            }
        };

        fetchLocations();
        const interval = setInterval(fetchLocations, 10000);
        return () => clearInterval(interval);
    }, [selectedDeviceId]);

    // Return everything the UI needs
    return {
        devices,
        selectedDeviceId,
        setSelectedDeviceId,
        history,
        lastUpdated,
        error
    };
}