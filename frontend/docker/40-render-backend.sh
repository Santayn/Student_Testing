#!/bin/sh
set -eu

BACKEND_HOST="${BACKEND_HOST:-backend}"
BACKEND_PORT="${BACKEND_PORT:-8080}"

sed \
  -e "s/__BACKEND_HOST__/${BACKEND_HOST}/g" \
  -e "s/__BACKEND_PORT__/${BACKEND_PORT}/g" \
  /etc/nginx/conf.d/default.conf.template \
  > /etc/nginx/conf.d/default.conf

echo "Frontend proxy target: http://${BACKEND_HOST}:${BACKEND_PORT}"
