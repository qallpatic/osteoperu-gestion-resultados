import { Component, OnInit, inject, signal } from '@angular/core';
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

  readonly lista = this.srv.lista;
  readonly cargando = this.srv.cargando;

  /** Precarga del modal de subida (null = cerrado). */
  readonly prefill = signal<UploadPrefill | null>(null);

  ngOnInit(): void {
    this.srv.cargar();
  }

  refrescar(): void {
    this.srv.cargar();
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
