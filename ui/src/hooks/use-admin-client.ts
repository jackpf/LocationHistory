import {useCallback, useEffect, useState} from 'react';
import {adminClient, grpcErrorMessage} from '../grpc/admin-client';
import {StoredDevice, type StoredLocation} from '../gen/common';
import type {ListDevicesResponse, ListLocationsResponse} from "../gen/admin-service.ts";

export function useAdminClient(refreshInterval: number) {
    const [devices, setDevices] = useState<StoredDevice[]>([]);
    const [selectedDeviceId, setSelectedDeviceId] = useState<string | null>(null);
    const [history, setHistory] = useState<StoredLocation[]>([]);
    const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
    const [error, setError] = useState<string | null>(null);

    const lastUpdatedLocation = (locations: StoredLocation[]): Date | null => {
        if (!locations || !locations.length) return null;

        return new Date(locations[locations.length - 1].timestamp);
    }

    const fetchDevices = useCallback(async () => {
        try {
            const response: ListDevicesResponse = await adminClient.listDevices({});
            console.log("ListDevicesResponse", response);
            setDevices(response.devices);
            setError(null);
        } catch (e) {
            console.error(e);
            setError(grpcErrorMessage("Failed to fetch devices", e));
        }
    }, []);

    const fetchLocations = useCallback(async () => {
        try {
            const response: ListLocationsResponse = await adminClient.listLocations({
                device: { id: selectedDeviceId }
            } as any);
            console.log("ListLocationsResponse", response);
            setHistory(response.locations);
            setLastUpdated(lastUpdatedLocation(response.locations));
        } catch (e) {
            console.error(e);
            setError(grpcErrorMessage("Failed to fetch locations", e));
        }
    }, [selectedDeviceId]);

    const approveDevice = async (deviceId: string) => {
        try {
            await adminClient.approveDevice(
                {device: {id: deviceId}} as any,
            );
            // Refresh list immediately to show the checkmark/status change
            await fetchDevices();
        } catch (e) {
            console.error(e);
            setError(grpcErrorMessage("Failed to approve device", e));
        }
    };

    const deleteDevice = async (deviceId: string) => {
        try {
            await adminClient.deleteDevice(
                {device: {id: deviceId}} as any,
            );
            // Refresh list immediately to show the checkmark/status change
            await fetchDevices();
        } catch (e) {
            console.error(e);
            setError(grpcErrorMessage("Failed to delete device", e));
        }
    };

    // Poll device list
    useEffect(() => {
        fetchDevices();
        const interval = setInterval(fetchDevices, refreshInterval);
        return () => clearInterval(interval);
    }, [fetchDevices, refreshInterval]);

    // Poll locations for selected device
    useEffect(() => {
        if (!selectedDeviceId) {
            setHistory([]);
            return;
        }

        fetchLocations();
        const interval = setInterval(fetchLocations, refreshInterval);
        return () => clearInterval(interval);
    }, [fetchLocations, selectedDeviceId, refreshInterval]);

    // Return everything the UI needs
    return {
        setSelectedDeviceId,
        approveDevice,
        deleteDevice,
        devices,
        selectedDeviceId,
        history,
        lastUpdated,
        error
    };
}