import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { NgChartsModule } from 'ng2-charts';
import { Subject, debounceTime, interval } from 'rxjs';
import type { ChartData, ChartOptions } from 'chart.js';

import { DashboardService } from '../dashboard.service';
import { ResultadoMasVisto, VistasDetalle } from '../dashboard.models';
import { SeccionGenericaComponent, PresetSeccion } from './seccion-generica.component';

/**
 * Seccion "Vistas de pacientes" del Dashboard.
 *
 * Grafica: top 10 resultados mas vistos (barras horizontales) + distribucion
 * por buckets de vistas (barras) + curva temporal de aperturas desde
 * EVENTO_VISTA (linea). Reutiliza el shell `SeccionGenericaComponent`.
 */
@Component({
  selector: 'app-dashboard-vistas',
  imports: [NgChartsModule, DatePipe, DecimalPipe, SeccionGenericaComponent],
  templateUrl: './dashboard-vistas.component.html',
  styleUrl: './dashboard-vistas.component.css',
})
export class DashboardVistasComponent implements OnInit {
  private readonly srv = inject(DashboardService);
  private readonly destroyRef = inject(DestroyRef);

  readonly cargando = signal(false);
  readonly exportando = signal(false);
  readonly error = signal<string | null>(null);
  readonly data = signal<VistasDetalle | null>(null);

  readonly desde = signal<string>('');
  readonly hasta = signal<string>('');
  readonly preset = signal<PresetSeccion>('30d');
  readonly today = this.toIso(new Date());

  private peticionSeq = 0;
  private readonly rangoCambiado$ = new Subject<void>();

  readonly rangoTexto = computed(() => {
    const d = this.data();
    return d ? `${d.desde} -> ${d.hasta}` : '';
  });

  readonly totalVistasEnRango = computed(() => {
    const curva = this.data()?.curvaTemporal ?? [];
    return curva.reduce((acc, p) => acc + p.total, 0);
  });

  readonly picoCurva = computed(() => {
    const curva = this.data()?.curvaTemporal ?? [];
    if (curva.length === 0) {
      return null;
    }
    return curva.reduce((acc, p) => (p.total > acc.total ? p : acc));
  });

  readonly promedioDiarioVistas = computed(() => {
    const curva = this.data()?.curvaTemporal ?? [];
    if (curva.length === 0) {
      return 0;
    }
    const total = curva.reduce((acc, p) => acc + p.total, 0);
    return Math.round((total / curva.length) * 10) / 10;
  });

  readonly esPrimerLoad = computed(() => this.cargando() && !this.data());

  chartTopVistos: ChartData<'bar'> = { labels: [], datasets: [] };
  chartBuckets: ChartData<'bar'> = { labels: [], datasets: [] };
  chartCurva: ChartData<'line'> = { labels: [], datasets: [] };

