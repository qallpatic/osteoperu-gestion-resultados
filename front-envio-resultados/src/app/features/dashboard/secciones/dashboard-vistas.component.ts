import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { NgChartsModule } from 'ng2-charts';
import { Subject, debounceTime, interval } from 'rxjs';
import type { ChartData, ChartOptions } from 'chart.js';

import { DashboardService } from '../dashboard.service';
import { PuntoHeatmap, VistasDetalle } from '../dashboard.models';
import { FechaCortaPipe } from '../pipes/fecha-corta.pipe';
import { SeccionGenericaComponent, PresetSeccion } from './seccion-generica.component';

/**
 * Seccion "Vistas de pacientes" del Dashboard.
 *
 * Muestra dos visualizaciones complementarias:
 * 1. Curva temporal stacked bar (aperturas + descargas por dia): actividad
 *    diaria desglosada para detectar picos/caidas.
 * 2. Heatmap 7x24 (dia-de-la-semana x hora-del-dia): combina dos
 *    dimensiones temporales para revelar patrones circadianos y
 *    semanales ("los jueves a las 14hs siempre hay pico"), util para
 *    optimizar la ventana de envio de SMS.
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
  imports: [DecimalPipe, NgChartsModule, FechaCortaPipe, SeccionGenericaComponent],
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

  /** Etiquetas de dias reordenadas para mostrar L M M J V S D (lunes primero). */
  readonly diasLabels = ['L', 'M', 'M', 'J', 'V', 'S', 'D'];

  /** Etiquetas de dias completas para el tooltip. */
  private readonly diasNombres = ['Domingo', 'Lunes', 'Martes', 'Miercoles', 'Jueves', 'Viernes', 'Sabado'];

  /** Mapeo: indice de fila (0=L, 1=M, ..., 6=D) -> diaSemana MySQL (1=Dom..7=Sab). */
  private readonly filaAdiaSemana = [2, 3, 4, 5, 6, 7, 1];

  /** Etiquetas de horas (0-23) para el eje X del heatmap. */
  readonly horasLabels: readonly string[] = Array.from({ length: 24 }, (_, h) =>
    String(h).padStart(2, '0'),
  );

  readonly rangoTexto = computed(() => {
    const d = this.data();
    if (!d) {
      return '';
    }
    return `${d.desde} → ${d.hasta}`;
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

  /** Mapa rapido (fila, hora) -> total, para el render del heatmap. */
  readonly heatmapMatrix = computed<readonly number[][]>(() => {
    const matriz: number[][] = Array.from({ length: 7 }, () => new Array<number>(24).fill(0));
    const heatmap = this.data()?.heatmap ?? [];
    for (const p of heatmap) {
      const fila = this.filaAdiaSemana.indexOf(p.diaSemana);
      if (fila >= 0 && fila < 7 && p.hora >= 0 && p.hora < 24) {
        matriz[fila][p.hora] = p.total;
      }
    }
    return matriz;
  });

  /** Maximo valor del heatmap; se usa para la escala de color. 1 si todo esta en 0. */
  readonly heatmapMax = computed(() => {
    let max = 0;
    for (const fila of this.heatmapMatrix()) {
      for (const v of fila) {
        if (v > max) {
          max = v;
        }
      }
    }
    return Math.max(max, 1);
  });

  /** Valores precomputados para los 5 escalones de la leyenda (0%, 25%, 50%, 75%, 100% del max). */
  readonly heatmapLeyenda = computed<readonly number[]>(() => {
    const m = this.heatmapMax();
    return [0, Math.round(m * 0.25), Math.round(m * 0.5), Math.round(m * 0.75), m];
  });

  readonly esPrimerLoad = computed(() => this.cargando() && !this.data());

  /** Stacked bar: aperturas del visor (base) + descargas de archivos (segmento superior). */
  chartCurva: ChartData<'bar'> = { labels: [], datasets: [] };

  /** Tooltip state: celda hovereada (null si nada). */
  readonly celdaHover = signal<{ fila: number; hora: number; x: number; y: number } | null>(null);

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

  /** Color de fondo de una celda segun su valor (0..max). Escala sqrt para no ser opacado por outliers. */
  colorPara(valor: number): string {
    if (valor <= 0) {
      return 'rgba(220, 230, 230, 0.6)';
    }
    const ratio = Math.sqrt(valor) / Math.sqrt(this.heatmapMax());
    // Interpolacion entre verde claro (#a8e6c0) y verde oscuro (#0f7a43)
    const r = Math.round(168 + (15 - 168) * ratio);
    const g = Math.round(230 + (122 - 230) * ratio);
    const b = Math.round(192 + (67 - 192) * ratio);
    return `rgb(${r}, ${g}, ${b})`;
  }

  onCeldaEnter(fila: number, hora: number, ev: MouseEvent): void {
    this.celdaHover.set({ fila, hora, x: ev.clientX, y: ev.clientY });
  }

  onCeldaMove(ev: MouseEvent): void {
    const h = this.celdaHover();
    if (h) {
      this.celdaHover.set({ ...h, x: ev.clientX, y: ev.clientY });
    }
  }

  onCeldaLeave(): void {
    this.celdaHover.set(null);
  }

  /** Texto del tooltip para la celda hovereada. */
  readonly tooltipTexto = computed(() => {
    const h = this.celdaHover();
    if (!h) {
      return null;
    }
    const matriz = this.heatmapMatrix();
    const valor = matriz[h.fila]?.[h.hora] ?? 0;
    const dia = this.diasNombres[(this.filaAdiaSemana[h.fila] - 1 + 7) % 7] ?? '?';
    const hh = String(h.hora).padStart(2, '0');
    if (valor === 0) {
      return `${dia} ${hh}:00 · sin actividad`;
    }
    return `${dia} ${hh}:00 · ${valor} ${valor === 1 ? 'interaccion' : 'interacciones'}`;
  });

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
          this.aplicarChartCurva(d);
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

  private toIso(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  /** Arma el stacked bar de la curva temporal (aperturas abajo + descargas arriba). */
  private aplicarChartCurva(d: VistasDetalle): void {
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
