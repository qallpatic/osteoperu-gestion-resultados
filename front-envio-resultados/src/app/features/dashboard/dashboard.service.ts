import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { environment } from '../../../environments/environment';
import { RespuestaApi } from '../../core/models/token-response.model';
import {
  AlmacenamientoDetalle,
  DashboardParams,
  DashboardResumen,
  VistasDetalle,
} from './dashboard.models';

/** Consume la API de métricas (/api/dashboard). Sólo accesible para rol ADMIN. */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/dashboard`;

  resumen(p: DashboardParams = {}): Observable<DashboardResumen> {
    return this.http
      .get<RespuestaApi<DashboardResumen>>(`${this.base}/resumen`, { params: this.toParams(p) })
      .pipe(map((r) => r.data));
  }

  /**
   * Descarga el .xlsx del dashboard para el mismo rango que {@link resumen}.
   * Devuelve el blob crudo: el caller decide si lo guarda o lo embebe.
   */
  exportarExcel(p: DashboardParams = {}): Observable<Blob> {
    return this.http.get(`${this.base}/resumen/exportar`, {
      params: this.toParams(p),
      responseType: 'blob',
    });
  }

  /** Detalle de la pestaña Almacenamiento (distribución, top 10 pesados, acumulado). */
  almacenamientoDetalle(p: DashboardParams = {}): Observable<AlmacenamientoDetalle> {
    return this.http
      .get<RespuestaApi<AlmacenamientoDetalle>>(`${this.base}/almacenamiento/detalle`, {
        params: this.toParams(p),
      })
      .pipe(map((r) => r.data));
  }

  /** Detalle de la pestaña Vistas (top 10 vistos, buckets, curva temporal). */
  vistasDetalle(p: DashboardParams = {}): Observable<VistasDetalle> {
    return this.http
      .get<RespuestaApi<VistasDetalle>>(`${this.base}/vistas/detalle`, {
        params: this.toParams(p),
      })
      .pipe(map((r) => r.data));
  }

  /**
   * Descarga el .xlsx con el detalle plano de "Resultados cargados" para el
   * mismo rango que las demas vistas del dashboard.
   */
  exportarExcelResultados(p: DashboardParams = {}): Observable<Blob> {
    return this.http.get(`${this.base}/resultados/exportar`, {
      params: this.toParams(p),
      responseType: 'blob',
    });
  }

  /** Construye HttpParams a partir de los strings desde/hasta (omite campos null/undefined). */
  private toParams(p: DashboardParams): HttpParams {
    let params = new HttpParams();
    if (p.desde) {
      params = params.set('desde', p.desde);
    }
    if (p.hasta) {
      params = params.set('hasta', p.hasta);
    }
    return params;
  }
}
