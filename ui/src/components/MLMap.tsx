import React, {useEffect, useMemo, useState} from 'react';
import Map, {Layer, NavigationControl, Popup, Source, useMap} from 'react-map-gl/maplibre';
import 'maplibre-gl/dist/maplibre-gl.css';
import {format, formatDistanceToNow} from 'date-fns';
import type {StoredLocation} from "../gen/common.ts";
import {type LngLatLike, type MapGeoJSONFeature} from "maplibre-gl";
import {MAPTILER_API_KEY} from "../config/config.ts";
import type {Point} from 'geojson';
import {Segmented} from "antd";
import {GlobalOutlined, MoonFilled, SunOutlined} from '@ant-design/icons';
import {useLocalStorage} from "../hooks/use-local-storage.ts";
import styles from './MLMap.module.css';
import circle from "@turf/circle";

if (!MAPTILER_API_KEY) {
    alert("MAPTILER_API_KEY must be set to use maptiler");
}

const getMapUrl = (style: string) => {
    return `https://api.maptiler.com/maps/${style}/style.json?key=${MAPTILER_API_KEY}`;
}
const DEFAULT_CENTER: [number, number] = [40, 0];
const DEFAULT_ZOOM = 2;
const DEFAULT_ZOOM_IN = 15;
const MAP_STYLE_OPTIONS = [
    {value: "streets-v2", label: <SunOutlined/>},
    {value: "base-v4-dark", label: <MoonFilled/>},
    {value: "satellite", label: <GlobalOutlined/>},
];

function MapUpdater({center, selectedId, history, forceRecenter, setForceRecenter}: {
    center: [number, number],
    selectedId: string | null,
    history: StoredLocation[],
    forceRecenter: boolean,
    setForceRecenter: (forceRecenter: boolean) => void,
}) {
    const {current: map} = useMap();

    const lastFlownId = React.useRef<string | null>(null);
    const lastHistory = React.useRef<StoredLocation[]>(history);

    useEffect(() => {
        if (!map || !selectedId) {
            lastFlownId.current = null;
            return;
        }

        const isNewDevice = selectedId !== lastFlownId.current;
        const isHistoryFresh = history !== lastHistory.current;
        const isWorldCenter = center[0] === DEFAULT_CENTER[0] && center[1] === DEFAULT_CENTER[1];

        if ((forceRecenter && !isNewDevice) || (isNewDevice && isHistoryFresh && !isWorldCenter)) {
            const targetCenter: LngLatLike = [center[1], center[0]];

            map.flyTo({
                center: targetCenter,
                zoom: DEFAULT_ZOOM_IN,
                duration: 1500
            });

            lastFlownId.current = selectedId;
            setForceRecenter(false);
        }

        lastHistory.current = history;
    }, [selectedId, center, map, history, forceRecenter]);

    return null;
}

interface MLMapProps {
    history: StoredLocation[];
    selectedDeviceId: string | null;
    forceRecenter: boolean;
    setForceRecenter: (forceRecenter: boolean) => void;
}

