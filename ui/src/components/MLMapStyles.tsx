import circle from "@turf/circle";

export const circlePoint = (lat: number, lon: number, accuracy: number) => {
    if (!accuracy || accuracy < 1) return null;

    return circle([lon, lat], accuracy, {
        steps: 64,
        units: "meters"
    });
};

export const pointStyle = {
    id: "history-points",
    type: "circle",
    paint: {
        "circle-radius": 4,
        "circle-color": "blue",
        "circle-stroke-color": "white",
        // Only show point outline for latest point
        "circle-stroke-width": [
            "case",
            ["boolean", ["get", "isLatest"], false],
            2,
            0
        ],
    }
};

export const accuracyCircleStyle = {
    id: "accuracy-fill",
    type: "fill",
    paint: {
        "fill-color": "blue",
        "fill-opacity": 0.3,
        "fill-outline-color": "blue"
    }
};

export const lineStyle = (cutoffRatio: number) => {
    cutoffRatio = Math.max(0, Math.min(1, cutoffRatio));

    return {
        id: "route-line",
        type: "line",
        paint: {
            "line-width": 4,
            "line-gradient": [
                "interpolate",
                ["linear"],
                ["line-progress"],

                // From Start (0.0) to Cutoff point -> Faint (Old)
                0, "rgba(0, 0, 255, 0.05)",
                cutoffRatio, "rgba(0, 0, 255, 0.2)",

                // Small transition zone (e.g. +5%) to fade into Solid (New)
                Math.min(1, cutoffRatio + 0.1), "rgba(0, 0, 255, 0.3)",

                // To End (1.0) -> Solid
                1, "rgba(0, 0, 255, 0.3)"
            ]
        }
    }
};