import {MAPTILER_API_KEY} from "../config/config.ts";
import {GlobalOutlined, MoonFilled, SunOutlined} from "@ant-design/icons";

export const DEFAULT_CENTER: [number, number] = [40, 0];
export const DEFAULT_ZOOM = 2;
export const DEFAULT_ZOOM_IN = 15;

export const POINT_LIMIT = 25;

export enum MapType {
    Maptiler = "maptiler",
    OSM = "openstreetmap"
}

export enum MapStyle {
    Light = "LIGHT",
    Dark = "DARK",
    Satellite = "SATELLITE",
}

export const mapStyleOptions = (mapType: MapType) => {
    const options = [
        {value: MapStyle.Light, label: <SunOutlined/>},
        {value: MapStyle.Dark, label: <MoonFilled/>},
    ];

    // Satellite only support for maptiler
    if (mapType === MapType.Maptiler) options.push({value: MapStyle.Satellite, label: <GlobalOutlined/>})

    return options;
}

export const getMapUrl = (mapType: MapType, style: MapStyle) => {
    switch (mapType) {
        case MapType.Maptiler:
            return getMapTilerUrl(style);
        case MapType.OSM:
            return getOSMUrl(style);
        default:
            alert(`Invalid map type ${mapType}, must be one of: ${Object.values(MapType)}`);
            return null;
    }
}

export const getMapTilerUrl = (style: MapStyle) => {
    if (!MAPTILER_API_KEY) {
        alert("MAPTILER_API_KEY must be set to use maptiler");
        return null;
    }

    let mapTilerStyle;
    if (style === MapStyle.Light) mapTilerStyle = "streets-v2";
    else if (style === MapStyle.Dark) mapTilerStyle = "base-v4-dark";
    else if (style === MapStyle.Satellite) mapTilerStyle = "satellite";

    return `https://api.maptiler.com/maps/${mapTilerStyle}/style.json?key=${MAPTILER_API_KEY}`;
}

export const getOSMUrl = (style: MapStyle) => {
    let source;
    if (style === MapStyle.Light) source = "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
    else if (style === MapStyle.Dark) source = "https://basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png";

    return {
        version: 8,
        sources: {
            "osm-tiles": {
                type: "raster",
                tiles: [source],
                tileSize: 256,
                attribution: "&copy; OpenStreetMap Contributors"
            }
        },
        layers: [
            {
                id: "osm-layer",
                type: "raster",
                source: "osm-tiles",
                minzoom: 0,
                maxzoom: 19
            }
        ]
    };
}