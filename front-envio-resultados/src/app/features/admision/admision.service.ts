import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { environment } from '../../../environments/environment';
import { RespuestaApi } from '../../core/models/token-response.model';
import { PaginaResponse } from '../resultados/resultados.models';

export interface CredencialResponse {
  dni: string;
  nombre: string | null;
  usuario: string;
  clave: string;
  portalUrl: string;
  qrUrl: string;
  smsEnviado: boolean;
}

export interface GenerarUsuarioRequest {
  dni: string;
  nombre?: string;
  telefono?: string;
  enviarSms?: boolean;
}

export interface PacienteAdmision {
  dni: string;
  nombre: string | null;
  telefono: string | null;
  totalResultados: number;
  ultimaFecha: string | null;
  estadoCuenta: 'SIN_CUENTA' | 'ACTIVO' | 'ANULADO';
}

/** Admision: listado de pacientes y alta/baja de su cuenta (/api/admision). */
@Injectable({ providedIn: 'root' })
export class AdmisionService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/admision`;

  listarPacientes(p: {
    dni?: string;
    pagina: number;
    tamano: number;
    ordenarPor?: string;
    direccion?: string;
  }): Observable<PaginaResponse<PacienteAdmision>> {
    let params = new HttpParams().set('pagina', p.pagina).set('tamano', p.tamano);
    if (p.dni) {
      params = params.set('dni', p.dni);
    }
    if (p.ordenarPor) {
      params = params.set('ordenarPor', p.ordenarPor);
    }
    if (p.direccion) {
      params = params.set('direccion', p.direccion);
    }
    return this.http
      .get<RespuestaApi<PaginaResponse<PacienteAdmision>>>(`${this.base}/pacientes`, { params })
      .pipe(map((r) => r.data));
  }

  generarUsuario(body: GenerarUsuarioRequest): Observable<CredencialResponse> {
    return this.http
      .post<RespuestaApi<CredencialResponse>>(`${this.base}/usuarios/generar`, body)
      .pipe(map((r) => r.data));
  }

  anularUsuario(dni: string): Observable<void> {
    return this.http
      .post<RespuestaApi<void>>(`${this.base}/usuarios/${dni}/anular`, {})
      .pipe(map(() => undefined));
  }
}
