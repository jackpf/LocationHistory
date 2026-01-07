FROM eclipse-temurin:17-jre

WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
RUN curl -Lo /bin/grpc_health_probe https://github.com/grpc-ecosystem/grpc-health-probe/releases/download/v0.4.19/grpc_health_probe-linux-amd64 && \
    chmod +x /bin/grpc_health_probe

RUN mkdir -p /data

COPY target/scala-3.7.0/server-assembly-0.1.0-SNAPSHOT.jar app.jar
COPY bin/entrypoint.sh entrypoint.sh

RUN mkdir -p ./bin
COPY bin/generate-ssl-cert.sh ./bin

HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
    CMD (/bin/grpc_health_probe -addr=:8080 -tls -tls-ca-cert=/data/certs/server.crt -tls-server-name=${SERVER_NAME} \
    && /bin/grpc_health_probe -addr=:8081) || exit 1

ENTRYPOINT ["/app/entrypoint.sh"]