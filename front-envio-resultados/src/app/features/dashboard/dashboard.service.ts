import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { environment } from '../../../environments/environment';
import { RespuestaApi } from '../../core/models/token-response.model';
import { DashboardParams, DashboardResumen } from './dashboard.models';

/** Consume la API de métricas (/api/dashboard). Sólo accesible para rol ADMIN. */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/dashboard`;

  resumen(p: DashboardParams = {}): Observable<DashboardResumen> {
    let params = new HttpParams();
    if (p.desde) {
      params = params.set('desde', p.desde);
    }
    if (p.hasta) {
      params = params.set('hasta', p.hasta);
    }
    return this.http
      .get<RespuestaApi<DashboardResumen>>(`${this.base}/resumen`, { params })
      .pipe(map((r) => r.data));
  }

  /**
   * Descarga el .xlsx del dashboard para el mismo rango que {@link resumen}.
   * Devuelve el blob crudo: el caller decide si lo guarda o lo embebe.
   */
  exportarExcel(p: DashboardParams = {}): Observable<Blob> {
    let params = new HttpParams();
    if (p.desde) {
      params = params.set('desde', p.desde);
    }
    if (p.hasta) {
      params = params.set('hasta', p.hasta);
    }
    return this.http.get(`${this.base}/resumen/exportar`, {
      params,
      responseType: 'blob',
    });
  }
}