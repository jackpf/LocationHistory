import React from 'react';
import {type Device, DeviceStatus, type StoredDevice} from "../gen/common.ts";

interface DeviceListProps {
    devices: StoredDevice[];
    selectedDeviceId: string | null;
    setSelectedDeviceId: (deviceId: string) => void;
    approveDevice: (deviceId: string) => void;
}

export const DeviceList: React.FC<DeviceListProps> = ({
                                                          devices,
                                                          selectedDeviceId,
                                                          setSelectedDeviceId,
                                                          approveDevice
                                                      }) => {
    const handleApprove = async (deviceId: string) => {
        approveDevice(deviceId);
    };

    const showDeviceStatus = (deviceStatus: DeviceStatus) => {
        switch (deviceStatus) {
            case DeviceStatus.DEVICE_REGISTERED:
                return <span><span style={{color: 'green'}}>●</span> Registered</span>;
            case DeviceStatus.DEVICE_PENDING:
                return <span><span style={{color: 'yellow'}}>●</span> Pending Approval</span>;
            case DeviceStatus.DEVICE_UNKNOWN:
            default:
                return <span><span style={{color: 'gray'}}>●</span> Unknown</span>;
        }
    }

    const showApproveDeviceIfPending = (deviceId: string, deviceStatus: DeviceStatus) => {
        if (deviceStatus == DeviceStatus.DEVICE_REGISTERED) {
            return <button
                className="approve-btn"
                onClick={(e) => {
                    e.stopPropagation();
                    handleApprove(deviceId);
                }}
                title="Approve Device"
            >
                Approve Device
            </button>
        }
    }

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
                        <div className="device-details">
                            <div className="detail-row">
                                <span className="detail-label">Status:</span>
                                <span>{showDeviceStatus(storedDevice.status)}</span>
                            </div>
                            <div className="detail-row">
                                <span className="detail-label"></span>
                                <span>{showApproveDeviceIfPending(device.id, storedDevice.status)}</span>
                            </div>
                        </div>
                    </div>
                )
            })}
        </aside>
    );
};