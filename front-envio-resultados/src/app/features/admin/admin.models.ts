/** Modelos del módulo de administración de usuarios. */

export interface RolResponse {
  id: number;
  nombre: string;
  descripcion: string;
}

export interface UsuarioResponse {
  id: number;
  usuario: string;
  /** Nombre canónico "APELLIDO_P APELLIDO_M, NOMBRES" — lo que se muestra y se imprime. */
  nombre: string | null;
  nombres: string | null;
  apellidoPaterno: string | null;
  apellidoMaterno: string | null;
  correo: string | null;
  dni: string | null;
  estado: boolean;
  roles: string[];
  fechaCreacion: string;
  fechaModificacion: string | null;
}

export interface PaginaResponse<T> {
  contenido: T[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
  ultima: boolean;
}

export interface UsuarioRequest {
  usuario: string;
  /** Nombre canónico "APELLIDO_P APELLIDO_M, NOMBRES". El backend lo segmenta. */
  nombre?: string;
  correo?: string;
  dni?: string;
  password: string;
  rolesIds: number[];
}

export interface UsuarioUpdateRequest {
  nombre?: string;
  correo?: string;
  dni?: string;
}

export interface ListarUsuariosParams {
  pagina: number;
  tamano: number;
  buscar?: string;
  rol?: string;
  estado?: boolean;
  ordenarPor?: string;
  direccion?: string;
}