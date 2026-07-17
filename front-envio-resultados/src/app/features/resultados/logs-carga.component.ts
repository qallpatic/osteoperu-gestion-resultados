import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

import { ResultadosService } from './resultados.service';
import { LogCarga } from './resultados.models';
import { sanitizarDocumento } from '../../core/utils/util-documento';

@Component({
  selector: 'app-resultados-logs',
  imports: [FormsModule, DatePipe],
  templateUrl: './logs-carga.component.html',
  styleUrl: './logs-carga.component.css',
})
export class LogsCargaComponent implements OnInit {
  private readonly srv = inject(ResultadosService);

  readonly logs = signal<LogCarga[]>([]);
  readonly cargando = signal(false);
  readonly pagina = signal(0);
  readonly totalPaginas = signal(0);
  readonly totalElementos = signal(0);
  readonly tamano = 10;

  fdni = '';
  fdesde = '';
  fhasta = '';

  /** Orden: null = default backend (fechaCreacion desc). */
  readonly ordenarPor = signal<string | null>(null);
  readonly direccion = signal<'asc' | 'desc' | null>(null);

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
    this.pagina.set(0);
    this.cargar();
  }

  /** Handler de input para el filtro de DNI: max 12 dígitos. */
  onDniInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.fdni = sanitizarDocumento(input.value, 12);
    input.value = this.fdni;
  }

  readonly notice = signal<string>('');

  readonly rangoDesde = computed(() =>
    this.totalElementos() === 0 ? 0 : this.pagina() * this.tamano + 1,
  );
  readonly rangoHasta = computed(() =>
    Math.min((this.pagina() + 1) * this.tamano, this.totalElementos()),
  );

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.srv
      .logs({
        dni: this.fdni.trim() || undefined,
        desde: this.fdesde || undefined,
        hasta: this.fhasta || undefined,
        pagina: this.pagina(),
        tamano: this.tamano,
        ordenarPor: this.ordenarPor() ?? undefined,
        direccion: this.direccion() ?? undefined,
      })
      .subscribe({
        next: (p) => {
          this.logs.set(p.contenido);
          this.totalPaginas.set(p.totalPaginas);
          this.totalElementos.set(p.totalElementos);
          this.cargando.set(false);
        },
        error: (e: HttpErrorResponse) => {
          this.cargando.set(false);
          this.notice.set(e.status === 0 ? 'No se pudo conectar con el servidor.' : 'Error al cargar los logs.');
        },
      });
  }

  aplicar(): void {
    this.pagina.set(0);
    this.cargar();
  }

  irPagina(delta: number): void {
    const nueva = this.pagina() + delta;
    if (nueva < 0 || nueva >= this.totalPaginas()) {
      return;
    }
    this.pagina.set(nueva);
    this.cargar();
  }
}
