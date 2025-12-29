import {useEffect, useState} from 'react';
import {adminClient} from '../grpc/admin-client';
import {StoredDevice, type StoredLocation} from '../gen/common';
import type {ListDevicesResponse, ListLocationsResponse} from "../gen/admin-service.ts";

export function useAdminClient() {
    const [devices, setDevices] = useState<StoredDevice[]>([]);
    const [selectedDeviceId, setSelectedDeviceId] = useState<string | null>(null);
    const [history, setHistory] = useState<StoredLocation[]>([]);
    const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
    const [error, setError] = useState<string | null>(null);

    const lastUpdatedLocation = (locations: StoredLocation[]): Date | null => {
        if (!locations || !locations.length) return null;

        return new Date(locations[locations.length - 1].timestamp);
    }

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
                setError("Failed to fetch devices");
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
                } as any);
                console.log("ListLocationsResponse", response);
                setHistory(response.locations);
                setLastUpdated(lastUpdatedLocation(response.locations));
            } catch (e) {
                console.error(e);
                setError("Failed to fetch locations");
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