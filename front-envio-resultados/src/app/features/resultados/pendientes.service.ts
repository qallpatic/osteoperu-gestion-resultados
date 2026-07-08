import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { map } from 'rxjs';

import { environment } from '../../../environments/environment';
import { RespuestaApi } from '../../core/models/token-response.model';
import { PaginaResponse } from './resultados.models';

/** Paciente registrado por Admisión, pendiente de subir resultados. */
export interface PacientePendiente {
  dni: string;
  nombre: string | null;
  telefono: string | null;
  fechaRegistro: string;
}

/** Estado compartido de la campanita de Resultados. */
@Injectable({ providedIn: 'root' })
export class PendientesService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/resultados`;

  readonly contador = signal(0);
  readonly lista = signal<PacientePendiente[]>([]);
  readonly cargando = signal(false);

  cargar(): void {
    this.cargando.set(true);
    this.http
      .get<RespuestaApi<PaginaResponse<PacientePendiente>>>(`${this.base}/pendientes?tamano=50`)
      .pipe(map((r) => r.data))
      .subscribe({
        next: (p) => {
          this.lista.set(p.contenido);
          this.contador.set(p.totalElementos);
          this.cargando.set(false);
        },
        error: () => {
          this.cargando.set(false);
          /* silencioso: la campanita no debe romper la app */
        },
      });
  }
}
