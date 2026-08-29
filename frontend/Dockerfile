FROM node:22-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

RUN npm run build


FROM nginx:1.27-alpine AS runtime

COPY --from=build /app/dist /usr/share/nginx/html

COPY docker/nginx.conf.template /etc/nginx/conf.d/default.conf.template
COPY docker/40-render-backend.sh /docker-entrypoint.d/40-render-backend.sh

RUN chmod +x /docker-entrypoint.d/40-render-backend.sh

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1/health >/dev/null 2>&1 || exit 1
