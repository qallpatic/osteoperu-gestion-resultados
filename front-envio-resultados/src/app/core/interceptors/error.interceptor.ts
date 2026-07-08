import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';

import { AuthService } from '../auth/auth.service';

/**
 * Ante un 401 (token expirado) intenta un único refresh y reintenta la petición.
 * Si el refresh falla, limpia la sesión y redirige al login.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const esAuth = req.url.includes('/api/auth/');
      if (error.status === 401 && !esAuth && auth.getRefreshToken()) {
        return auth.refresh().pipe(
          switchMap((token) =>
            next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })),
          ),
          catchError((e) => {
            auth.limpiarSesion();
            router.navigate(['/login']);
            return throwError(() => e);
          }),
        );
      }
      return throwError(() => error);
    }),
  );
};
