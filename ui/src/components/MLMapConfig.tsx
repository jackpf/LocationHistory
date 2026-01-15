import {MAP_TYPE, MAPTILER_API_KEY} from "../config/config.ts";
import {GlobalOutlined, MoonFilled, SunOutlined} from "@ant-design/icons";

export const DEFAULT_CENTER: [number, number] = [40, 0];
export const DEFAULT_ZOOM = 2;
export const DEFAULT_ZOOM_IN = 15;
// TODO Refactor to constants and convert to correct style in url functions
// TODO Dynamic options based on map type (satellite isn't available for OSM)
export const MAP_STYLE_OPTIONS = [
    {value: "streets-v2", label: <SunOutlined/>},
    {value: "base-v4-dark", label: <MoonFilled/>},
    {value: "satellite", label: <GlobalOutlined/>},
];
export const POINT_LIMIT = 25;

export const getMapUrl = (mapType: string, style: string) => {
    switch (mapType) {
        case "maptiler":
            return getMapTilerUrl(style);
        case "openstreetmaps":
            return getOSMUrl(style);
        default:
            alert("Invalid map type " + MAP_TYPE + ", must be one of: [maptiler, openstreetmaps]");
            return null;
    }
}

export const getMapTilerUrl = (style: string) => {
    if (!MAPTILER_API_KEY) {
        alert("MAPTILER_API_KEY must be set to use maptiler");
        return null;
    }

    return `https://api.maptiler.com/maps/${style}/style.json?key=${MAPTILER_API_KEY}`;
}

export const getOSMUrl = (style: string) => {
    if (style === "base-v4-dark") { // TODO Refactor this check
        return {
            version: 8,
            sources: {
                'dark-raster-tiles': {
                    type: 'raster',
                    tiles: ['https://basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png'],
                    tileSize: 256,
                    attribution: "&copy; OpenStreetMap Contributors"
                }
            },
            layers: [
                {
                    id: 'dark-tiles',
                    type: 'raster',
                    source: 'dark-raster-tiles',
                    minzoom: 0,
                    maxzoom: 19
                }
            ]
        };
    }

    return {
        version: 8,
        sources: {
            "osm-tiles": {
                type: "raster",
                tiles: ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
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