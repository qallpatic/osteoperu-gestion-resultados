/** Envoltura uniforme de respuestas del backend (claves en inglés). */
export interface RespuestaApi<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

/** Respuesta de autenticación del backend (/api/auth/login|refresh). */
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  usuario: string;
  nombre: string;
  roles: string[];
}
