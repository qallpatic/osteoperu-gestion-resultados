import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { NgChartsModule } from 'ng2-charts';
import { Subject, debounceTime, interval } from 'rxjs';
import type { ChartConfiguration, ChartData, ChartOptions } from 'chart.js';

import { DashboardService } from '../dashboard.service';
import { DashboardResumen, PuntoSerieDiaria } from '../dashboard.models';
import { FechaCortaPipe } from '../pipes/fecha-corta.pipe';
import { SeccionGenericaComponent, PresetSeccion } from './seccion-generica.component';

const DIAS_SEMANA = ['Dom', 'Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab'];

/**
 * Seccion "Resultados cargados" del Dashboard.
 * Muestra: serie temporal principal + distribucion por dia de la semana +
 * leyendas con totales, promedios, pico y dias sin actividad.
 *
 * Reutiliza el shell visual `SeccionGenericaComponent` para header + filtros.
 */
@Component({
  selector: 'app-dashboard-resultados',
  imports: [NgChartsModule, DatePipe, DecimalPipe, FechaCortaPipe, SeccionGenericaComponent],
  templateUrl: './dashboard-resultados.component.html',
  styleUrl: './dashboard-resultados.component.css',
})
export class DashboardResultadosComponent implements OnInit {
  private readonly srv = inject(DashboardService);
  private readonly destroyRef = inject(DestroyRef);

  readonly cargando = signal(false);
  readonly exportando = signal(false);
  readonly error = signal<string | null>(null);
  readonly data = signal<DashboardResumen | null>(null);

  readonly desde = signal<string>('');
  readonly hasta = signal<string>('');
  readonly preset = signal<PresetSeccion>('30d');
  readonly today = this.toIso(new Date());

  private peticionSeq = 0;
  private readonly rangoCambiado$ = new Subject<void>();

  /** Distribucion calculada en frontend desde la serie diaria (no requiere query nueva). */
  readonly distribucionSemanal = computed(() => {
    const serie = this.data()?.serieResultadosPorDia ?? [];
    return this.calcularDistribucionSemanal(serie);
  });

  /** Total real dentro del rango (suma de la serie). */
  readonly totalEnRango = computed(() =>
    (this.data()?.serieResultadosPorDia ?? []).reduce((acc, p) => acc + p.total, 0),
  );

  /** Promedio diario redondeado a 1 decimal. */
  readonly promedioDiario = computed(() => {
    const serie = this.data()?.serieResultadosPorDia ?? [];
    if (serie.length === 0) {
      return 0;
    }
    const total = serie.reduce((acc, p) => acc + p.total, 0);
    return Math.round((total / serie.length) * 10) / 10;
  });

  /** Dia con mas actividad dentro del rango. */
  readonly diaPico = computed(() => {
    const serie = this.data()?.serieResultadosPorDia ?? [];
    if (serie.length === 0) {
      return null;
    }
    return serie.reduce((acc, p) => (p.total > acc.total ? p : acc));
  });

  /** Cantidad de dias sin actividad (total == 0) dentro del rango. */
  readonly diasSinActividad = computed(() =>
    (this.data()?.serieResultadosPorDia ?? []).filter((p) => p.total === 0).length,
  );

  /** Texto del rango efectivo con pipe aplicado (DD-Mes corto). */
  readonly rangoTexto = computed(() => {
    const d = this.data();
    if (!d) {
      return '';
    }
    const desde = fechaCortaPipeInstance.transform(d.desde);
    const hasta = fechaCortaPipeInstance.transform(d.hasta);
    return `${desde} → ${hasta}`;
  });

  /** Primer load: data null y cargando. */
  readonly esPrimerLoad = computed(() => this.cargando() && !this.data());

  chartResultados: ChartData<'bar'> = { labels: [], datasets: [] };
  chartSemanal: ChartData<'bar'> = { labels: [], datasets: [] };

  readonly chartOptions: ChartOptions<'line' | 'bar'> = {
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
      x: {
        grid: { display: false },
        ticks: {
          color: '#6c757d',
          callback: function (this: { getLabelForValue: (v: number) => string }, value: string | number) {
            const idx = typeof value === 'number' ? value : Number(value);
            return fechaCortaPipeInstance.transform(this.getLabelForValue(idx));
          },
        },
      },
      y: { beginAtZero: true, grid: { color: '#eef3f1' }, ticks: { color: '#6c757d', precision: 0 } },
    },
  };

  readonly chartSemanalOptions: ChartOptions<'bar'> = {
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
        callbacks: {
          title: (items) => `Dia ${items[0]?.label ?? ''}`,
        },
      },
    },
    scales: {
      x: { grid: { display: false }, ticks: { color: '#6c757d' } },
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
      .resumen({ desde: this.desde() || undefined, hasta: this.hasta() || undefined })
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
              : (e.error?.message ?? 'Error al cargar la seccion.'),
          );
        },
      });
  }

  /**
   * Distribuye los totales por dia de la semana (0=Dom, 6=Sab).
   * Como la serie diaria ya viene agregada por dia, solo tenemos que sumar
   * por (getDay() del ISO).
   */
  private calcularDistribucionSemanal(serie: PuntoSerieDiaria[]): { dia: number; total: number }[] {
    const acum = [0, 0, 0, 0, 0, 0, 0];
    for (const p of serie) {
      const d = new Date(`${p.fecha}T00:00:00`);
      if (!Number.isNaN(d.getTime())) {
        acum[d.getDay()] += p.total;
      }
    }
    return acum.map((total, dia) => ({ dia, total }));
  }

  private aplicarCharts(d: DashboardResumen): void {
    const labels = d.serieResultadosPorDia.map((p) => p.fecha);
    this.chartResultados = {
      labels,
      datasets: [
        {
          data: d.serieResultadosPorDia.map((p) => p.total),
          label: 'Resultados',
          backgroundColor: 'rgba(18, 140, 126, 0.65)',
          hoverBackgroundColor: 'rgba(7, 94, 84, 0.95)',
          borderRadius: 6,
          maxBarThickness: 28,
        },
      ],
    };

    const dist = this.distribucionSemanal();
    this.chartSemanal = {
      labels: dist.map((d) => DIAS_SEMANA[d.dia]),
      datasets: [
        {
          data: dist.map((d) => d.total),
          label: 'Resultados',
          backgroundColor: 'rgba(7, 94, 84, 0.7)',
          hoverBackgroundColor: 'rgba(4, 61, 54, 0.95)',
          borderRadius: 6,
          maxBarThickness: 50,
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
   * Pide el .xlsx con el detalle plano de resultados cargados al backend y
   * dispara la descarga en el navegador. Reutiliza el rango activo.
   */
  exportarExcel(): void {
    if (this.exportando()) {
      return;
    }
    this.exportando.set(true);
    this.srv
      .exportarExcelResultados({ desde: this.desde() || undefined, hasta: this.hasta() || undefined })
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `dashboard_resultados_${this.desde() || ''}_${this.hasta() || ''}.xlsx`;
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
              : (e.error?.message ?? 'Error al exportar resultados.'),
          );
        },
      });
  }
}

// Evita warning TS6133 si ChartConfiguration no se usa en este archivo.
type _ChartCfgRef = ChartConfiguration;

// Instancia reusable del pipe para usarla dentro del callback de ticks
// (Chart.js llama fuera del inyector de Angular, asi que no se puede usar
// el pipe via | fechaCorta; se transforma manualmente).
const fechaCortaPipeInstance = new FechaCortaPipe();