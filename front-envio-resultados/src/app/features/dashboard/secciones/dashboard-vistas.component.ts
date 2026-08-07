import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { NgChartsModule } from 'ng2-charts';
import { Subject, debounceTime, interval } from 'rxjs';
import type { ChartData, ChartOptions } from 'chart.js';

import { DashboardService } from '../dashboard.service';
import { VistasDetalle } from '../dashboard.models';
import { FechaCortaPipe } from '../pipes/fecha-corta.pipe';
import { SeccionGenericaComponent, PresetSeccion } from './seccion-generica.component';

/**
 * Seccion "Vistas de pacientes" del Dashboard.
 *
 * Muestra 2 graficos:
 * 1. Curva temporal stacked bar (aperturas del visor abajo + descargas
 *    arriba): actividad diaria desglosada para detectar picos/caidas.
 * 2. Distribucion horaria (24 barras, 0-23): a que hora del dia concentran
 *    los pacientes la revision de sus resultados. Util para optimizar la
 *    ventana de envio de SMS.
 *
 * KPIs accionables:
 * - Aperturas del visor
 * - Descargas de archivos
 * - Ratio descarga / apertura
 * - Hora pico (ej: "14:00 · 14 interacciones")
 *
 * Reutiliza el shell `SeccionGenericaComponent`.
 */
@Component({
  selector: 'app-dashboard-vistas',
  imports: [NgChartsModule, DatePipe, DecimalPipe, FechaCortaPipe, SeccionGenericaComponent],
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
    if (!d) {
      return '';
    }
    const desde = fechaCortaPipeInstance.transform(d.desde);
    const hasta = fechaCortaPipeInstance.transform(d.hasta);
    return `${desde} → ${hasta}`;
  });

  readonly totalAperturasEnRango = computed(() => this.data()?.totalAperturas ?? 0);
  readonly totalDescargasEnRango = computed(() => this.data()?.totalDescargas ?? 0);
  readonly ratioDescargasPorApertura = computed(() => this.data()?.ratioDescargaPorApertura ?? 0);

  /** Hora pico formateada "HH:00 · N interacciones" o "—" si no hubo actividad. */
  readonly horaPicoTexto = computed(() => {
    const d = this.data();
    if (!d || d.horaPico === null || d.totalHoraPico === null) {
      return '—';
    }
    const hh = String(d.horaPico).padStart(2, '0');
    return `${hh}:00 · ${d.totalHoraPico.toLocaleString('es-PE')} interacciones`;
  });

  readonly esPrimerLoad = computed(() => this.cargando() && !this.data());

  /** Stacked bar: aperturas abajo + descargas arriba. */
  chartCurva: ChartData<'bar'> = { labels: [], datasets: [] };
  chartDistribucionHoraria: ChartData<'bar'> = { labels: [], datasets: [] };

  readonly chartCurvaOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: { mode: 'index', intersect: false },
    animation: { duration: 300 },
    plugins: {
      legend: {
        display: true,
        position: 'bottom',
        labels: { color: '#6c757d', boxWidth: 12, padding: 12 },
      },
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
      x: {
        stacked: true,
        grid: { display: false },
        ticks: {
          color: '#6c757d',
          maxTicksLimit: 12,
          callback: function (this: { getLabelForValue: (v: number) => string }, value: string | number) {
            const idx = typeof value === 'number' ? value : Number(value);
            return fechaCortaPipeInstance.transform(this.getLabelForValue(idx));
          },
        },
      },
      y: {
        stacked: true,
        beginAtZero: true,
        grid: { color: '#eef3f1' },
        ticks: { color: '#6c757d', precision: 0 },
      },
    },
  };

  readonly chartDistribucionHorariaOptions: ChartOptions<'bar'> = {
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
        callbacks: {
          title: (items) => {
            const hora = items[0]?.label ?? '';
            return `${hora}:00`;
          },
          label: (item) => {
            const n = item.parsed.y ?? 0;
            return ` ${n} ${n === 1 ? 'interacción' : 'interacciones'}`;
          },
        },
      },
    },
    scales: {
      x: { grid: { display: false }, ticks: { color: '#6c757d', autoSkip: true, maxTicksLimit: 12 } },
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
    // ===== Curva temporal (stacked bar: aperturas abajo + descargas arriba) =====
    this.chartCurva = {
      labels: d.curvaTemporal.map((p) => p.fecha),
      datasets: [
        {
          data: d.curvaTemporalAperturas.map((p) => p.total),
          label: 'Aperturas del visor',
          backgroundColor: 'rgba(37, 211, 102, 0.75)',
          hoverBackgroundColor: 'rgba(15, 122, 67, 0.95)',
          borderRadius: 4,
          stack: 'interacciones',
        },
        {
          data: d.curvaTemporalDescargas.map((p) => p.total),
          label: 'Descargas de archivos',
          backgroundColor: 'rgba(253, 203, 110, 0.85)',
          hoverBackgroundColor: 'rgba(184, 138, 60, 0.95)',
          borderRadius: 4,
          stack: 'interacciones',
        },
      ],
    };

    // ===== Distribucion horaria (24 barras, 0-23) =====
    this.chartDistribucionHoraria = {
      labels: d.distribucionHoraria.map((p) => String(p.hora).padStart(2, '0')),
      datasets: [
        {
          data: d.distribucionHoraria.map((p) => p.total),
          label: 'Interacciones por hora',
          backgroundColor: d.distribucionHoraria.map((p) =>
            d.horaPico !== null && p.hora === d.horaPico
              ? 'rgba(7, 94, 84, 0.95)'
              : 'rgba(37, 211, 102, 0.65)',
          ),
          hoverBackgroundColor: 'rgba(15, 122, 67, 0.95)',
          borderRadius: 4,
        },
      ],
    };
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

// Instancia reusable del pipe para usarla dentro del callback de ticks
// (Chart.js llama fuera del inyector de Angular, asi que no se puede usar
// el pipe via | fechaCorta; se transforma manualmente).
const fechaCortaPipeInstance = new FechaCortaPipe();
