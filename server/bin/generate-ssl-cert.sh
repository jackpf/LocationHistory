#!/bin/bash

set -euf -o pipefail

if [ -z "$1" ]; then
  echo "Supply an out dir argument"
  exit 1
fi
OUT_DIR="$1"

openssl req -x509 -newkey rsa:4096 -nodes \
  -keyout "${OUT_DIR}/server.key" \
  -out "${OUT_DIR}/server.crt" \
  -days 3650 \
  -subj "/CN=LocationServer" \
  -addext "subjectAltName = DNS:localhost,IP:127.0.0.1,IP:10.0.2.2,DNS:nuc" # TODO This should be dynamic based on server name

openssl pkcs8 -topk8 -nocrypt -in "${OUT_DIR}/server.key" -out "${OUT_DIR}/server.pem"