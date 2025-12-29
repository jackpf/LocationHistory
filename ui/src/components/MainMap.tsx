import React, {useEffect} from 'react';
import {Circle, CircleMarker, MapContainer, Polyline, Popup, TileLayer, useMap} from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import type {Location} from '../gen/common';

const DEFAULT_CENTER: [number, number] = [20, 0];
const DEFAULT_ZOOM = 3;
const DEFAULT_ZOOM_IN = 16;

function MapUpdater({center, selectedId}: { center: [number, number], selectedId: string | null }) {
    const map = useMap();
    const lastFlownToId = React.useRef<string | null>(null);

    useEffect(() => {
        const isWorldCenter = center[0] === DEFAULT_CENTER[0] && center[1] === DEFAULT_CENTER[1];
        if (selectedId && !isWorldCenter && selectedId !== lastFlownToId.current) {
            map.getContainer().classList.add('hide-while-flying');
            map.flyTo(center, DEFAULT_ZOOM_IN, {duration: 1.5});
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

interface MainMapProps {
    history: Location[];
    lastUpdated: Date;
    selectedDeviceId: string | null;
}

export const MainMap: React.FC<MainMapProps> = ({history, lastUpdated, selectedDeviceId}) => {
    const polylinePositions = history.map(loc => [loc.lat, loc.lon] as [number, number]);
    const lastLocation = history.length > 0 ? history[history.length - 1] : null;
    const mapCenter: [number, number] = lastLocation != null ? [lastLocation.lat, lastLocation.lon] : DEFAULT_CENTER;

    return (
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
    );
};