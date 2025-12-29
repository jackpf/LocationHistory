import React from 'react';
import type {StoredDevice} from "../gen/common.ts";

interface DeviceListProps {
    devices: StoredDevice[];
    selectedDeviceId: string | null;
    setSelectedDeviceId: (deviceId: string) => void;
}

export const DeviceList: React.FC<DeviceListProps> = ({devices, selectedDeviceId, setSelectedDeviceId}) => {
    return (
        <aside className="sidebar">
            <div className="sidebar-header">
                <h2>Devices</h2>
            </div>

            {devices.map((storedDevice: StoredDevice) => storedDevice.device != null && (
                <div
                    key={storedDevice.device.id}
                    onClick={() => storedDevice.device != null ? setSelectedDeviceId(storedDevice.device.id) : null}
                    className={`device-item ${selectedDeviceId === storedDevice.device.id ? 'selected' : ''}`}
                >
                    <strong>{storedDevice.device.id || "No ID"}</strong>
                    <div className="device-id-subtext">
                        ID: {storedDevice.device.id.substring(0, 6)}...
                    </div>
                </div>
            ))}
        </aside>
    );
};