# LocationHistory UI

The web-based visualization dashboard for your location data.

## Features
* **Interactive Maps:** powered by [MapLibre](https://maplibre.org/) / [MapTiler](https://www.maptiler.com/) / [OpenStreetMap](https://www.openstreetmap.org/) / [Leaflet](https://leafletjs.com/).
* **Timeline View:** view your location history for multiple devices.
* **Device Management:** view status of connected trackers.

## 🛠 Tech Stack
* **Language:** TypeScript
* **Framework:** React
* **Bundler:** Vite
* **Styling:** Tailwind CSS / CSS Modules

## 🚀 Development

### Local build

The UI consists of 2 components: the React app and a gRPC proxy,
since browsers do not natively support gRPC communication.

Both need to be running for the UI to work properly & communicate with the backend.

### The proxy

```bash
make run-proxy # run locally
make package-proxy # package docker container
```

### The UI

```bash
make run # run locally
make package # package docker container
```