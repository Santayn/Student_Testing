#!/bin/sh
set -eu

BACKEND_HOST="${BACKEND_HOST:-backend}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
PROXY_SEND_TIMEOUT="${PROXY_SEND_TIMEOUT:-300s}"
PROXY_READ_TIMEOUT="${PROXY_READ_TIMEOUT:-300s}"

sed \
  -e "s/__BACKEND_HOST__/${BACKEND_HOST}/g" \
  -e "s/__BACKEND_PORT__/${BACKEND_PORT}/g" \
  -e "s/__PROXY_SEND_TIMEOUT__/${PROXY_SEND_TIMEOUT}/g" \
  -e "s/__PROXY_READ_TIMEOUT__/${PROXY_READ_TIMEOUT}/g" \
  /etc/nginx/conf.d/default.conf.template \
  > /etc/nginx/conf.d/default.conf

echo "Frontend proxy target: http://${BACKEND_HOST}:${BACKEND_PORT}"
