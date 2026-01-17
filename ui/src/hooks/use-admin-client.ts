import {useCallback, useEffect, useState} from 'react';
import {adminClient, grpcErrorMessage} from '../utils/admin-client';
import {type StoredLocation} from '../gen/common';
import {
    type ListDevicesResponse,
    type ListLocationsResponse,
    NotificationType,
    type StoredDeviceWithMetadata
} from "../gen/admin-service.ts";

export function useAdminClient(refreshInterval: number) {
    const [devices, setDevices] = useState<StoredDeviceWithMetadata[]>([]);
    const [selectedDeviceId, setSelectedDeviceId] = useState<string | null>(null);
    const [history, setHistory] = useState<StoredLocation[]>([]);
    const [error, setError] = useState<string | null>(null);

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
            if (selectedDeviceId == null) return;
            const response: ListLocationsResponse = await adminClient.listLocations({
                deviceId: selectedDeviceId
            });
            console.log("ListLocationsResponse", response);
            setHistory(response.locations);
        } catch (e) {
            console.error(e);
            setError(grpcErrorMessage("Failed to fetch locations", e));
        }
    }, [selectedDeviceId]);

    const approveDevice = async (deviceId: string) => {
        try {
            await adminClient.approveDevice(
                {deviceId: deviceId},
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
                {deviceId: deviceId},
            );
            // Refresh list immediately to show the checkmark/status change
            await fetchDevices();
        } catch (e) {
            console.error(e);
            setError(grpcErrorMessage("Failed to delete device", e));
        }
    };

    const sendNotification = useCallback(async (deviceId: string, notificationType: NotificationType) => {
        try {
            return await adminClient.sendNotification(
                {deviceId: deviceId, notificationType: notificationType},
            );
        } catch (e) {
            console.error(e);
            setError(grpcErrorMessage("Failed to send notification", e));
        }
    }, []);

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
        sendNotification,
        devices,
        selectedDeviceId,
        history,
        error
    };
}