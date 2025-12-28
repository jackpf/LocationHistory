import { useState, useEffect } from 'react';
import { adminClient } from '../grpc/admin-client';
import { StoredDevice, Location } from '../gen/common';
import type {ListDevicesResponse, ListLocationsResponse} from "../gen/admin-service.ts";

export function useDeviceManager() {
    const [devices, setDevices] = useState<StoredDevice[]>([]);
    const [selectedDeviceId, setSelectedDeviceId] = useState<string | null>(null);
    const [history, setHistory] = useState<Location[]>([]);
    const [lastUpdated, setLastUpdated] = useState<Date>(new Date());
    const [error, setError] = useState<string | null>(null);

    // Poll device list
    useEffect(() => {
        const fetchDevices = async () => {
            try {
                const response: ListDevicesResponse = await adminClient.listDevices({});
                console.log("ListDevicesResponse", response);
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

    // Poll locations for selected device
    useEffect(() => {
        if (!selectedDeviceId) {
            setHistory([]);
            return;
        }

        const fetchLocations = async () => {
            try {
                const response: ListLocationsResponse = await adminClient.listLocations({
                    device: { id: selectedDeviceId }
                });
                console.log("ListLocationsResponse", response);
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