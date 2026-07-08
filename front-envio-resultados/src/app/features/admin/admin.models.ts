/** Modelos del módulo de administración de usuarios. */

export interface RolResponse {
  id: number;
  nombre: string;
  descripcion: string;
}

export interface UsuarioResponse {
  id: number;
  usuario: string;
  nombre: string | null;
  apellido: string | null;
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
  nombre?: string;
  apellido?: string;
  correo?: string;
  dni?: string;
  password: string;
  rolesIds: number[];
}

export interface UsuarioUpdateRequest {
  nombre?: string;
  apellido?: string;
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
