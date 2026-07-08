import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, finalize, map, shareReplay, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { RespuestaApi, TokenResponse } from '../models/token-response.model';
import { Sesion } from '../models/sesion.model';

const ACCESS_KEY = 'osteo_access';
const REFRESH_KEY = 'osteo_refresh';
const SESSION_KEY = 'osteo_sesion';

/**
 * Autenticación real contra /api/auth (JWT). Mantiene la sesión en signals y
 * persiste los tokens en localStorage. El token se adjunta a cada request
 * mediante el interceptor; el refresh se gestiona con single-flight.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/auth`;

  private readonly _sesion = signal<Sesion | null>(this.leerSesion());

  readonly sesion = this._sesion.asReadonly();
  readonly autenticado = computed(() => this._sesion() !== null);
  readonly nombre = computed(() => this._sesion()?.nombre ?? '');
  readonly roles = computed(() => this._sesion()?.roles ?? []);
  readonly rolPrincipal = computed(() => this._sesion()?.roles?.[0] ?? '');

  /** Observable compartido mientras hay un refresh en curso (single-flight). */
  private refrescando$?: Observable<string>;

  login(usuario: string, contrasena: string): Observable<void> {
    return this.http
      .post<RespuestaApi<TokenResponse>>(`${this.base}/login`, { usuario, contrasena })
      .pipe(map((r) => this.guardar(r.data)));
  }

  logout(): void {
    const refreshToken = this.getRefreshToken();
    if (refreshToken) {
      // Revocación best-effort en el backend; no bloquea el cierre local.
      this.http.post(`${this.base}/logout`, { refreshToken }).subscribe({ error: () => {} });
    }
    this.limpiarSesion();
  }

  refresh(): Observable<string> {
    if (this.refrescando$) {
      return this.refrescando$;
    }
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No hay refresh token disponible.'));
    }
    this.refrescando$ = this.http
      .post<RespuestaApi<TokenResponse>>(`${this.base}/refresh`, { refreshToken })
      .pipe(
        map((r) => {
          this.guardar(r.data);
          return r.data.accessToken;
        }),
        shareReplay(1),
        finalize(() => (this.refrescando$ = undefined)),
      );
    return this.refrescando$;
  }

  getAccessToken(): string | null {
    return localStorage.getItem(ACCESS_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  }

  tieneAlgunRol(roles: string[]): boolean {
    if (!roles.length) {
      return true;
    }
    const propios = this.roles();
    return roles.some((r) => propios.includes(r));
  }

  limpiarSesion(): void {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(SESSION_KEY);
    this._sesion.set(null);
  }

  private guardar(token: TokenResponse): void {
    localStorage.setItem(ACCESS_KEY, token.accessToken);
    localStorage.setItem(REFRESH_KEY, token.refreshToken);
    const sesion: Sesion = {
      usuario: token.usuario,
      nombre: token.nombre || token.usuario,
      roles: token.roles ?? [],
    };
    localStorage.setItem(SESSION_KEY, JSON.stringify(sesion));
    this._sesion.set(sesion);
  }

  private leerSesion(): Sesion | null {
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw || !localStorage.getItem(ACCESS_KEY)) {
      return null;
    }
    try {
      return JSON.parse(raw) as Sesion;
    } catch {
      return null;
    }
  }
}
