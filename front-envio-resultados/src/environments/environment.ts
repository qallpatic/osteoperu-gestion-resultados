/**
 * Configuracion de PRODUCCION (build por defecto).
 * El front y el backend se despliegan en el servidor 161.132.53.229.
 */
export const environment = {
  production: true,
  // API del backend (Basic Auth). El front se sirve en :80 y llama al backend en :7090.
  apiBaseUrl: 'http://161.132.53.229:7090/back-envio-resultados-api',
  apiUser: 'osteoperu',
  apiPassword: 'Osteoperu@2026',
};
