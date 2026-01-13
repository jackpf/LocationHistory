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
                <div className={styles.mapOverlay}>
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
                <Source type="geojson" data={lineGeoJson as any}>
                    <Layer
                        id="route-line"
                        type="line"
                        paint={{
                            "line-color": "blue",
                            "line-width": 3,
                            "line-opacity": 0.3,
                        }}
                    />
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