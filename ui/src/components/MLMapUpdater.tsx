import type {StoredLocation} from "../gen/common.ts";
import {useMap} from "react-map-gl/maplibre";
import React, {useEffect} from "react";
import type {LngLatLike} from "maplibre-gl";
import {DEFAULT_CENTER, DEFAULT_ZOOM_IN} from "./MLMapConfig.tsx";

export function MapUpdater({center, selectedId, history, forceRecenter, setForceRecenter}: {
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