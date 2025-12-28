import React, {useEffect, useState} from 'react'
import {Circle, CircleMarker, MapContainer, Polyline, Popup, TileLayer, useMap} from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import './App.css'
import type {LocationPoint} from './model/location-point'
import {useDeviceManager} from './hooks/use-device-manager'
import type {StoredDevice} from "./gen/common.ts";

const DEFAULT_CENTER: [number, number] = [20, 0];
const DEFAULT_ZOOM = 3;
const DEFAULT_ZOOM_IN = 16;

function MapUpdater({center, selectedId}: { center: [number, number], selectedId: string | null }) {
    const map = useMap();
    const [flownToId, setFlownToId] = useState<string | null>(null);

    useEffect(() => {
        const isWorldCenter = center[0] === DEFAULT_CENTER[0] && center[1] === DEFAULT_CENTER[1];
        console.log('Flying to center', center, 'with zoom', DEFAULT_ZOOM_IN);
        if (selectedId && !isWorldCenter && selectedId !== flownToId) {
            map.flyTo(center, DEFAULT_ZOOM_IN, {duration: 1.5});
            setFlownToId(selectedId);
        }
    }, [selectedId, center, flownToId, map]);

    useEffect(() => {
        if (!selectedId) setFlownToId(null);
    }, [selectedId]);

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
    } = useDeviceManager();

    const locationPoints: LocationPoint[] = history.map((item, index) => ({
        id: index,
        lat: item.lat,
        lng: item.lon,
        accuracy: item.accuracy,
        timestamp: new Date().toLocaleTimeString() // Generate a placeholder time
    }));
    const polylinePositions = locationPoints.map(loc => [loc.lat, loc.lng] as [number, number]);
    const lastLocation: Location = locationPoints.length > 0 ? locationPoints[locationPoints.length - 1] : null
    const mapCenter = lastLocation != null ? [lastLocation.lat, lastLocation.lng] : DEFAULT_CENTER

    return (
        <div style={{display: 'flex', height: '100vh', width: '100vw'}}>

            {/* Sidebar */}
            <div style={{width: '300px', borderRight: '1px solid #ccc', overflowY: 'auto', background: '#1a1212'}}>
                <div style={{padding: '1rem', borderBottom: '1px solid #ddd'}}>
                    <h2>Devices</h2>
                    {error && <small style={{color: 'red'}}>{error}</small>}
                </div>

                {devices.map((storedDevice: StoredDevice) => (
                    <div
                        key={storedDevice.device.id}
                        onClick={() => setSelectedDeviceId(storedDevice.device.id)}
                        style={{
                            padding: '1rem',
                            cursor: 'pointer',
                            background: selectedDeviceId === storedDevice.device.id ? '#e3f2fd' : 'transparent',
                            borderBottom: '1px solid #eee'
                        }}
                    >
                        <strong>{storedDevice.device.id || "No ID"}</strong>
                        <div
                            style={{fontSize: '0.8rem', color: '#666'}}>ID: {storedDevice.device.id.substring(0, 6)}...
                        </div>
                    </div>
                ))}
            </div>

            {/* Map Area */}
            <div style={{flex: 1, position: 'relative'}}>
                <div style={{
                    position: 'absolute',
                    top: 10,
                    right: 10,
                    zIndex: 1000,
                    background: 'black',
                    padding: '10px'
                }}>
                    <strong>Points:</strong> {history.length} <br/>
                    <small>Updated: {lastUpdated.toLocaleTimeString()}</small>
                </div>

                <MapContainer
                    center={DEFAULT_CENTER}
                    zoom={DEFAULT_ZOOM}
                    style={{height: "100%", width: "100%"}}
                >
                    <TileLayer attribution='&copy; OpenStreetMap contributors'
                               url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"/>

                    <MapUpdater center={mapCenter} selectedId={selectedDeviceId}/>

                    <Polyline positions={polylinePositions} pathOptions={{
                        color: 'blue',
                        weight: 3,
                        opacity: 0.3
                    }}/>

                    {lastLocation != null && (
                        <React.Fragment key={lastLocation.id}>
                            <CircleMarker
                                center={[lastLocation.lat, lastLocation.lng]}
                                radius={4} // Fixed 4px dot
                                pathOptions={{
                                    color: 'white',
                                    fillColor: 'blue',
                                    fillOpacity: 1,
                                    weight: 2
                                }}
                            >
                                <Popup>
                                    <strong>Time:</strong> {lastLocation.timestamp}<br/>
                                    <strong>Latitude:</strong> {lastLocation.lat} meters<br/>
                                    <strong>Longitude:</strong> {lastLocation.lng} meters<br/>
                                    <strong>Accuracy:</strong> {lastLocation.accuracy} meters
                                </Popup>
                            </CircleMarker>

                            <Circle
                                center={[lastLocation.lat, lastLocation.lng]}
                                radius={lastLocation.accuracy}
                                pathOptions={{
                                    color: 'blue',
                                    fillColor: 'blue',
                                    fillOpacity: 0.5,
                                    stroke: false
                                }}
                            />
                        </React.Fragment>
                    )}

                    {locationPoints.map(loc => (
                        <React.Fragment key={loc.id}>
                            <CircleMarker
                                center={[loc.lat, loc.lng]}
                                radius={4} // Fixed 4px dot
                                pathOptions={{
                                    color: 'white',
                                    fillColor: 'blue',
                                    fillOpacity: 1,
                                    weight: 2
                                }}
                            >
                                <Popup>
                                    <strong>Time:</strong> {loc.timestamp}<br/>
                                    <strong>Latitude:</strong> {loc.lat} meters<br/>
                                    <strong>Longitude:</strong> {loc.lng} meters<br/>
                                    <strong>Accuracy:</strong> {loc.accuracy} meters
                                </Popup>
                            </CircleMarker>
                        </React.Fragment>
                    ))}

                </MapContainer>
            </div>
        </div>
    )
}

export default App