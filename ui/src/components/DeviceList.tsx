import React from 'react';
import type {Device, StoredDevice} from "../gen/common.ts";

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

            {devices.map((storedDevice: StoredDevice) => {
                const device: Device | undefined = storedDevice.device;
                if (!device) return;

                return (
                    <div
                        key={device.id}
                        onClick={() => setSelectedDeviceId(device.id)}
                        className={`device-item ${selectedDeviceId === device.id ? 'selected' : ''}`}
                    >
                        <strong>{device.id || "No ID"}</strong>
                        <div className="device-id-subtext">
                            ID: {device.id.substring(0, 6)}...
                        </div>
                    </div>
                )
            })}
        </aside>
    );
};