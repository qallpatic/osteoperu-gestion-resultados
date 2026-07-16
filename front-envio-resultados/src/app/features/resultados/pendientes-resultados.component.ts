import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';

import { PacientePendiente, PendientesService } from './pendientes.service';
import { UploadComponent, UploadPrefill } from './upload.component';

/** Pacientes registrados por Admisión, pendientes de subir resultados. */
@Component({
  selector: 'app-pendientes-resultados',
  imports: [DatePipe, UploadComponent],
  templateUrl: './pendientes-resultados.component.html',
  styleUrl: './pendientes-resultados.component.css',
})
export class PendientesResultadosComponent implements OnInit {
  private readonly srv = inject(PendientesService);

  readonly cargando = this.srv.cargando;

  /** Orden: null = default backend (fechaCreacion desc). */
  readonly ordenarPor = signal<string | null>(null);
  readonly direccion = signal<'asc' | 'desc' | null>(null);

  /** Lista que ya viene del service, filtrada y ordenada en memoria. */
  readonly lista = computed<PacientePendiente[]>(() => {
    const fuente = this.srv.lista();
    const campo = this.ordenarPor();
    const dir = this.direccion();
    if (!campo || !dir) {
      return fuente;
    }
    const factor = dir === 'asc' ? 1 : -1;
    return [...fuente].sort((a, b) => {
      const va = (a as unknown as Record<string, unknown>)[campo];
      const vb = (b as unknown as Record<string, unknown>)[campo];
      if (va == null && vb == null) {
        return 0;
      }
      if (va == null) {
        return 1;
      }
      if (vb == null) {
        return -1;
      }
      if (typeof va === 'number' && typeof vb === 'number') {
        return (va - vb) * factor;
      }
      return String(va).localeCompare(String(vb), 'es', { numeric: true }) * factor;
    });
  });

  /** Precarga del modal de subida (null = cerrado). */
  readonly prefill = signal<UploadPrefill | null>(null);

  ngOnInit(): void {
    this.srv.cargar();
  }

  refrescar(): void {
    this.srv.cargar();
  }

  /** Cicla null -> asc -> desc -> null para una columna. */
  ordenar(campo: string): void {
    if (this.ordenarPor() !== campo) {
      this.ordenarPor.set(campo);
      this.direccion.set('asc');
    } else if (this.direccion() === 'asc') {
      this.direccion.set('desc');
    } else {
      this.ordenarPor.set(null);
      this.direccion.set(null);
    }
  }

  subir(p: PacientePendiente): void {
    this.prefill.set({ dni: p.dni, nombre: p.nombre, telefono: p.telefono });
  }

  cerrarModal(): void {
    this.prefill.set(null);
  }

  onEnviado(): void {
    this.cerrarModal();
    this.srv.cargar();
  }
}
