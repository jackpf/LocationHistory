import React, {useState} from 'react';
import {type Device, DeviceStatus, type StoredDevice} from "../gen/common.ts";

interface DeviceListProps {
    devices: StoredDevice[];
    selectedDeviceId: string | null;
    setSelectedDeviceId: (deviceId: string) => void;
    approveDevice: (deviceId: string) => void;
    deleteDevice: (deviceId: string) => void;
    logout: () => void;
}

export const DeviceList: React.FC<DeviceListProps> = ({
                                                          devices,
                                                          selectedDeviceId,
                                                          setSelectedDeviceId,
                                                          approveDevice,
                                                          deleteDevice,
                                                          logout
                                                      }) => {
    const handleApprove = async (deviceId: string) => {
        await approveDevice(deviceId);
    };

    const handleDelete = async (deviceId: string) => {
        if (window.confirm("Are you sure you want to delete this device?")) {
            await deleteDevice(deviceId);
        }
        setOpenMenuId(null);
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
        if (deviceStatus === DeviceStatus.DEVICE_PENDING) {
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

    const [isOpen, setIsOpen] = useState(true);
    const [openMenuId, setOpenMenuId] = useState<string | null>(null);

    return (
        <>
            <button
                className={`sidebar-toggle ${isOpen ? 'open' : ''}`}
                onClick={() => setIsOpen(!isOpen)}
                aria-label="Toggle Sidebar"
            >
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <line x1="3" y1="12" x2="21" y2="12"></line>
                    <line x1="3" y1="6" x2="21" y2="6"></line>
                    <line x1="3" y1="18" x2="21" y2="18"></line>
                </svg>
            </button>

            <aside className={`sidebar ${isOpen ? 'open' : 'closed'}`}>
                <div className="sidebar-header">
                    <h2>Devices</h2>
                </div>

                {devices.length === 0 && <div className="no-devices">No devices</div>}

                {devices.map((storedDevice: StoredDevice) => {
                    const device: Device | undefined = storedDevice.device;
                    if (!device) return;

                    return (
                        <div
                            key={device.id}
                            onClick={() => setSelectedDeviceId(device.id)}
                            className={`device-item ${selectedDeviceId === device.id ? 'selected' : ''}`}
                        >
                            <div
                                className="device-menu-container"
                                onClick={(e) => e.stopPropagation()}
                            >
                                <button
                                    className="device-menu-trigger"
                                    onClick={() => setOpenMenuId(openMenuId === device.id ? null : device.id)}
                                >
                                    ⋮
                                </button>

                                {openMenuId === device.id && (
                                    <div className="device-menu-dropdown">
                                        <button
                                            className="device-delete-btn"
                                            onClick={() => handleDelete(device.id)}
                                        >
                                            Delete
                                        </button>
                                    </div>
                                )}
                            </div>

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

                <div className="logout-button">
                    <button onClick={logout}>Logout</button>
                </div>
            </aside>
        </>
    );
};