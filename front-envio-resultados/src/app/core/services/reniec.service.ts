import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { environment } from '../../../environments/environment';
import { RespuestaApi } from '../models/token-response.model';

/**
 * Servicio compartido para consultar RENIEC por DNI.
 * Reutiliza el endpoint existente en /api/resultados/reniec/{dni}.
 * La respuesta del backend ya viene cacheada en BD (RENIEC_CACHE),
 * asi que este metodo no incurre en costo del proveedor si el dato es reciente.
 */
@Injectable({ providedIn: 'root' })
export class ReniecService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/resultados`;

  /** Devuelve el nombre canónico "APELLIDO_P APELLIDO_M, NOMBRES" o null. */
  consultarNombre(dni: string): Observable<string | null> {
    return this.http
      .get<RespuestaApi<{ dni: string; nombre: string | null }>>(`${this.base}/reniec/${dni}`)
      .pipe(map((r) => r.data?.nombre ?? null));
  }
}