import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { NgChartsModule } from 'ng2-charts';
import { Subject, debounceTime, interval } from 'rxjs';
import type { ChartData, ChartOptions } from 'chart.js';

import { DashboardService } from '../dashboard.service';
import { AlmacenamientoDetalle, ArchivoPesado, DistribucionTipoContenido } from '../dashboard.models';
import { SeccionGenericaComponent, PresetSeccion } from './seccion-generica.component';
import { formatearPeso } from '../util-format';

/**
 * Seccion "Almacenamiento usado" del Dashboard.
 *
 * Grafica: distribucion por MIME type (doughnut) + top 10 archivos mas
 * pesados (barras horizontales) + curva acumulada de MB a lo largo del
 * tiempo (linea). Reutiliza el shell `SeccionGenericaComponent`.
 */
@Component({
  selector: 'app-dashboard-almacenamiento',
  imports: [NgChartsModule, DatePipe, DecimalPipe, SeccionGenericaComponent],
  templateUrl: './dashboard-almacenamiento.component.html',
  styleUrl: './dashboard-almacenamiento.component.css',
})
export class DashboardAlmacenamientoComponent implements OnInit {
  private readonly srv = inject(DashboardService);
  private readonly destroyRef = inject(DestroyRef);

  readonly cargando = signal(false);
  readonly exportando = signal(false);
  readonly error = signal<string | null>(null);
  readonly data = signal<AlmacenamientoDetalle | null>(null);

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

  readonly pesoTotalFormateado = computed(() =>
    formatearPeso(this.data()?.acumuladoMb.at(-1)?.mbAcumulado ?? 0),
  );

  readonly totalArchivosEnRango = computed(() => {
    const d = this.data();
    if (!d || d.acumuladoMb.length === 0) {
      return 0;
    }
    return d.distribucionPorTipo.reduce((acc, t) => acc + t.cantidad, 0);
  });

  readonly esPrimerLoad = computed(() => this.cargando() && !this.data());

  chartDistribucion: ChartData<'doughnut'> = { labels: [], datasets: [] };
  chartTopArchivos: ChartData<'bar'> = { labels: [], datasets: [] };
  chartAcumulado: ChartData<'line'> = { labels: [], datasets: [] };

  readonly chartDistribucionOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '60%',
    plugins: {
      legend: { position: 'right', labels: { boxWidth: 12, font: { size: 11 } } },
      tooltip: {
        backgroundColor: '#043d36',
        titleColor: '#fff',
        bodyColor: '#e6f2f0',
        borderColor: '#075e54',
        borderWidth: 1,
        padding: 10,
        callbacks: {
          label: (ctx) => {
            const value = Number(ctx.parsed);
            const ds = ctx.dataset.data as number[];
            const total = ds.reduce((acc, v) => acc + Number(v), 0) || 1;
            const pct = ((value / total) * 100).toFixed(1);
            const label = ctx.label ?? '';
            return `${label}: ${formatearPeso(value).texto} (${pct}%)`;
          },
        },
      },
    },
  };

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
        callbacks: {
          label: (ctx) => {
            const v = Number(ctx.parsed.x);
            return formatearPeso(v).texto;
          },
        },
      },
    },
    scales: {
      x: {
        beginAtZero: true,
        grid: { color: '#eef3f1' },
        ticks: {
          color: '#6c757d',
          callback: (val) => formatearPeso(Number(val)).texto,
        },
      },
      y: { grid: { display: false }, ticks: { color: '#6c757d', autoSkip: false } },
    },
  };

  readonly chartAcumuladoOptions: ChartOptions<'line'> = {
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
          label: (ctx) => `${ctx.label}: ${formatearPeso(Number(ctx.parsed.y)).texto}`,
        },
      },
    },
    scales: {
      x: { grid: { display: false }, ticks: { color: '#6c757d', maxTicksLimit: 12 } },
      y: {
        beginAtZero: true,
        grid: { color: '#eef3f1' },
        ticks: {
          color: '#6c757d',
          callback: (val) => formatearPeso(Number(val)).texto,
        },
      },
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
      .almacenamientoDetalle({ desde: this.desde() || undefined, hasta: this.hasta() || undefined })
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
              : (e.error?.message ?? 'Error al cargar el detalle de almacenamiento.'),
          );
        },
      });
  }

  private aplicarCharts(d: AlmacenamientoDetalle): void {
    // ===== Distribucion (doughnut) =====
    const dist = d.distribucionPorTipo;
    this.chartDistribucion = {
      labels: dist.map((t) => this.cortoTipo(t)),
      datasets: [
        {
          data: dist.map((t) => Number(t.pesoMb)),
          backgroundColor: ['#075e54', '#128c7e', '#25d366', '#34b7f1', '#fdcb6e', '#d63031', '#a29bfe', '#fd79a8'],
          borderWidth: 2,
          borderColor: '#fff',
        },
      ],
    };

    // ===== Top 10 archivos pesados (barras horizontales) =====
    const top: ArchivoPesado[] = d.archivosMasPesados;
    this.chartTopArchivos = {
      labels: top.map((a) => this.recortarNombre(a.nombreOriginal)),
      datasets: [
        {
          data: top.map((a) => Number(a.pesoMb)),
          label: 'Peso',
          backgroundColor: 'rgba(7, 94, 84, 0.7)',
          hoverBackgroundColor: 'rgba(4, 61, 54, 0.95)',
          borderRadius: 6,
        },
      ],
    };

    // ===== Acumulado MB (linea) =====
    this.chartAcumulado = {
      labels: d.acumuladoMb.map((p) => p.fecha),
      datasets: [
        {
          data: d.acumuladoMb.map((p) => Number(p.mbAcumulado)),
          label: 'MB acumulados',
          borderColor: '#075e54',
          backgroundColor: 'rgba(7, 94, 84, 0.15)',
          pointBackgroundColor: '#075e54',
          pointRadius: 3,
          pointHoverRadius: 5,
          tension: 0.3,
          fill: true,
        },
      ],
    };
  }

  /** Acorta un MIME type para la leyenda: "application/pdf" -> "pdf". */
  private cortoTipo(t: DistribucionTipoContenido): string {
    const slash = t.tipoContenido.lastIndexOf('/');
    return slash >= 0 ? t.tipoContenido.substring(slash + 1) : t.tipoContenido;
  }

  private recortarNombre(n: string, max = 36): string {
    if (n.length <= max) {
      return n;
    }
    return n.substring(0, max - 1) + '...';
  }

  private toIso(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  /**
   * Pide el .xlsx con el detalle plano de archivos al backend y dispara la
   * descarga en el navegador. Reutiliza el rango activo.
   */
  exportarExcel(): void {
    if (this.exportando()) {
      return;
    }
    this.exportando.set(true);
    this.srv
      .exportarExcelAlmacenamiento({ desde: this.desde() || undefined, hasta: this.hasta() || undefined })
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `dashboard_almacenamiento_${this.desde() || ''}_${this.hasta() || ''}.xlsx`;
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
              : (e.error?.message ?? 'Error al exportar almacenamiento.'),
          );
        },
      });
  }
}
