# LocationHistory Server

The backbone of the LocationHistory project.
This Scala application handles high-throughput ingestion of
location points from mobile clients and provides an API for the web frontend.

It consists of 2 services which run on separate ports,
with separate authentications systems.

- Beacon service: handles client (Android app) communication & location pings
- Admin service: serves the web UI

## Tech Stack
* **Language:** Scala 3
* **Framework:** gRPC
* **Storage:** In-memory or SQLite
* **Build Tool:** sbt

## Build & Run

### Local build

```bash
make lint test integration-test package
```

### Running Locally
The server can be run locally via:

```bash
# First of all, generate a self-signed SSL certificate (required)
make generate-local-cert
# Then run the server
RUN_ARGS='--admin-password=password --storage-type=in_memory' make run # via sbt
```