export const MLMap: React.FC<MLMapProps> = ({history, selectedDeviceId, forceRecenter, setForceRecenter}) => {
    const [popupInfo, setPopupInfo] = useState<MapGeoJSONFeature | null>(null);
    const [cursor, setCursor] = useState('');
    const [mapStyle, setMapStyle] = useLocalStorage("ml_map_style", MAP_STYLE_OPTIONS[0].value);
    const mapUrl = useMemo(() => getMapUrl(mapStyle), [mapStyle]);

    const lastLocation: StoredLocation | null = history.length > 0 ? history[history.length - 1] : null;
    const mapCenter: [number, number] = lastLocation != null && lastLocation.location != null ?
        [lastLocation.location.lat, lastLocation.location.lon] : DEFAULT_CENTER;

    const geoJsonData = useMemo(() => {
        return {
            type: 'FeatureCollection',
            features: history
                .filter(h => !!h.location)
                .slice(history.length - 21, history.length - 1)
                .map((h, index) => ({
                    type: 'Feature',
                    properties: {
                        // Popup properties
                        lat: h.location!.lat,
                        lon: h.location!.lon,
                        accuracy: h.location!.accuracy,
                        time: h.timestamp,
                        index: index
                    },
                    geometry: {
                        type: 'Point',
                        coordinates: [h.location!.lon, h.location!.lat]
                    }
                }))
        };
    }, [history]);

    const lineGeoJson = useMemo(() => {
        return {
            type: 'Feature',
            properties: {},
            geometry: {
                type: 'LineString',
                coordinates: history
                    .filter(h => !!h.location)
                    .map(h => [h.location!.lon, h.location!.lat])
            }
        };
    }, [history]);

    let startTime = 0
    let endTime = 0
    if (history.length > 0) {
        startTime = history[0].timestamp;
        endTime = history[history.length - 1].timestamp;
    }
    const totalDuration = endTime - startTime;

    // 2. Calculate "24 hours ago" timestamp
    const twentyFourHoursAgo = Date.now() - (24 * 60 * 60 * 1000);

    // 3. Find where "24h ago" is as a fraction (0.0 to 1.0) of the trip
    // Example: If trip is 48h long, 24h ago is at 0.5 (50%)
    let cutoffRatio = (twentyFourHoursAgo - startTime) / totalDuration;

    // Clamp it between 0 and 1 (in case trip is shorter than 24h or fully in past)
    cutoffRatio = Math.max(0, Math.min(1, cutoffRatio));

    // 4. Create the Gradient Style dynamically
    const lineStyle = {
        id: 'route-line',
        type: 'line',
        paint: {
            'line-width': 4,
            'line-gradient': [
                'interpolate',
                ['linear'],
                ['line-progress'],

                // From Start (0.0) to Cutoff point -> Faint (Old)
                0, 'rgba(0, 0, 255, 0.05)',
                cutoffRatio, 'rgba(0, 0, 255, 0.2)',

                // Small transition zone (e.g. +5%) to fade into Solid (New)
                // If you want a hard cut, make this number same as cutoffRatio
                Math.min(1, cutoffRatio + 0.1), 'rgba(0, 0, 255, 0.3)',

                // To End (1.0) -> Solid
                1, 'rgba(0, 0, 255, 0.3)'
            ]
        }
    };

    // 2. Generate the Accuracy Polygon (Geometry)
    // Only do this if we have a valid point and accuracy > 0
    const accuracyCircle = useMemo(() => {
        if (history.length == 0) return null;
        const lastHistory = history[history.length - 1];

        const {lat, lon, accuracy} = lastHistory.location!;
        if (!accuracy || accuracy < 1) return null; // Hide if 0 or null

        // turf/circle takes [lon, lat], radius, options
        return circle([lon, lat], accuracy, {
            steps: 64,       // Smoothness of the circle
            units: 'meters'  // CRITICAL: Interpret radius as meters
        });
    }, [history]);

    return (
        <main className={styles.mapArea}>

            {/* Map settings */}
            <div className={styles.mapSettings}>
                <Segmented
                    vertical
                    className={styles.segmentedControl}
                    options={MAP_STYLE_OPTIONS}
                    value={mapStyle}
                    onChange={setMapStyle}
                />
            </div>

            {/* Overlay UI */}
            {selectedDeviceId && (
                <div className="map-overlay">
                    <strong>Points:</strong> {history.length} <br/>
                    <small>
                        Updated: {lastLocation
                        ? formatDistanceToNow(new Date(lastLocation.timestamp), {addSuffix: true})
                        : "never"}
                    </small>
                </div>
            )}

            <Map
                initialViewState={{
                    longitude: DEFAULT_CENTER[1],
                    latitude: DEFAULT_CENTER[0],
                    zoom: DEFAULT_ZOOM
                }}
                style={{width: '100%', height: '100%'}}
                mapStyle={mapUrl}
                interactiveLayerIds={['history-points']}
                cursor={cursor}
                onMouseEnter={() => setCursor('pointer')}
                onMouseLeave={() => setCursor('')}
                onClick={(event) => {
                    if (event.features && event.features.length > 0) {
                        setPopupInfo(event.features[0]);
                    } else {
                        setPopupInfo(null);
                    }
                }}
            >
                <MapUpdater
                    center={mapCenter}
                    selectedId={selectedDeviceId}
                    history={history}
                    forceRecenter={forceRecenter}
                    setForceRecenter={setForceRecenter}
                />

                <NavigationControl position="bottom-right"/>

                {/* Line */}
                <Source type="geojson" data={lineGeoJson as any} lineMetrics={true}>
                    <Layer {...lineStyle}/>
                </Source>

                {/* Points */}
                <Source type="geojson" data={geoJsonData as any}>
                    <Layer
                        id="history-points"
                        type="circle"
                        paint={{
                            "circle-radius": 4,
                            "circle-color": "blue",
                            "circle-stroke-width": 2,
                            "circle-stroke-color": "white"
                        }}
                    />
                </Source>

                {accuracyCircle && (
                    <Source id="accuracy-zone" type="geojson" data={accuracyCircle}>
                        <Layer
                            id="accuracy-fill"
                            type="fill"
                            paint={{
                                'fill-color': 'blue', // Match your theme
                                'fill-opacity': 0.3,  // Very faint (10%)
                                'fill-outline-color': 'blue' // Optional: slight border
                            }}
                        />
                    </Source>
                )}

                {popupInfo && (
                    <Popup
                        longitude={(popupInfo.geometry as Point).coordinates[0]}
                        latitude={(popupInfo.geometry as Point).coordinates[1]}
                        anchor="bottom"
                        onClose={() => setPopupInfo(null)}
                    >
                        <div>
                            <strong>Latitude:</strong> {popupInfo.properties.lat}<br/>
                            <strong>Longitude:</strong> {popupInfo.properties.lon}<br/>
                            <strong>Accuracy:</strong> {popupInfo.properties.accuracy}m<br/>
                            <strong>Time:</strong> {format(new Date(popupInfo.properties.time), 'yyyy-MM-dd HH:mm:ss')}
                        </div>
                    </Popup>
                )}

            </Map>
        </main>
    );
}