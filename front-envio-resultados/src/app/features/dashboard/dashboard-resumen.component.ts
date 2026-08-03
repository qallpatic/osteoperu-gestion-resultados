import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { NgChartsModule } from 'ng2-charts';
import { Subject, debounceTime, interval } from 'rxjs';
import type { ChartData, ChartOptions } from 'chart.js';

import { DashboardService } from './dashboard.service';
import { DashboardResumen } from './dashboard.models';
import { FechaCortaPipe } from './pipes/fecha-corta.pipe';
import { formatearPeso } from './util-format';

type Preset = '7d' | '30d' | '90d' | '365d';

/** Vista "Resumen" del modulo Dashboard (4 KPIs + 2 charts principales). */
@Component({
  selector: 'app-dashboard-resumen',
  imports: [NgChartsModule, DatePipe, DecimalPipe],
  templateUrl: './dashboard-resumen.component.html',
  styleUrl: './dashboard-resumen.component.css',
})
export class DashboardResumenComponent implements OnInit {
  private readonly srv = inject(DashboardService);
  private readonly destroyRef = inject(DestroyRef);

  readonly cargando = signal(false);
  readonly exportando = signal(false);
  readonly error = signal<string | null>(null);
  readonly data = signal<DashboardResumen | null>(null);

  /** Filtros activos. Las fechas se manejan como string YYYY-MM-DD. */
  readonly desde = signal<string>('');
  readonly hasta = signal<string>('');
  readonly preset = signal<Preset>('30d');
  /** Hoy (YYYY-MM-DD) para limitar el input "Hasta" en el date picker. */
  readonly today = this.toIso(new Date());

  /** Ultima peticion en vuelo; si llega otra, se descarta para no pintar datos viejos. */
  private peticionSeq = 0;

  /** Stream para auto-aplicar el rango tras un cambio manual en los inputs. */
  private readonly rangoCambiado$ = new Subject<void>();

  /** Formateo reactivo del peso total, recalculado cuando cambia la data. */
  readonly pesoFormateado = computed(() => formatearPeso(this.data()?.pesoTotalMb ?? 0));

  readonly rangoTexto = computed(() => {
    const d = this.data();
    if (!d) {
      return '';
    }
    return `${d.desde} → ${d.hasta}`;
  });

  /** Mientras data es null y se esta cargando, mostramos un spinner "primer load". */
  readonly esPrimerLoad = computed(() => this.cargando() && !this.data());

  /**
   * Opciones globales para los graficos. Animacion de 300ms (vs 1000ms por defecto)
   * para que el repintado al cambiar de rango se sienta inmediato.
   *
   * El eje X usa un callback que formatea "2026-07-27" a "27-Jul" para que el
   * usuario identifique el punto rapidamente. Los labels originales (ISO) se
   * conservan en data para que el tooltip pueda mostrar la fecha larga.
   */
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
        callbacks: {
          title: (items) => {
            const raw = items[0]?.label;
            if (!raw) {
              return '';
            }
            const d = new Date(`${raw}T00:00:00`);
            if (Number.isNaN(d.getTime())) {
              return raw;
            }
            return d.toLocaleDateString('es-PE', {
              weekday: 'long',
              day: '2-digit',
              month: 'long',
              year: 'numeric',
            });
          },
        },
      },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: {
          color: '#6c757d',
          maxRotation: 0,
          autoSkipPadding: 16,
          callback: function (this: { getLabelForValue: (v: number) => string }, value: string | number) {
            const idx = typeof value === 'number' ? value : Number(value);
            return fechaCortaPipeInstance.transform(this.getLabelForValue(idx));
          },
        },
      },
      y: {
        beginAtZero: true,
        grid: { color: '#eef3f1' },
        ticks: { color: '#6c757d', precision: 0 },
      },
    },
  };

  /**
   * Objetos ChartData: SE REEMPLAZAN COMPLETOS en cada update (no se mutan).
   * Esto es lo que dispara ngOnChanges en BaseChartDirective y por lo tanto
   * el repintado del canvas. Si mutaramos labels/datasets in-place, Angular
   * no detectaria el cambio (misma referencia) y el grafico quedaria "pegado".
   */
  chartResultados: ChartData<'bar'> = { labels: [], datasets: [] };
  chartUsuarios: ChartData<'line'> = { labels: [], datasets: [] };

  constructor() {
    // Auto-aplicar 400ms despues del ultimo cambio manual en Desde/Hasta.
    this.rangoCambiado$
      .pipe(debounceTime(400), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.aplicar());

    // Auto-refresh cada 30s SOLO mientras el componente esta montado.
    // Si el ADMIN navega a otra vista, el destroyRef corta la suscripcion.
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

  onDesde(event: Event): void {
    const v = (event.target as HTMLInputElement).value;
    if (v) {
      this.desde.set(v);
      this.preset.set('30d');
      this.rangoCambiado$.next();
    }
  }

  onHasta(event: Event): void {
    const v = (event.target as HTMLInputElement).value;
    if (v) {
      this.hasta.set(v);
      this.preset.set('30d');
      this.rangoCambiado$.next();
    }
  }

  aplicarPreset(p: Preset, recargar = true): void {
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

  /**
   * Aplica el rango actual. Se usa tanto desde el boton "Aplicar" como desde
   * el debounce automatico. Si la validacion falla, muestra error y NO consulta.
   */
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
          this.aplicarDatosGraficos(d);
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
              : (e.error?.message ?? 'Error al cargar el dashboard.'),
          );
        },
      });
  }

  /**
   * Reemplaza los objetos chartResultados / chartUsuarios con referencias
   * NUEVAS. ng2-charts detecta el cambio por referencia (ngOnChanges), copia
   * los datos al chart existente via Object.assign y llama a chart.update()
   * para repintar. No necesitamos @ViewChild ni llamadas manuales.
   */
  private aplicarDatosGraficos(d: DashboardResumen): void {
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
  }

  private toIso(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  /**
   * Pide el .xlsx al backend y dispara la descarga en el navegador. Reutiliza
   * el rango activo (mismo que se ve en pantalla) y, si el backend esta
   * cacheado, sale casi instantaneo.
   */
  exportarExcel(): void {
    if (this.exportando()) {
      return;
    }
    this.exportando.set(true);
    this.srv
      .exportarExcel({ desde: this.desde() || undefined, hasta: this.hasta() || undefined })
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `dashboard_${this.desde() || ''}_${this.hasta() || ''}.xlsx`;
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
              : (e.error?.message ?? 'Error al exportar el dashboard.'),
          );
        },
      });
  }
}

// Closure local: el callback de ticks lo necesita para formatear labels.
// Declarado fuera del componente para que sea accesible desde el callback
// de Chart.js sin problemas de `this`. Es `pure: true` y no depende del
// inyector de Angular, asi que se puede instanciar y usar libremente.
const fechaCortaPipeInstance = new FechaCortaPipe();