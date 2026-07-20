import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

import { PacienteService } from './paciente.service';
import { AuthService } from '../../core/auth/auth.service';
import { Archivo, ResultadoDetalle, ResultadoResumen } from '../resultados/resultados.models';
import { NombreNaturalPipe } from '../../core/pipes/nombre-natural.pipe';

@Component({
  selector: 'app-mis-resultados',
  imports: [FormsModule, DatePipe, NombreNaturalPipe],
  templateUrl: './mis-resultados.component.html',
  styleUrl: './mis-resultados.component.css',
})
export class MisResultadosComponent implements OnInit {
  private readonly srv = inject(PacienteService);
  private readonly auth = inject(AuthService);

  /** ADMIN ve todos los resultados; el paciente solo los suyos. */
  readonly esAdmin = computed(() => this.auth.roles().includes('ADMIN'));

  readonly resultados = signal<ResultadoResumen[]>([]);
  readonly cargando = signal(false);
  readonly pagina = signal(0);
  readonly totalPaginas = signal(0);
  readonly totalElementos = signal(0);
  readonly tamano = 8;

  fdesde = '';
  fhasta = '';

  readonly notice = signal<{ type: 'ok' | 'error'; text: string } | null>(null);
  readonly detalle = signal<ResultadoDetalle | null>(null);
  readonly cargandoDetalle = signal(false);
  readonly ocupadoArchivo = signal<number | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.srv
      .misResultados({
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

  abrirDetalle(r: ResultadoResumen): void {
    this.cargandoDetalle.set(true);
    this.detalle.set(null);
    this.srv.detalle(r.id).subscribe({
      next: (d) => {
        this.detalle.set(d);
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

  verArchivo(a: Archivo): void {
    const d = this.detalle();
    if (!d) {
      return;
    }
    this.ocupadoArchivo.set(a.id);
    this.srv.verArchivo(d.id, a.id).subscribe({
      next: (blob) => {
        this.ocupadoArchivo.set(null);
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
      },
      error: (e: HttpErrorResponse) => {
        this.ocupadoArchivo.set(null);
        this.notice.set({ type: 'error', text: this.msg(e) });
      },
    });
  }

  descargar(a: Archivo): void {
    const d = this.detalle();
    if (!d) {
      return;
    }
    this.ocupadoArchivo.set(a.id);
    this.srv.descargarArchivo(d.id, a.id).subscribe({
      next: (blob) => {
        this.ocupadoArchivo.set(null);
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = a.nombreOriginal;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: (e: HttpErrorResponse) => {
        this.ocupadoArchivo.set(null);
        this.notice.set({ type: 'error', text: this.msg(e) });
      },
    });
  }

  private msg(e: HttpErrorResponse): string {
    if (e.status === 0) {
      return 'No se pudo conectar con el servidor.';
    }
    return 'Ocurrió un error. Intenta nuevamente.';
  }

  /** Convierte el mensaje a HTML con enlaces clickeables. */
  mensajeHtml(texto: string): string {
    if (!texto) {
      return '';
    }
    const htmlEscapado = texto
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\r?\n/g, '<br>');
    return htmlEscapado.replace(
      /(https?:\/\/[^\s<]+)/g,
      '<a href="$1" target="_blank" rel="noopener noreferrer">$1</a>',
    );
  }

  /** Cuenta solo archivos activos para mostrar el total real al paciente. */
  contarArchivosActivos(d: ResultadoDetalle): number {
    return d.archivos.filter((a) => a.activo).length;
  }

  /**
   * Calcula la vigencia del resultado contra la fecha actual.
   * Devuelve uno de cuatro estados segun los dias restantes:
   *  - 'ok'       (>30 dias): vigente
   *  - 'warn'     (8-30 dias): por vencer (ambar)
   *  - 'alert'    (1-7 dias): proximo a vencer (rojo)
   *  - 'vencido'  (<=0 dias): caducado
   */
  vigenciaInfo(fechaVigencia: string | null | undefined): {
    estado: 'ok' | 'warn' | 'alert' | 'vencido' | 'sin-fecha';
    texto: string;
    dias: number | null;
  } {
    if (!fechaVigencia) {
      return { estado: 'sin-fecha', texto: '', dias: null };
    }
    const hasta = new Date(fechaVigencia);
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    hasta.setHours(0, 0, 0, 0);
    const diffMs = hasta.getTime() - hoy.getTime();
    const dias = Math.round(diffMs / 86400000);

    if (dias > 30) {
      return { estado: 'ok', texto: `Vigente · ${dias} días`, dias };
    }
    if (dias > 7) {
      return { estado: 'warn', texto: `Vence en ${dias} días`, dias };
    }
    if (dias > 0) {
      return { estado: 'alert', texto: `Vence en ${dias} ${dias === 1 ? 'día' : 'días'}`, dias };
    }
    if (dias === 0) {
      return { estado: 'alert', texto: 'Vence hoy', dias: 0 };
    }
    return { estado: 'vencido', texto: `Caducado hace ${Math.abs(dias)} ${Math.abs(dias) === 1 ? 'día' : 'días'}`, dias };
  }

  esVencido(fechaVigencia: string | null | undefined): boolean {
    return this.vigenciaInfo(fechaVigencia).estado === 'vencido';
  }
}
