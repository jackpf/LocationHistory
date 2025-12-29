FROM debian:bullseye-slim

RUN apt-get update && \
    apt-get install -y wget unzip ca-certificates && \
    rm -rf /var/lib/apt/lists/*

RUN wget -qO /tmp/proxy.zip \
    https://github.com/improbable-eng/grpc-web/releases/download/v0.15.0/grpcwebproxy-v0.15.0-linux-x86_64.zip && \
    unzip -p /tmp/proxy.zip > /usr/local/bin/grpcwebproxy && \
    chmod +x /usr/local/bin/grpcwebproxy && \
    rm /tmp/proxy.zip

ENTRYPOINT ["grpcwebproxy"]