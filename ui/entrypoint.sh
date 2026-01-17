#!/bin/sh

set -e

CONFIG_FILE="/usr/share/nginx/html/config.js"

echo "Generating runtime config..."

if [ -n "${PROXY_URL}" ]; then # Allow defining the full proxy URL, e.g. http://server:1234
  RESOLVED_PROXY_URL="\"${PROXY_URL}\""
elif [ -n "${PROXY_PORT}" ]; then # Allow defining only the port if running on the same host, e.g. 1234
  RESOLVED_PROXY_URL="\`\${window.location.protocol}//\${window.location.hostname}:${PROXY_PORT}\`"
else
  echo "PROXY_URL or PROXY_PORT must be set" >&2
  exit 1
fi

cat <<EOF > "${CONFIG_FILE}"
window.APP_CONFIG = {
  PROXY_URL: ${RESOLVED_PROXY_URL},
  MAP_TYPE: "${MAP_TYPE:-}",
  MAPTILER_API_KEY: "${MAPTILER_API_KEY:-}"
};
EOF

exec "$@"