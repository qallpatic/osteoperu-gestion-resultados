/**
 * Configuracion de PRODUCCION (build por defecto).
 * El front se sirve con Nginx y las llamadas a /back-envio-resultados-api
 * se redirigen (proxy) al backend en el mismo origen -> sin CORS ni IPs fijas,
 * y compatible con dominio + HTTPS. Auth por JWT (Bearer).
 */
export const environment = {
  production: true,
  apiBaseUrl: '/osteoperu-api-resultados',
};
