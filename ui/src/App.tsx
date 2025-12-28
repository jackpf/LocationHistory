import React, {useEffect} from 'react'
import {Circle, CircleMarker, MapContainer, Popup, TileLayer, useMap} from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import './App.css'
import type {LocationPoint} from './model/location-point'
import {useDeviceManager} from './hooks/use-device-manager'
import type {StoredDevice} from "./gen/common.ts";

function MapUpdater({center, zoom}: { center: [number, number], zoom: number }) {
    const map = useMap();

    useEffect(() => {
        console.log('Flying to center', center, 'with zoom', zoom);
        if (center) {
            map.flyTo(center, zoom);
        }
    }, [center, map, zoom]);

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

    const WORLD_CENTER: [number, number] = [20, 0];
    const WORLD_ZOOM = 3;

    const locationPoints: LocationPoint[] = history.map((item, index) => ({
        id: index,
        lat: item.lat,
        lng: item.lon,
        accuracy: item.accuracy,
        timestamp: new Date().toLocaleTimeString() // Generate a placeholder time
    }));
    const lastLocation: Location = locationPoints.length > 0 ? locationPoints[locationPoints.length - 1] : null
    const mapCenter = lastLocation != null ? [lastLocation.lat, lastLocation.lng] : WORLD_CENTER
    const mapZoom = lastLocation != null ? 13 : WORLD_ZOOM;

    // const polylinePositions = history.map(loc => [loc.lat, loc.lon] as [number, number]);
    // const currentPolylinePosition = polylinePositions.length > 0 ? polylinePositions[polylinePositions.length - 1] : null;

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
                    background: 'white',
                    padding: '10px'
                }}>
                    <strong>Points:</strong> {history.length} <br/>
                    <small>Updated: {lastUpdated.toLocaleTimeString()}</small>
                </div>

                <MapContainer
                    center={WORLD_CENTER}
                    zoom={WORLD_ZOOM}
                    style={{height: "100%", width: "100%"}}
                >
                    <TileLayer attribution='&copy; OpenStreetMap contributors'
                               url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"/>

                    <MapUpdater center={mapCenter} zoom={mapZoom}/>

                    {/*<Polyline positions={polylinePositions} pathOptions={{ color: 'blue' }} />*/}

                    {locationPoints.map(loc => (
                        <React.Fragment key={loc.id}>
                            <Circle
                                center={[loc.lat, loc.lng]}
                                radius={loc.accuracy}
                                pathOptions={{color: 'blue', fillColor: 'blue', fillOpacity: 0.1, stroke: false}}
                            />

                            <CircleMarker
                                center={[loc.lat, loc.lng]}
                                radius={4} // Fixed 4px dot
                                pathOptions={{color: 'white', fillColor: 'blue', fillOpacity: 1, weight: 2}}
                            >
                                <Popup>
                                    <strong>Time:</strong> {loc.timestamp}<br/>
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