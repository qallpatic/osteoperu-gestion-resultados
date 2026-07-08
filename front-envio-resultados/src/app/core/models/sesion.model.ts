/** Roles del sistema (deben coincidir con los del backend). */
export type Rol = 'ADMIN' | 'RESULTADOS' | 'ADMISION' | 'PACIENTE' | 'CALLCENTER';

/** Datos de la sesión activa del usuario. */
export interface Sesion {
  usuario: string;
  nombre: string;
  roles: string[];
}

/** Subítem (submenú) de un ítem del menú lateral. */
export interface SubMenuItem {
  label: string;
  icon: string;
  route: string;
}

/** Ítem del menú lateral. `roles` vacío = visible para cualquier usuario autenticado. */
export interface MenuItem {
  label: string;
  icon: string;
  route: string;
  roles: Rol[];
  /** false = módulo aún no implementado (se muestra atenuado con badge "Pronto"). */
  disponible: boolean;
  /** Submenús que se despliegan bajo el ítem (p. ej. Resultados → Cargar/Consultar/Logs). */
  children?: SubMenuItem[];
}
