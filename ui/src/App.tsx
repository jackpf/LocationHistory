import React, {useEffect} from 'react';
import {Circle, CircleMarker, MapContainer, Polyline, Popup, TileLayer, useMap} from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import './App.css'; // Your new CSS file
import {useAdminClient} from './hooks/use-admin-client.ts';
import type {StoredDevice} from "./gen/common.ts";

const DEFAULT_CENTER: [number, number] = [20, 0];
const DEFAULT_ZOOM = 3;
const DEFAULT_ZOOM_IN = 16;

function MapUpdater({center, selectedId}: { center: [number, number], selectedId: string | null }) {
    const map = useMap();
    const lastFlownToId = React.useRef<string | null>(null);

    useEffect(() => {
        const isWorldCenter = center[0] === DEFAULT_CENTER[0] && center[1] === DEFAULT_CENTER[1];
        if (selectedId && !isWorldCenter && selectedId !== lastFlownToId.current) {
            // Hide markers while flying to prevent zoom artifacts
            map.getContainer().classList.add('hide-while-flying');
            map.flyTo(center, DEFAULT_ZOOM_IN, {duration: 1.5});
            // Un-hide markers after flying complete
            map.once('moveend', () => {
                map.getContainer().classList.remove('hide-while-flying');
            });
            lastFlownToId.current = selectedId;
        }

        if (!selectedId) {
            lastFlownToId.current = null;
        }
    }, [selectedId, center, map]);

    return null;
}

function App() {
    const {
        devices,
        selectedDeviceId,
        setSelectedDeviceId,
        history,
        lastUpdated,
        error
    } = useAdminClient();

    const polylinePositions = history.map(loc => [loc.lat, loc.lon] as [number, number]);
    const lastLocation = history.length > 0 ? history[history.length - 1] : null;
    const mapCenter: [number, number] = lastLocation != null ? [lastLocation.lat, lastLocation.lon] : DEFAULT_CENTER;

    return (
        <div className="app-container">

            {/* Sidebar */}
            <aside className="sidebar">
                <div className="sidebar-header">
                    <h2>Devices</h2>
                    {error && <div className="error-text">{error}</div>}
                </div>

                {devices.map((storedDevice: StoredDevice) => storedDevice.device != null && (
                    <div
                        key={storedDevice.device.id}
                        onClick={() => setSelectedDeviceId(storedDevice.device.id)}
                        className={`device-item ${selectedDeviceId === storedDevice.device.id ? 'selected' : ''}`}
                    >
                        <strong>{storedDevice.device.id || "No ID"}</strong>
                        <div className="device-id-subtext">
                            ID: {storedDevice.device.id.substring(0, 6)}...
                        </div>
                    </div>
                ))}
            </aside>

            {/* Map Area */}
            <main className="map-area">
                <div className="map-overlay">
                    <strong>Points:</strong> {history.length} <br/>
                    <small>Updated: {lastUpdated.toLocaleTimeString()}</small>
                </div>

                <MapContainer center={DEFAULT_CENTER} zoom={DEFAULT_ZOOM} preferCanvas={true}>
                    <TileLayer
                        attribution='&copy; OpenStreetMap contributors'
                        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    />

                    <MapUpdater center={mapCenter} selectedId={selectedDeviceId}/>

                    <Polyline
                        positions={polylinePositions}
                        pathOptions={{color: 'blue', weight: 3, opacity: 0.3}}
                    />

                    {/* Historical Points */}
                    {history.map((loc, index) => (
                        <CircleMarker
                            key={index}
                            center={[loc.lat, loc.lon]}
                            radius={4}
                            pathOptions={{color: 'white', fillColor: 'blue', fillOpacity: 1, weight: 2}}
                        >
                            <Popup>
                                <strong>Latitude:</strong> {loc.lat}<br/>
                                <strong>Longitude:</strong> {loc.lon}<br/>
                                <strong>Accuracy:</strong> {loc.accuracy}m
                            </Popup>
                        </CircleMarker>
                    ))}

                    {/* Current Accuracy Circle */}
                    {lastLocation != null && (
                        <Circle
                            center={[lastLocation.lat, lastLocation.lon]}
                            radius={lastLocation.accuracy}
                            pathOptions={{fillColor: 'blue', fillOpacity: 0.2, stroke: false}}
                        />
                    )}
                </MapContainer>
            </main>
        </div>
    );
}

export default App;