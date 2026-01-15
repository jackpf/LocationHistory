import {MAPTILER_API_KEY} from "../config/config.ts";
import {GlobalOutlined, MoonFilled, SunOutlined} from "@ant-design/icons";

if (!MAPTILER_API_KEY) {
    alert("MAPTILER_API_KEY must be set to use maptiler");
}

export const getMapUrl = (style: string) => {
    return `https://api.maptiler.com/maps/${style}/style.json?key=${MAPTILER_API_KEY}`;
}
export const DEFAULT_CENTER: [number, number] = [40, 0];
export const DEFAULT_ZOOM = 2;
export const DEFAULT_ZOOM_IN = 15;
export const MAP_STYLE_OPTIONS = [
    {value: "streets-v2", label: <SunOutlined/>},
    {value: "base-v4-dark", label: <MoonFilled/>},
    {value: "satellite", label: <GlobalOutlined/>},
];

export const POINT_LIMIT = 25;