  readonly chartTopOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    indexAxis: 'y',
    animation: { duration: 300 },
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: '#043d36',
        titleColor: '#fff',
        bodyColor: '#e6f2f0',
        borderColor: '#075e54',
        borderWidth: 1,
        padding: 10,
      },
    },
    scales: {
      x: { beginAtZero: true, grid: { color: '#eef3f1' }, ticks: { color: '#6c757d', precision: 0 } },
      y: { grid: { display: false }, ticks: { color: '#6c757d', autoSkip: false } },
    },
  };

  readonly chartBucketsOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    animation: { duration: 300 },
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: '#043d36',
        titleColor: '#fff',
        bodyColor: '#e6f2f0',
        borderColor: '#075e54',
        borderWidth: 1,
        padding: 10,
      },
    },
    scales: {
      x: { grid: { display: false }, ticks: { color: '#6c757d' } },
      y: { beginAtZero: true, grid: { color: '#eef3f1' }, ticks: { color: '#6c757d', precision: 0 } },
    },
  };

  readonly chartCurvaOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: { mode: 'index', intersect: false },
    animation: { duration: 300 },
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: '#043d36',
        titleColor: '#fff',
        bodyColor: '#e6f2f0',
        borderColor: '#075e54',
        borderWidth: 1,
        padding: 10,
      },
    },
    scales: {
      x: { grid: { display: false }, ticks: { color: '#6c757d', maxTicksLimit: 12 } },
      y: { beginAtZero: true, grid: { color: '#eef3f1' }, ticks: { color: '#6c757d', precision: 0 } },
    },
  };

  constructor() {
    this.rangoCambiado$
      .pipe(debounceTime(400), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.aplicar());

    interval(30_000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (!this.cargando() && this.data()) {
          this.cargar();
        }
      });
  }

  ngOnInit(): void {
    this.aplicarPreset('30d', false);
    this.cargar();
  }

  onDesde(v: string): void {
    if (v) {
      this.desde.set(v);
      this.preset.set('30d');
      this.rangoCambiado$.next();
    }
  }

  onHasta(v: string): void {
    if (v) {
      this.hasta.set(v);
      this.preset.set('30d');
      this.rangoCambiado$.next();
    }
  }

  aplicarPreset(p: PresetSeccion, recargar = true): void {
    this.preset.set(p);
    const hoy = new Date();
    const dias = p === '7d' ? 7 : p === '30d' ? 30 : p === '90d' ? 90 : 365;
    const desde = new Date(hoy);
    desde.setDate(hoy.getDate() - (dias - 1));
    this.desde.set(this.toIso(desde));
    this.hasta.set(this.toIso(hoy));
    if (recargar) {
      this.cargar();
    }
  }

  aplicar(): void {
    if (!this.desde() || !this.hasta()) {
      this.error.set('Selecciona un rango de fechas valido.');
      return;
    }
    if (this.desde() > this.hasta()) {
      this.error.set('La fecha "desde" no puede ser posterior a "hasta".');
      return;
    }
    this.error.set(null);
    this.cargar();
  }

  cargar(): void {
    if (this.cargando()) {
      return;
    }
    this.cargando.set(true);
    this.error.set(null);
    const seq = ++this.peticionSeq;
    this.srv
      .vistasDetalle({ desde: this.desde() || undefined, hasta: this.hasta() || undefined })
      .subscribe({
        next: (d) => {
          if (seq !== this.peticionSeq) {
            return;
          }
          this.data.set(d);
          this.aplicarCharts(d);
          this.cargando.set(false);
        },
        error: (e: HttpErrorResponse) => {
          if (seq !== this.peticionSeq) {
            return;
          }
          this.cargando.set(false);
          this.error.set(
            e.status === 0
              ? 'No se pudo conectar con el servidor.'
              : (e.error?.message ?? 'Error al cargar el detalle de vistas.'),
          );
        },
      });
  }

  private aplicarCharts(d: VistasDetalle): void {
    // ===== Top 10 mas vistos (barras horizontales) =====
    const top: ResultadoMasVisto[] = d.resultadosMasVistos;
    this.chartTopVistos = {
      labels: top.map((r) => this.recortarEtiqueta(r)),
      datasets: [
        {
          data: top.map((r) => r.contadorVistas),
          label: 'Vistas',
          backgroundColor: 'rgba(37, 211, 102, 0.7)',
          hoverBackgroundColor: 'rgba(15, 122, 67, 0.95)',
          borderRadius: 6,
        },
      ],
    };

    // ===== Distribucion buckets (barras verticales) =====
    this.chartBuckets = {
      labels: d.distribucionBuckets.map((b) => b.etiqueta),
      datasets: [
        {
          data: d.distribucionBuckets.map((b) => b.cantidad),
          label: 'Resultados',
          backgroundColor: ['#075e54', '#128c7e', '#25d366', '#fdcb6e'],
          hoverBackgroundColor: ['#043d36', '#075e54', '#128c7e', '#b88a3c'],
          borderRadius: 6,
          maxBarThickness: 80,
        },
      ],
    };

    // ===== Curva temporal (linea) =====
    this.chartCurva = {
      labels: d.curvaTemporal.map((p) => p.fecha),
      datasets: [
        {
          data: d.curvaTemporal.map((p) => p.total),
          label: 'Vistas',
          borderColor: '#25d366',
          backgroundColor: 'rgba(37, 211, 102, 0.15)',
          pointBackgroundColor: '#075e54',
          pointRadius: 3,
          pointHoverRadius: 5,
          tension: 0.35,
          fill: true,
        },
      ],
    };
  }

  /** Etiqueta corta para el top 10: dni + primeras letras del nombre. */
  private recortarEtiqueta(r: ResultadoMasVisto): string {
    const dni = r.documentoIdentidad ?? '';
    return dni.length > 0 ? `DNI ${dni.slice(-6)}` : r.idGenerado.slice(0, 8);
  }

  private toIso(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  /**
   * Pide el .xlsx con el detalle plano de eventos (CLICK/VISTA/DESCARGA) al
   * backend y dispara la descarga en el navegador. Reutiliza el rango activo.
   */
  exportarExcel(): void {
    if (this.exportando()) {
      return;
    }
    this.exportando.set(true);
    this.srv
      .exportarExcelVistas({ desde: this.desde() || undefined, hasta: this.hasta() || undefined })
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `dashboard_vistas_${this.desde() || ''}_${this.hasta() || ''}.xlsx`;
          document.body.appendChild(a);
          a.click();
          a.remove();
          URL.revokeObjectURL(url);
          this.exportando.set(false);
        },
        error: (e: HttpErrorResponse) => {
          this.exportando.set(false);
          this.error.set(
            e.status === 0
              ? 'No se pudo conectar con el servidor.'
              : (e.error?.message ?? 'Error al exportar vistas.'),
          );
        },
      });
  }
}
