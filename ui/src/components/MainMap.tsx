import React, {useEffect} from 'react';
import {Circle, CircleMarker, MapContainer, Polyline, Popup, TileLayer, useMap} from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import type {StoredLocation} from '../gen/common';
import {format, formatDistanceToNow} from "date-fns";

const DEFAULT_CENTER: [number, number] = [20, 0];
const DEFAULT_ZOOM = 3;
const DEFAULT_ZOOM_IN = 16;

function MapUpdater({center, selectedId, history}: {
    center: [number, number],
    selectedId: string | null,
    history: Location[]
}) {
    const map = useMap();

    const lastFlownId = React.useRef<string | null>(null);
    const lastHistory = React.useRef<Location[]>(history);

    useEffect(() => {
        if (!selectedId) {
            lastFlownId.current = null;
            return;
        }

        const isNewDevice = selectedId !== lastFlownId.current;
        const isHistoryFresh = history !== lastHistory.current;
        const isWorldCenter = center[0] === DEFAULT_CENTER[0] && center[1] === DEFAULT_CENTER[1];

        if (isNewDevice && isHistoryFresh && !isWorldCenter) {
            map.getContainer().classList.add('hide-while-flying');

            map.flyTo(center, DEFAULT_ZOOM_IN, {
                duration: 1.5
            });

            map.once('moveend', () => {
                map.getContainer().classList.remove('hide-while-flying');
            });

            lastFlownId.current = selectedId;
        }

        lastHistory.current = history;
    }, [selectedId, center, map, history]);

    return null;
}

interface MainMapProps {
    history: StoredLocation[];
    lastUpdated: Date | null;
    selectedDeviceId: string | null;
}

export const MainMap: React.FC<MainMapProps> = ({history, lastUpdated, selectedDeviceId}) => {
    const polylinePositions: [number, number][] = history.flatMap(loc => {
        if (!loc.location) return [];
        return [[loc.location.lat, loc.location.lon] as [number, number]];
    });
    const lastLocation: StoredLocation | null = history.length > 0 ? history[history.length - 1] : null;
    const mapCenter: [number, number] = lastLocation != null && lastLocation.location != null ?
        [lastLocation.location.lat, lastLocation.location.lon] : DEFAULT_CENTER;

    return (
        <main className="map-area">
            {selectedDeviceId && (<div className="map-overlay">
                <strong>Points:</strong> {history.length} <br/>
                <small>Updated: {lastUpdated != null ? formatDistanceToNow(lastUpdated, {addSuffix: true}) : "never"}</small>
            </div>)}

            <MapContainer center={DEFAULT_CENTER} zoom={DEFAULT_ZOOM} preferCanvas={true}>
                <TileLayer
                    attribution="&copy; OpenStreetMap contributors"
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />

                <MapUpdater center={mapCenter} selectedId={selectedDeviceId} history={history}/>

                <Polyline
                    positions={polylinePositions}
                    pathOptions={{color: 'blue', weight: 3, opacity: 0.3}}
                />

                {/* Historical Points */}
                {history.map((storedLocation, index) => {
                    const location = storedLocation.location;
                    if (!location) return;

                    return (
                        <CircleMarker
                            key={index}
                            center={[location.lat, location.lon]}
                            radius={4}
                            pathOptions={{color: 'white', fillColor: 'blue', fillOpacity: 1, weight: 2}}
                        >
                            <Popup>
                                <strong>Latitude:</strong> {location.lat}<br/>
                                <strong>Longitude:</strong> {location.lon}<br/>
                                <strong>Accuracy:</strong> {location.accuracy}m
                                <strong>Time:</strong> {format(new Date(storedLocation.timestamp), 'yyyy-MM-dd HH:mm:ss')}
                            </Popup>
                        </CircleMarker>
                    )
                })}

                {/* Current Accuracy Circle */}
                {lastLocation != null && lastLocation.location != null && (
                    <Circle
                        center={[lastLocation.location.lat, lastLocation.location.lon]}
                        radius={lastLocation.location.accuracy}
                        pathOptions={{fillColor: 'blue', fillOpacity: 0.2, stroke: false}}
                    />
                )}
            </MapContainer>
        </main>
    );
};