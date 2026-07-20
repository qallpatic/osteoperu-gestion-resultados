import { Injectable, OnDestroy, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, finalize, map, shareReplay, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { RespuestaApi, TokenResponse } from '../models/token-response.model';
import { Sesion } from '../models/sesion.model';

const ACCESS_KEY = 'osteo_access';
const REFRESH_KEY = 'osteo_refresh';
const SESSION_KEY = 'osteo_sesion';
/** Margen (ms) antes del vencimiento del access token en que se dispara el refresh
 *  proactivo. 60s permite absorber latencia de red sin golpear el 401. */
const REFRESH_LEAD_MS = 60_000;
/** Delay minimo si el token ya esta vencido al reconstruir la sesion: refrescar
 *  de inmediato en vez de esperar 5s para no exponer una ventana de 401. */
const REFRESH_RECARGA_INMEDIATA_MS = 0;

interface DecodedJwtPayload {
  exp: number;
  sub: string;
  iat: number;
  [key: string]: unknown;
}

/**
 * Autenticacion real contra /api/auth (JWT). Mantiene la sesion en signals y
 * persiste los tokens en localStorage. El token se adjunta a cada request
 * mediante el interceptor; el refresh se gestiona con single-flight y ademas
 * se programa de forma proactiva ~1 min antes del vencimiento para absorber
 * cargas largas sin parpadeos de 401 -> refresh -> retry que se observaban
 * en produccion con TTL=15min.
 */
@Injectable({ providedIn: 'root' })
export class AuthService implements OnDestroy {
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
  /** Handle del timer proactivo para poder cancelarlo en logout/refresh manual. */
  private refreshProactivoHandle?: ReturnType<typeof setTimeout>;
  /** Handler del listener de storage para resetear el timer cuando otra tab refresca. */
  private storageListener?: (event: StorageEvent) => void;

  constructor() {
    // Si la sesion se reconstruye desde localStorage (recarga de pagina o
    // tab inactiva), reagenda el refresh proactivo del nuevo access token para
    // evitar que la proxima peticion del operador caiga en 401.
    if (this._sesion()) {
      this.programarRefreshProactivo(this.getAccessToken() ?? undefined);
    }
    // Multi-tab: si otra tab refresca el access token (lo escribe en localStorage),
    // reagendamos nuestro timer con el nuevo exp para no quedarnos con un delay
    // calculado sobre el token viejo. El evento NO se dispara en la tab que
    // escribe, asi que aqui solo reaccionamos a los cambios de OTRA tab.
    this.storageListener = (event: StorageEvent) => {
      if (event.key !== ACCESS_KEY) {
        return;
      }
      this.programarRefreshProactivo(event.newValue);
    };
    window.addEventListener('storage', this.storageListener);
  }

  login(usuario: string, contrasena: string, recaptchaToken: string | null = null): Observable<void> {
    const body: { usuario: string; contrasena: string; recaptchaToken?: string } = {
      usuario,
      contrasena,
    };
    if (recaptchaToken) {
      body.recaptchaToken = recaptchaToken;
    }
    return this.http
      .post<RespuestaApi<TokenResponse>>(`${this.base}/login`, body)
      .pipe(
        map((r) => {
          this.guardar(r.data);
          this.programarRefreshProactivo(r.data.accessToken);
          return;
        }),
      );
  }

  /**
   * Solicita al backend la regeneracion de la clave del paciente asociado al
   * documento (DNI/CE) ingresado. La respuesta es siempre generica e incluye
   * el telefono ofuscado (ultimos 4 digitos del telefono al que se envio, o
   * del DNI tipeado si el paciente no existe) para que el usuario verifique
   * visualmente sin comprometer la informacion.
   *
   * Si el caller fue bloqueado por rate-limit, el backend responde 200 con
   * `retryAfterSeconds > 0` y `mensajeUsuario` explicativo; el componente
   * detecta eso y muestra el tiempo de espera.
   */
  recuperarPassword(documento: string): Observable<RecuperarPasswordData> {
    return this.http
      .post<RespuestaApi<RecuperarPasswordData>>(`${this.base}/recuperar-password`, {
        documento,
      })
      .pipe(map((r) => r.data));
  }

  logout(): void {
    this.cancelarRefreshProactivo();
    const refreshToken = this.getRefreshToken();
    if (refreshToken) {
      // Revocacion best-effort en el backend; no bloquea el cierre local.
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
      this.cancelarRefreshProactivo();
      return throwError(() => new Error('No hay refresh token disponible.'));
    }
    this.refrescando$ = this.http
      .post<RespuestaApi<TokenResponse>>(`${this.base}/refresh`, { refreshToken })
      .pipe(
        map((r) => {
          this.guardar(r.data);
          this.programarRefreshProactivo(r.data.accessToken);
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
    this.cancelarRefreshProactivo();
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(SESSION_KEY);
    this._sesion.set(null);
  }

  /**
   * Decodifica el payload JWT (sin firma) para conocer el timestamp exacto
   * de expiracion. Tolerante a tokens malformados.
   */
  private decodificarExp(accessToken: string): number | null {
    const partes = accessToken.split('.');
    if (partes.length !== 3) {
      return null;
    }
    try {
      const payload = atob(partes[1].replace(/-/g, '+').replace(/_/g, '/')) as string;
      const json = JSON.parse(payload) as DecodedJwtPayload;
      return typeof json.exp === 'number' ? json.exp * 1000 : null;
    } catch {
      return null;
    }
  }

  /**
   * Programa un refresh automatico antes de que el access token expire.
   * Si la sesion se reconstruye desde localStorage (recarga de pagina), el
   * constructor reagenda el timer con el access token vigente.
   */
  private programarRefreshProactivo(accessToken: string | null | undefined): void {
    this.cancelarRefreshProactivo();
    if (!accessToken) {
      return;
    }
    const expMs = this.decodificarExp(accessToken);
    if (!expMs) {
      return;
    }
    const restante = expMs - Date.now() - REFRESH_LEAD_MS;
    // Si el token ya expiro (o expira dentro del margen), refrescar de inmediato;
    // si no, esperar el remanente con un piso de 5s para no spammear si hay drift.
    const delay = restante <= 0 ? REFRESH_RECARGA_INMEDIATA_MS : Math.max(restante, 5_000);
    if (delay === REFRESH_RECARGA_INMEDIATA_MS) {
      if (this._sesion() && this.getRefreshToken()) {
        this.refresh().subscribe({ error: () => undefined });
      }
      return;
    }
    this.refreshProactivoHandle = setTimeout(() => {
      if (this._sesion() && this.getRefreshToken()) {
        this.refresh().subscribe({ error: () => undefined });
      }
    }, delay);
  }

  private cancelarRefreshProactivo(): void {
    if (this.refreshProactivoHandle) {
      clearTimeout(this.refreshProactivoHandle);
      this.refreshProactivoHandle = undefined;
    }
  }

  ngOnDestroy(): void {
    this.cancelarRefreshProactivo();
    if (this.storageListener) {
      window.removeEventListener('storage', this.storageListener);
      this.storageListener = undefined;
    }
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

export interface RecuperarPasswordData {
  mensajeUsuario: string;
  soporteTelefono: string;
  telefonoOfuscado: string;
  retryAfterSeconds: number | null;
}
