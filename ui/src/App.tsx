import React, { useState, useEffect } from 'react'
import { MapContainer, TileLayer, Polyline, Circle, CircleMarker, Popup } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import './App.css'

// 1. Define your types (Scala: case class LocationResponse(...))
interface LocationPoint {
    id: number;
    lat: number;
    lon: number;
    accuracy: number;
    timestamp: string;
}

function App() {
    // 2. State is now typed: useState<LocationPoint[]>
    const [history, setHistory] = useState<LocationPoint[]>([]);

    useEffect(() => {
        // Mock fetching data
        const data: LocationPoint[] = [
            { id: 1, lat: 51.505, lon: -0.09, accuracy: 25, timestamp: "10:00 AM" },
            { id: 2, lat: 51.51,  lon: -0.1, accuracy: 50,  timestamp: "10:05 AM" },
            { id: 3, lat: 51.51,  lon: -0.12, accuracy: 200, timestamp: "10:10 AM" },
        ];
        setHistory(data);
    }, []);

    // 3. Transform data for the Polyline
    // The Polyline component expects an array of tuples: [[lat, lng], [lat, lng]]
    const polylinePositions = history.map(loc => [loc.lat, loc.lon] as [number, number]);

    // Handle "Loading" or "Empty" states safely
    if (history.length === 0) return <div>Loading history...</div>;

    // Center the map on the most recent location (last item in list)
    const lastLocation = history[history.length - 1];

    return (
        <div style={{ height: "100vh", width: "100vw" }}>
            <MapContainer
                center={[lastLocation.lat, lastLocation.lon]}
                zoom={13}
                style={{ height: "100%", width: "100%" }}
            >
                <TileLayer
                    attribution='&copy; OpenStreetMap contributors'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />

                {/* THE ROUTE (Polyline)
            pathOptions={{ color: 'blue' }} makes the line blue
        */}
                <Polyline positions={polylinePositions} pathOptions={{ color: 'blue' }} />

                {/* OPTIONAL: Draw dots at each recorded point
            Using CircleMarker is much cleaner than the big default pin
        */}
                {history.map(loc => (
                    <React.Fragment key={loc.id}>
                        {/* 1. The Accuracy Halo (Real World Meters) */}
                        {/* We use a light fill and no border stroke to look like a "zone" */}
                        <Circle
                            center={[loc.lat, loc.lon]}
                            radius={loc.accuracy}
                            pathOptions={{ color: 'blue', fillColor: 'blue', fillOpacity: 0.1, stroke: false }}
                        />

                        {/* 2. The Center Dot (Fixed Pixels) */}
                        {/* This stays nice and visible even if accuracy is huge */}
                        <CircleMarker
                            center={[loc.lat, loc.lon]}
                            radius={4} // Fixed 4px dot
                            pathOptions={{ color: 'white', fillColor: 'blue', fillOpacity: 1, weight: 2 }}
                        >
                            <Popup>
                                <strong>Time:</strong> {loc.timestamp}<br/>
                                <strong>Accuracy:</strong> {loc.accuracy} meters
                            </Popup>
                        </CircleMarker>
                    </React.Fragment>
                ))}

            </MapContainer>
        </div>
    )
}

export default App