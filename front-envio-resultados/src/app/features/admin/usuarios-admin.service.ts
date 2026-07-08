import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { environment } from '../../../environments/environment';
import { RespuestaApi } from '../../core/models/token-response.model';
import {
  ListarUsuariosParams,
  PaginaResponse,
  RolResponse,
  UsuarioRequest,
  UsuarioResponse,
  UsuarioUpdateRequest,
} from './admin.models';

/** Consume la API de administración de usuarios (/api/admin). */
@Injectable({ providedIn: 'root' })
export class UsuariosAdminService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/admin`;

  listar(p: ListarUsuariosParams): Observable<PaginaResponse<UsuarioResponse>> {
    let params = new HttpParams()
      .set('pagina', p.pagina)
      .set('tamano', p.tamano)
      .set('ordenarPor', p.ordenarPor ?? 'id')
      .set('direccion', p.direccion ?? 'asc');
    if (p.buscar) {
      params = params.set('buscar', p.buscar);
    }
    if (p.rol) {
      params = params.set('rol', p.rol);
    }
    if (p.estado !== undefined) {
      params = params.set('estado', p.estado);
    }
    return this.http
      .get<RespuestaApi<PaginaResponse<UsuarioResponse>>>(`${this.base}/usuarios`, { params })
      .pipe(map((r) => r.data));
  }

  crear(req: UsuarioRequest): Observable<UsuarioResponse> {
    return this.http
      .post<RespuestaApi<UsuarioResponse>>(`${this.base}/usuarios`, req)
      .pipe(map((r) => r.data));
  }

  actualizar(id: number, req: UsuarioUpdateRequest): Observable<UsuarioResponse> {
    return this.http
      .put<RespuestaApi<UsuarioResponse>>(`${this.base}/usuarios/${id}`, req)
      .pipe(map((r) => r.data));
  }

  eliminar(id: number): Observable<void> {
    return this.http
      .delete<RespuestaApi<void>>(`${this.base}/usuarios/${id}`)
      .pipe(map(() => undefined));
  }

  cambiarEstado(id: number, activo: boolean): Observable<UsuarioResponse> {
    return this.http
      .patch<RespuestaApi<UsuarioResponse>>(`${this.base}/usuarios/${id}/estado`, { activo })
      .pipe(map((r) => r.data));
  }

  asignarRoles(id: number, rolesIds: number[]): Observable<UsuarioResponse> {
    return this.http
      .patch<RespuestaApi<UsuarioResponse>>(`${this.base}/usuarios/${id}/roles`, { rolesIds })
      .pipe(map((r) => r.data));
  }

  cambiarPassword(id: number, password: string): Observable<void> {
    return this.http
      .patch<RespuestaApi<void>>(`${this.base}/usuarios/${id}/password`, { password })
      .pipe(map(() => undefined));
  }

  roles(): Observable<RolResponse[]> {
    return this.http
      .get<RespuestaApi<RolResponse[]>>(`${this.base}/roles`)
      .pipe(map((r) => r.data));
  }
}
