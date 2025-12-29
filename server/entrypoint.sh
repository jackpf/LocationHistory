#!/bin/sh

set -e

JAVA_ARGS=""

if [ -n "$LISTEN_PORT" ]; then
    JAVA_ARGS="$JAVA_ARGS --listen-port $LISTEN_PORT"
fi

if [ -n "$ADMIN_PASSWORD" ]; then
    JAVA_ARGS="$JAVA_ARGS --admin-password $ADMIN_PASSWORD"
fi

exec java -jar /app/app.jar $JAVA_ARGS "$@"