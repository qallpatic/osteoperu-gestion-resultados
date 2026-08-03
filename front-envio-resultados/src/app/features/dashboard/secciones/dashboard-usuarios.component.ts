import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { NgChartsModule } from 'ng2-charts';
import { Subject, debounceTime, interval } from 'rxjs';
import type { ChartConfiguration, ChartData, ChartOptions } from 'chart.js';

import { DashboardService } from '../dashboard.service';
import { DashboardResumen, PuntoSerieDiaria } from '../dashboard.models';
import { SeccionGenericaComponent, PresetSeccion } from './seccion-generica.component';

const MESES_CORTO = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun',
                     'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];

/**
 * Seccion "Usuarios registrados" del Dashboard.
 * Muestra: serie temporal + distribucion por dia de la semana + crecimiento
 * mensual (acumulado) + leyendas con totales, promedios y pico historico.
 *
 * Reutiliza el shell visual `SeccionGenericaComponent` para header + filtros.
 */
@Component({
  selector: 'app-dashboard-usuarios',
  imports: [NgChartsModule, DatePipe, DecimalPipe, SeccionGenericaComponent],
  templateUrl: './dashboard-usuarios.component.html',
  styleUrl: './dashboard-usuarios.component.css',
})
export class DashboardUsuariosComponent implements OnInit {
  private readonly srv = inject(DashboardService);
  private readonly destroyRef = inject(DestroyRef);

  readonly cargando = signal(false);
  readonly error = signal<string | null>(null);
  readonly data = signal<DashboardResumen | null>(null);

  readonly desde = signal<string>('');
  readonly hasta = signal<string>('');
  readonly preset = signal<PresetSeccion>('30d');
  readonly today = this.toIso(new Date());

  private peticionSeq = 0;
  private readonly rangoCambiado$ = new Subject<void>();

  readonly distribucionSemanal = computed(() => {
    const serie = this.data()?.serieUsuariosPorDia ?? [];
    return this.calcularDistribucionSemanal(serie);
  });

  /** Crecimiento mensual: suma de registros por mes (YYYY-MM). */
  readonly crecimientoMensual = computed(() => {
    const serie = this.data()?.serieUsuariosPorDia ?? [];
    return this.calcularCrecimientoMensual(serie);
  });

  readonly totalEnRango = computed(() =>
    (this.data()?.serieUsuariosPorDia ?? []).reduce((acc, p) => acc + p.total, 0),
  );

  readonly promedioDiario = computed(() => {
    const serie = this.data()?.serieUsuariosPorDia ?? [];
    if (serie.length === 0) {
      return 0;
    }
    const total = serie.reduce((acc, p) => acc + p.total, 0);
    return Math.round((total / serie.length) * 10) / 10;
  });

  readonly rangoTexto = computed(() => {
    const d = this.data();
    return d ? `${d.desde} -> ${d.hasta}` : '';
  });

  readonly esPrimerLoad = computed(() => this.cargando() && !this.data());

  chartUsuarios: ChartData<'line'> = { labels: [], datasets: [] };
  chartSemanal: ChartData<'bar'> = { labels: [], datasets: [] };
  chartMensual: ChartData<'bar'> = { labels: [], datasets: [] };

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

  private calcularCrecimientoMensual(serie: PuntoSerieDiaria[]): { etiqueta: string; total: number }[] {
    const porMes = new Map<string, number>();
    for (const p of serie) {
      const d = new Date(`${p.fecha}T00:00:00`);
      if (Number.isNaN(d.getTime())) {
        continue;
      }
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
      porMes.set(key, (porMes.get(key) ?? 0) + p.total);
    }
    return Array.from(porMes.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([key, total]) => {
        const [yyyy, mm] = key.split('-');
        return { etiqueta: `${MESES_CORTO[Number(mm) - 1]} ${yyyy.slice(2)}`, total };
      });
  }

  private aplicarCharts(d: DashboardResumen): void {
    const labels = d.serieUsuariosPorDia.map((p) => p.fecha);
    this.chartUsuarios = {
      labels,
      datasets: [
        {
          data: d.serieUsuariosPorDia.map((p) => p.total),
          label: 'Usuarios',
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

    const dist = this.distribucionSemanal();
    this.chartSemanal = {
      labels: dist.map((d) => ['Dom', 'Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab'][d.dia]),
      datasets: [
        {
          data: dist.map((d) => d.total),
          label: 'Usuarios',
          backgroundColor: 'rgba(37, 211, 102, 0.7)',
          hoverBackgroundColor: 'rgba(15, 122, 67, 0.95)',
          borderRadius: 6,
          maxBarThickness: 50,
        },
      ],
    };

    const cm = this.crecimientoMensual();
    this.chartMensual = {
      labels: cm.map((c) => c.etiqueta),
      datasets: [
        {
          data: cm.map((c) => c.total),
          label: 'Registros',
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
}

// Evita warning TS6133 si ChartConfiguration no se usa en este archivo.
type _ChartCfgRef = ChartConfiguration;