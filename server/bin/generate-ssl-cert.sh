#!/bin/bash

set -euf -o pipefail

if [ -z "$1" ]; then
  echo "Supply an out dir argument"
  exit 1
fi
OUT_DIR="$1"

if [ -z "$SERVER_NAME" ]; then
  echo "$$SERVER_NAME must be set"
  exit 2
fi

if [[ "$SERVER_NAME" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    SAN_PREFIX="IP"
else
    SAN_PREFIX="DNS"
fi

openssl req -x509 -newkey rsa:4096 -nodes \
  -keyout "${OUT_DIR}/server.key" \
  -out "${OUT_DIR}/server.crt" \
  -days 3650 \
  -subj "/CN=LocationServer" \
  -addext "subjectAltName = DNS:localhost,IP:127.0.0.1,${SAN_PREFIX}:${$SERVER_NAME}"

openssl pkcs8 -topk8 -nocrypt -in "${OUT_DIR}/server.key" -out "${OUT_DIR}/server.pem"