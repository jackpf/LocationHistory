import React, {useEffect, useMemo, useState} from "react";
import Map, {Layer, NavigationControl, Popup, Source} from "react-map-gl/maplibre";
import "maplibre-gl/dist/maplibre-gl.css";
import {format, formatDistanceToNow} from "date-fns";
import type {StoredLocation} from "../gen/common.ts";
import {type MapGeoJSONFeature} from "maplibre-gl";
import type {Point} from "geojson";
import {Segmented} from "antd";
import {useLocalStorage} from "../hooks/use-local-storage.ts";
import styles from "./MLMap.module.css";
import {accuracyCircleStyle, circlePoint, lineStyle, pointStyle} from "./MLMapStyles.tsx";
import {DEFAULT_CENTER, DEFAULT_ZOOM, getMapUrl, mapStyleOptions, MapType, POINT_LIMIT} from "./MLMapConfig.tsx";
import {MapUpdater} from "./MLMapUpdater.tsx";
import {MAP_TYPE} from "../config/config.ts";

interface MLMapProps {
    history: StoredLocation[];
    selectedDeviceId: string | null;
    forceRecenter: boolean;
    setForceRecenter: (forceRecenter: boolean) => void;
}

export const MLMap: React.FC<MLMapProps> = ({history, selectedDeviceId, forceRecenter, setForceRecenter}) => {
    const [popupInfo, setPopupInfo] = useState<MapGeoJSONFeature | null>(null);
    const [cursor, setCursor] = useState("");
    const [mapStyle, setMapStyle] = useLocalStorage("ml_map_style", mapStyleOptions(MAP_TYPE)[0].value);
    const [currentTime, setCurrentTime] = useState(() => Date.now());

    const mapUrl = useMemo(() => getMapUrl(MAP_TYPE as MapType, mapStyle), [mapStyle]);

    const lastLocation: StoredLocation | null = history.length > 0 ? history[history.length - 1] : null;
    const mapCenter: [number, number] = lastLocation != null && lastLocation.location != null ?
        [lastLocation.location.lat, lastLocation.location.lon] : DEFAULT_CENTER;

    // Update current time periodically
    useEffect(() => {
        const interval = setInterval(() => {
            setCurrentTime(() => Date.now());
        }, 60000);

        return () => clearInterval(interval);
    }, []);

    const pointData = useMemo(() => {
        return {
            type: "FeatureCollection",
            features: history
                .filter(h => !!h.location)
                .slice(-POINT_LIMIT)
                .map((h, index, locations) => ({
                    type: "Feature",
                    properties: {
                        lat: h.location!.lat,
                        lon: h.location!.lon,
                        accuracy: h.location!.accuracy,
                        time: h.timestamp,
                        index: index,
                        isLatest: index === locations.length - 1
                    },
                    geometry: {
                        type: "Point",
                        coordinates: [h.location!.lon, h.location!.lat]
                    }
                }))
        };
    }, [history]);

    const lineData = useMemo(() => {
        return {
            type: "Feature",
            properties: {},
            geometry: {
                type: "LineString",
                coordinates: history
                    .filter(h => !!h.location)
                    .map(h => [h.location!.lon, h.location!.lat])
            }
        };
    }, [history]);

    // Calculate cutoff ratio for faded-out lines
    let cutoffRatio = 0;
    if (history.length > 0) {
        const startTime = history[0].timestamp;
        const endTime = history[history.length - 1].timestamp;
        const totalDuration = endTime - startTime;
        const twentyFourHoursAgo = currentTime - (24 * 60 * 60 * 1000);
        cutoffRatio = totalDuration > 0 ? (twentyFourHoursAgo - startTime) / totalDuration : 0;
    }

    // Draw an accuracy circle for the last point
    const accuracyCircle = useMemo(() => {
        const lastHistory = history[history.length - 1];
        if (!lastHistory?.location) return null;
        const {lat, lon, accuracy} = lastHistory.location;

        return circlePoint(lat, lon, accuracy);
    }, [history]);

    return (
        <main className={styles.mapArea}>

            {/* Map settings */}
            <div className={styles.mapSettings}>
                <Segmented
                    vertical
                    className={styles.segmentedControl}
                    options={mapStyleOptions(MAP_TYPE as MapType)}
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
                style={{width: "100%", height: "100%"}}
                mapStyle={mapUrl as any}
                interactiveLayerIds={["history-points"]}
                cursor={cursor}
                onMouseEnter={() => setCursor("pointer")}
                onMouseLeave={() => setCursor("")}
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
                <Source type="geojson" data={lineData as any} lineMetrics={true}>
                    <Layer {...lineStyle(cutoffRatio) as any} />
                </Source>

                {/* Points */}
                <Source type="geojson" data={pointData as any}>
                    <Layer {...pointStyle as any} />
                </Source>

                {accuracyCircle && (
                    <Source id="accuracy-zone" type="geojson" data={accuracyCircle}>
                        <Layer {...accuracyCircleStyle as any} />
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
                            <strong>Time:</strong> {format(new Date(popupInfo.properties.time), "yyyy-MM-dd HH:mm:ss")}
                        </div>
                    </Popup>
                )}

            </Map>
        </main>
    );
}