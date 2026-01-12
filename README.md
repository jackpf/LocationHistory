# LocationHistory

Build status:

![Build Status](https://github.com/jackpf/LocationHistory/actions/workflows/main-build.yml/badge.svg)
![Latest Version](https://img.shields.io/github/v/release/jackpf/LocationHistory)

[![Docker Hub](https://img.shields.io/docker/v/jackpfarrelly/location-history-server?label=Server%20Image)](https://hub.docker.com/r/jackpfarrelly/location-history-server)
[![Docker Hub](https://img.shields.io/docker/v/jackpfarrelly/location-history-ui?label=UI%20Image)](https://hub.docker.com/r/jackpfarrelly/location-history-ui)
[![Docker Hub](https://img.shields.io/docker/v/jackpfarrelly/location-history-ui-proxy?label=UI%20Proxy%20Image)](https://hub.docker.com/r/jackpfarrelly/location-history-ui-proxy)

[![Docker Hub](https://img.shields.io/docker/pulls/jackpfarrelly/location-history-server)](https://hub.docker.com/r/jackpfarrelly/location-history-server)

> **Private, self-hosted, and secure location tracking.**

LocationHistory is an open-source alternative to Google's "Find Your Phone"
(and essentially also Google Timeline).
It allows you to track your device's location,
store the data on your own server,
and visualize your history without sharing your data with third parties.

- Designed with privacy as the core feature, LocationHistory ensures that your movement data remains your own.
- Completely [FOSS](https://en.wikipedia.org/wiki/Free_and_open-source_software) - only uses open source libraries.
- (Of course) completely free of external tracking or adverts.

| ![Web UI](./docs/img/ui-screenshot.png) | ![Android Client](./docs/img/android-screenshot.png) |
|:---------------------------------------:|:----------------------------------------------------:|

## How To Use It

1. Get the Android app from the [releases](https://github.com/jackpf/LocationHistory/releases) page (F-Droid releases hopefully coming soon!)
2. To host your LocationHistory server via [Docker Compose](https://docs.docker.com/compose/), you just need 2 files:
   1. Copy the example [docker-compose.yml](./examples/docker-compose.yml) (you shouldn't need to change anything here)
   2. Copy the example [.env](./examples/.env) file into the same directory and customise the variables according the explanations
3. Run with `docker compose up`
4. You should now have a working location history server! Check connectivity via the app.

## Security

Client communication uses auto-generated self-signed SSL certificates,
with a TOFU (trust-on-first-use) model.

⚠ **By default the admin endpoint is NOT using SSL, in order to
enable a user-friendly setup (i.e. avoiding self-signed certificate headaches).**

Therefore, unless you configure SSL for the UI,
logging into the backend should only be done on your local network
& not exposed externally (otherwise your admin password could be exposed)!

Optional SSL setup is coming soon.

---

## Project Structure

This repository is a monorepo containing all components:

| Component                        | Description                                                                  |
|:---------------------------------|:-----------------------------------------------------------------------------|
| [**Server**](./server/README.md) | The gRPC backend that ingests and stores location data, also serving the UI  |
| [**Client**](./client/README.md) | Native Android application for battery-friendly background location syncing  |
| [**UI**](./ui/README.md)         | Modern TypeScript-based web dashboard for visualizing history.               |
| [**Shared**](./shared/README.md) | Shared protocol definitions and logic.                                       |

## Getting Started

### Prerequisites
- **Java 17+** (Required for Server & Android builds)
- **Node.js 18+** & **npm** (Required for UI)
- **sbt** (Scala Build Tool)
- **Docker** (For packaging and running locally)

### Build & Run
We use a root `Makefile` to orchestrate builds across the entire stack.

#### Running locally

This is a quick way to run everything locally.

See each individual component README for instructions
how to customise the environment.

```bash
# Note that you'll need to run each of these commands in individual shells
# Run the server
RUN_ARGS='--admin-password=password --storage-type=in_memory' make -C server run
# Run the UI proxy
make -C ui run-proxy
# Run the UI
make -C ui init-local-env run
```

#### Running locally via Docker

If you want to test the full Docker stack, you can run like so:

1. Make a copy of the example [docker-compose.yml](./examples/docker-compose.yml) and [.env file](./examples/.env)
2. Customise it to your liking (it might just work out of the box though)
3. Package everything locally: `make package-all`
4. Run `docker compose up`

Your components should be running on ports specified in the `.env` file.

## License
This project is licensed under the [GPLv3 License](https://www.gnu.org/licenses/gpl-3.0.en.html).