# ---------- build (Angular) ----------
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# Build de produccion (usa src/environments/environment.ts)
RUN npm run build -- --configuration production

# ---------- runtime (Nginx) ----------
FROM nginx:1.27-alpine
# SPA: config con fallback a index.html para las rutas de Angular
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/front-envio-resultados/browser /usr/share/nginx/html

EXPOSE 80
