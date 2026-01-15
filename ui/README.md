# LocationHistory UI

The web-based visualization dashboard for your location data.

The UI currently supports 2 map types (controlled by the `MAP_TYPE` env var):

| Map type                                        | Description                                                                                                                            |
|:------------------------------------------------|:---------------------------------------------------------------------------------------------------------------------------------------|
| [OpenStreetMap](https://www.openstreetmap.org/) | The basic default using raster tiling. Requires no API key.                                                                            |
| [MapTiler](https://www.maptiler.com/)      | Uses vector tiling and looks a lot nicer. Requires a free API key - see [here](https://www.maptiler.com/cloud/pricing/) for more info. |

## Features
* **Interactive Maps:** powered by [MapLibre](https://maplibre.org/) / [MapTiler](https://www.maptiler.com/) / [OpenStreetMap](https://www.openstreetmap.org/).
* **Timeline View:** view your location history for multiple devices.
* **Device Management:** view status of connected trackers.

## Tech Stack
* **Language:** TypeScript
* **Framework:** React
* **Bundler:** Vite
* **Styling:** Tailwind CSS / CSS Modules

## Development

### Local build

The UI consists of 2 components: the React app and a gRPC proxy,
since browsers do not natively support gRPC communication.

Both need to be running for the UI to work properly & communicate with the backend.

### The proxy

```bash
make run-proxy
```

### The UI

You need to set up a couple of variables in a `.env.local` file in order to run locally:

```bash
make init-local-env # creates your .env.local file
```

Or if you're using the maptiler flavour:

```bash
cat <<EOF > .env.local
VITE_PROXY_URL=http://localhost:9123
VITE_MAP_TYPE=maptiler
VITE_MAPTILER_API_KEY=<YOUR_API_KEY>
EOF
```

Then:

```bash
make run
```
