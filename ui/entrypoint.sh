#!/bin/sh

set -e

CONFIG_FILE="/usr/share/nginx/html/config.js"

echo "Generating runtime config..."

cat <<EOF > $CONFIG_FILE
window.APP_CONFIG = {
  PROXY_URL: "${PROXY_URL:-}",
  MAPTILER_API_KEY: "${MAPTILER_API_KEY:-}"
};
EOF

exec "$@"