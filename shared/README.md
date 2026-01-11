# Shared Resources

Contains resources shared between all components (mainly Protobuf definitions).

See the Protobuf definitions [here](./src/main/protobuf).

Protobuf libraries are built & published to local repos, which are then used
in the other components. Therefore, shared components must be built before
any other components is built (this should happen automatically via each
component's build pipeline/Makefile).

## Build & Run

### Build & publish locally

```bash
make publish
```
