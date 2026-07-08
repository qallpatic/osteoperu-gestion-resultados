import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

import { ResultadosService } from './resultados.service';
import { ResultadoDetalle, ResultadoResumen } from './resultados.models';

@Component({
  selector: 'app-resultados-consultar',
  imports: [FormsModule, DatePipe],
  templateUrl: './consultar-resultados.component.html',
  styleUrl: './consultar-resultados.component.css',
})
export class ConsultarResultadosComponent implements OnInit {
  private readonly srv = inject(ResultadosService);

  readonly resultados = signal<ResultadoResumen[]>([]);
  readonly cargando = signal(false);
  readonly pagina = signal(0);
  readonly totalPaginas = signal(0);
  readonly totalElementos = signal(0);
  readonly tamano = 10;

  fdni = '';
  fnombre = '';
  fdesde = '';
  fhasta = '';

  readonly notice = signal<{ type: 'ok' | 'error'; text: string } | null>(null);

  // Detalle / edición
  readonly detalle = signal<ResultadoDetalle | null>(null);
  readonly cargandoDetalle = signal(false);
  readonly guardando = signal(false);
  editTelefono = '';
  editNombre = '';
  reenviar = false;

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
      .listar({
        dni: this.fdni.trim() || undefined,
        nombre: this.fnombre.trim() || undefined,
        desde: this.fdesde || undefined,
        hasta: this.fhasta || undefined,
        pagina: this.pagina(),
        tamano: this.tamano,
      })
      .subscribe({
        next: (p) => {
          this.resultados.set(p.contenido);
          this.totalPaginas.set(p.totalPaginas);
          this.totalElementos.set(p.totalElementos);
          this.cargando.set(false);
        },
        error: (e: HttpErrorResponse) => {
          this.cargando.set(false);
          this.notice.set({ type: 'error', text: this.msg(e) });
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

  verDetalle(r: ResultadoResumen): void {
    this.cargandoDetalle.set(true);
    this.detalle.set(null);
    this.srv.detalle(r.id).subscribe({
      next: (d) => {
        this.detalle.set(d);
        this.editTelefono = d.telefono;
        this.editNombre = d.nombrePaciente ?? '';
        this.reenviar = false;
        this.cargandoDetalle.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.cargandoDetalle.set(false);
        this.notice.set({ type: 'error', text: this.msg(e) });
      },
    });
  }

  cerrarDetalle(): void {
    this.detalle.set(null);
  }

  guardarCambios(): void {
    const d = this.detalle();
    if (!d) {
      return;
    }
    this.guardando.set(true);
    this.srv
      .actualizar(d.id, {
        telefono: this.editTelefono || undefined,
        nombrePaciente: this.editNombre || undefined,
        reenviarSms: this.reenviar,
      })
      .subscribe({
        next: (res) => {
          this.guardando.set(false);
          this.detalle.set(res);
          this.notice.set({
            type: 'ok',
            text: this.reenviar ? 'Resultado actualizado y SMS reenviado.' : 'Resultado actualizado.',
          });
          this.cargar();
        },
        error: (e: HttpErrorResponse) => {
          this.guardando.set(false);
          this.notice.set({ type: 'error', text: this.msg(e) });
        },
      });
  }

  private msg(e: HttpErrorResponse): string {
    if (e.status === 0) {
      return 'No se pudo conectar con el servidor.';
    }
    return e.error?.message ?? 'Ocurrió un error. Intenta nuevamente.';
  }
}
