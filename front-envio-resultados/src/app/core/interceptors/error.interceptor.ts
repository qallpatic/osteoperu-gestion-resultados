import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';

import { AuthService } from '../auth/auth.service';

/**
 * Ante un 401 (access token expirado) intenta un unico refresh + retry.
 * Si el refresh falla, limpia la sesion, redirige al login y avisa al usuario
 * para evitar el bucle "Ocurrio un error al enviar los resultados" que se veia
 * en produccion cuando el refresh token tambien estaba vencido.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const redirigirALogin = (): void => {
    router.navigate(['/login'], {
      queryParams: { motivo: 'sesion-expirada' },
      replaceUrl: true,
    });
  };

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || req.url.includes('/api/auth/')) {
        return throwError(() => error);
      }
      if (!auth.getRefreshToken()) {
        // Sesion muerta sin refresh: login directo.
        auth.limpiarSesion();
        redirigirALogin();
        return throwError(() => error);
      }
      return auth.refresh().pipe(
        switchMap((token) =>
          next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })),
        ),
        catchError(() => {
          auth.limpiarSesion();
          redirigirALogin();
          return throwError(() => error);
        }),
      );
    }),
  );
};
