#!/bin/bash

set -euf -o pipefail

if [ -z "${1:-}" ]; then
  echo "Supply an out dir argument"
  exit 1
fi
OUT_DIR="${1}"

if [ -z "${SERVER_NAME}" ]; then
  echo "\$SERVER_NAME must be set"
  exit 2
fi

# Defaults
SAN_LIST="DNS:localhost,IP:127.0.0.1"

# Allow multiple comma-separated values
IFS=',' read -ra ADDR <<< "${SERVER_NAME},${ALT_SERVER_NAMES:-}"

for ENTRY in "${ADDR[@]}"; do
    ENTRY=$(echo "$ENTRY" | xargs)
    if [[ -z "$ENTRY" ]]; then
        continue
    fi

    if [[ "$ENTRY" =~ : ]]; then # IPv6
        if [[ "$ENTRY" =~ [^0-9a-fA-F:] ]]; then
            echo "Detected IPv6 with invalid characters"
            exit 3
        fi
        PREFIX="IP"
    elif [[ "$ENTRY" =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; then # IPv4
        PREFIX="IP"
    else # Hostname
        PREFIX="DNS"
    fi

    SAN_LIST="${SAN_LIST},${PREFIX}:${ENTRY}"
done

echo "Generating cert with SANs: $SAN_LIST"

openssl req -x509 -newkey rsa:4096 -nodes \
  -keyout "${OUT_DIR}/server.key" \
  -out "${OUT_DIR}/server.crt" \
  -days 3650 \
  -subj "/CN=LocationServer" \
  -addext "subjectAltName = ${SAN_LIST}"

openssl pkcs8 -topk8 -nocrypt -in "${OUT_DIR}/server.key" -out "${OUT_DIR}/server.pem"