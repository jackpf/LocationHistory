import React, {useEffect, useState} from 'react'
import {Circle, CircleMarker, MapContainer, Polyline, Popup, TileLayer} from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import './App.css'
import {Metadata} from 'nice-grpc-web'
import type {LocationPoint} from './model/location-point'
import {adminClient} from './grpc/admin-client'

function App() {
    // 2. State is now typed: useState<LocationPoint[]>
    const [history, setHistory] = useState<LocationPoint[]>([]);

    useEffect(() => {
        const fetchData = async () => {
            console.log("Fetching data");
            // Looks like a normal async function!
            // const devicesResponse = await adminClient.listDevices({}, {
            //     metadata: Metadata({ 'authorization': 'Bearer admin' })
            // });
            // console.log(devicesResponse);
            const locationsResponse = await adminClient.listLocations({
                device: {
                    id: "85c51331-4212-48cb-bb88-7949533b9217",
                    publicKey: "xxx" // Notice: public_key becomes publicKey
                }
            });
            console.log(locationsResponse);

            const data: LocationPoint[] = locationsResponse.locations.map((item, index) => ({
                id: index,                 // React needs an ID (use index if data has no ID)
                lat: item.lat,             // Pass through
                lng: item.lon,             // ⚠️ Rename 'lon' to 'lng' for Leaflet
                accuracy: item.accuracy,   // Pass through
                timestamp: new Date().toLocaleTimeString() // Generate a placeholder time
            }));

            console.log("Data:");
            console.log(data);

            setHistory(data);
        };
        fetchData();
    }, []);

    // 3. Transform data for the Polyline
    // The Polyline component expects an array of tuples: [[lat, lng], [lat, lng]]
    const polylinePositions = history.map(loc => [loc.lat, loc.lng] as [number, number]);

    // Handle "Loading" or "Empty" states safely
    if (history.length === 0) return <div>Loading history...</div>;

    // Center the map on the most recent location (last item in list)
    const lastLocation = history[history.length - 1];

    return (
        <div style={{ height: "100vh", width: "100vw" }}>
            <MapContainer
                center={[lastLocation.lat, lastLocation.lng]}
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
                            center={[loc.lat, loc.lng]}
                            radius={loc.accuracy}
                            pathOptions={{ color: 'blue', fillColor: 'blue', fillOpacity: 0.1, stroke: false }}
                        />

                        {/* 2. The Center Dot (Fixed Pixels) */}
                        {/* This stays nice and visible even if accuracy is huge */}
                        <CircleMarker
                            center={[loc.lat, loc.lng]}
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