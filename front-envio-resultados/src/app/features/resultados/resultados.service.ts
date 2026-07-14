import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { environment } from '../../../environments/environment';
import { RespuestaApi } from '../../core/models/token-response.model';
import {
  ActualizarResultadoRequest,
  LogCarga,
  PaginaResponse,
  ResultadoDetalle,
  ResultadoResumen,
  UploadRequest,
  UploadResultadoResponse,
} from './resultados.models';
import { ReniecService } from '../../core/services/reniec.service';

/** Consume la API de resultados (/api/resultados). El JWT lo añade el interceptor. */
@Injectable({ providedIn: 'root' })
export class ResultadosService {
  private readonly http = inject(HttpClient);
  private readonly reniec = inject(ReniecService);
  private readonly base = `${environment.apiBaseUrl}/api/resultados`;

  /** Carga (el endpoint devuelve el DTO directo, no envuelto). */
  upload(req: UploadRequest): Observable<UploadResultadoResponse> {
    const form = new FormData();
    form.append('phone', req.phone);
    form.append('dni', req.dni);
    form.append('patientName', req.patientName);
    form.append('mensaje', req.mensaje);
    form.append('confirmarNombre', String(req.confirmarNombre));
    for (const file of req.files) {
      form.append('files', file, file.name);
    }
    return this.http.post<UploadResultadoResponse>(`${this.base}/upload`, form);
  }

  listar(p: {
    dni?: string;
    nombre?: string;
    desde?: string;
    hasta?: string;
    pagina: number;
    tamano: number;
  }): Observable<PaginaResponse<ResultadoResumen>> {
    return this.http
      .get<RespuestaApi<PaginaResponse<ResultadoResumen>>>(this.base, { params: this.armarParams(p) })
      .pipe(map((r) => r.data));
  }

  detalle(id: number): Observable<ResultadoDetalle> {
    return this.http
      .get<RespuestaApi<ResultadoDetalle>>(`${this.base}/${id}`)
      .pipe(map((r) => r.data));
  }

  actualizar(id: number, body: ActualizarResultadoRequest): Observable<ResultadoDetalle> {
    return this.http
      .put<RespuestaApi<ResultadoDetalle>>(`${this.base}/${id}`, body)
      .pipe(map((r) => r.data));
  }

  logs(p: {
    dni?: string;
    desde?: string;
    hasta?: string;
    pagina: number;
    tamano: number;
  }): Observable<PaginaResponse<LogCarga>> {
    return this.http
      .get<RespuestaApi<PaginaResponse<LogCarga>>>(`${this.base}/logs`, { params: this.armarParams(p) })
      .pipe(map((r) => r.data));
  }

  /** Nombre completo desde RENIEC (para autocompletar la carga). */
  consultarReniec(dni: string): Observable<{ dni: string; nombre: string | null }> {
    return this.reniec.consultarNombre(dni).pipe(
      map((nombre) => ({ dni, nombre })),
    );
  }

  private armarParams(p: Record<string, string | number | undefined>): HttpParams {
    let params = new HttpParams();
    for (const [k, v] of Object.entries(p)) {
      if (v !== undefined && v !== '') {
        params = params.set(k, v);
      }
    }
    return params;
  }
}
