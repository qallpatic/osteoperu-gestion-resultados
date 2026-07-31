import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../auth/auth.service';

/** Rutas privadas: sin sesión redirige a /login. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.autenticado() ? true : router.createUrlTree(['/login']);
};

/** Impide ver /login con sesión activa: redirige al inicio. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.autenticado() ? router.createUrlTree(['/']) : true;
};

/** Restringe una ruta a los roles indicados. Uso: `roleGuard(['ADMIN','RESULTADOS'])`. */
export function roleGuard(roles: string[]): CanActivateFn {
	return () => {
		const auth = inject(AuthService);
		const router = inject(Router);
		if (!auth.autenticado()) {
			return router.createUrlTree(['/login']);
		}
		return auth.tieneAlgunRol(roles) ? true : router.createUrlTree(['/']);
	};
}

/**
 * Guarda de /dashboard: deja pasar solo al rol ADMIN. Para los demas roles,
 * redirige al modulo principal segun su perfil (mantiene la convencion del
 * sidebar: "Resultados" para RESULTADOS, "Gestion de usuarios" para ADMISION,
 * "Mis resultados" para PACIENTE). Si el rol no encaja en ninguno conocido,
 * cae al modulo mas cercano (/resultados).
 */
export const dashboardAdminGuard: CanActivateFn = () => {
	const auth = inject(AuthService);
	const router = inject(Router);
	if (!auth.autenticado()) {
		return router.createUrlTree(['/login']);
	}
	if (auth.tieneAlgunRol(['ADMIN'])) {
		return true;
	}
	if (auth.tieneAlgunRol(['RESULTADOS'])) {
		return router.createUrlTree(['/resultados/cargar']);
	}
	if (auth.tieneAlgunRol(['ADMISION'])) {
		return router.createUrlTree(['/gestion-usuarios']);
	}
	if (auth.tieneAlgunRol(['PACIENTE'])) {
		return router.createUrlTree(['/paciente']);
	}
	return router.createUrlTree(['/resultados/cargar']);
};
