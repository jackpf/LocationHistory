#!/bin/sh

set -e

## SSL certs

CERTS_DIR="/data/certs"

mkdir -p "${CERTS_DIR}"
if [ ! -f "${CERTS_DIR}/server.key" ] || [ ! -f "${CERTS_DIR}/server.pem" ]; then
  echo "Generating SSL keys..."

  /app/bin/generate-ssl-cert.sh "${CERTS_DIR}"
else
  echo "SSL keys exist"
fi

## App

JAVA_ARGS=""

if [ -n "$BEACON_PORT" ]; then
    JAVA_ARGS="$JAVA_ARGS --beacon-port $BEACON_PORT"
fi

if [ -n "$ADMIN_PORT" ]; then
    JAVA_ARGS="$JAVA_ARGS --admin-port $ADMIN_PORT"
fi

if [ -n "$ADMIN_PASSWORD" ]; then
    JAVA_ARGS="$JAVA_ARGS --admin-password $ADMIN_PASSWORD"
fi

if [ -n "$STORAGE_TYPE" ]; then
    JAVA_ARGS="$JAVA_ARGS --storage-type $STORAGE_TYPE"
fi

if [ -n "$SSL_CERTS_DIR" ]; then
    JAVA_ARGS="$JAVA_ARGS --ssl-certs-directory $SSL_CERTS_DIR"
else
    JAVA_ARGS="$JAVA_ARGS --ssl-certs-directory /data/certs"
fi

if [ -n "$DATA_DIR" ]; then
    JAVA_ARGS="$JAVA_ARGS --data-directory $DATA_DIR"
else
    JAVA_ARGS="$JAVA_ARGS --data-directory /data"
fi

if [ -n "$ENRICHERS" ]; then
    JAVA_ARGS="$JAVA_ARGS --enrichers $ENRICHERS"
fi

exec java -jar /app/app.jar $JAVA_ARGS "$@"