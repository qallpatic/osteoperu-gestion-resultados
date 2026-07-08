import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { environment } from '../../../environments/environment';
import { RespuestaApi } from '../../core/models/token-response.model';
import { PaginaResponse, ResultadoDetalle, ResultadoResumen } from '../resultados/resultados.models';

/** Portal del paciente: solo lectura de sus propios resultados (/api/paciente). */
@Injectable({ providedIn: 'root' })
export class PacienteService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/paciente`;

  misResultados(p: {
    desde?: string;
    hasta?: string;
    pagina: number;
    tamano: number;
  }): Observable<PaginaResponse<ResultadoResumen>> {
    let params = new HttpParams().set('pagina', p.pagina).set('tamano', p.tamano);
    if (p.desde) {
      params = params.set('desde', p.desde);
    }
    if (p.hasta) {
      params = params.set('hasta', p.hasta);
    }
    return this.http
      .get<RespuestaApi<PaginaResponse<ResultadoResumen>>>(`${this.base}/resultados`, { params })
      .pipe(map((r) => r.data));
  }

  detalle(id: number): Observable<ResultadoDetalle> {
    return this.http
      .get<RespuestaApi<ResultadoDetalle>>(`${this.base}/resultados/${id}`)
      .pipe(map((r) => r.data));
  }

  verArchivo(idResultado: number, idArchivo: number): Observable<Blob> {
    return this.http.get(`${this.base}/resultados/${idResultado}/archivos/${idArchivo}`, {
      responseType: 'blob',
    });
  }

  descargarArchivo(idResultado: number, idArchivo: number): Observable<Blob> {
    return this.http.get(`${this.base}/resultados/${idResultado}/archivos/${idArchivo}/descargar`, {
      responseType: 'blob',
    });
  }
}
