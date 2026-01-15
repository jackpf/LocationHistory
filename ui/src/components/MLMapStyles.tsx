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
    const COLOR_FADED = "rgba(0, 0, 255, 0.05)";
    const COLOR_TRANSITION = "rgba(0, 0, 255, 0.2)";
    const COLOR_SOLID = "rgba(0, 0, 255, 0.3)";
    const FADE_LENGTH = 0.1;

    let gradientStops;

    if (cutoffRatio <= 0) {
        // Full solid
        gradientStops = [
            0, COLOR_SOLID,
            1, COLOR_SOLID
        ];
    } else if (cutoffRatio >= 1) {
        // Full faded
        gradientStops = [
            0, COLOR_FADED,
            1, COLOR_FADED
        ];
    } else {
        // Faded until cutoff ratio
        gradientStops = [
            0, COLOR_FADED,
            cutoffRatio, COLOR_TRANSITION
        ];

        const transitionEnd = cutoffRatio + FADE_LENGTH;

        // Clamp the transition end
        if (transitionEnd < 1) {
            gradientStops.push(transitionEnd, COLOR_SOLID);
        }
        gradientStops.push(1, COLOR_SOLID);
    }

    return {
        id: "route-line",
        type: "line",
        paint: {
            "line-width": 4,
            "line-gradient": [
                "interpolate",
                ["linear"],
                ["line-progress"],
                ...gradientStops
            ]
        }
    }
};