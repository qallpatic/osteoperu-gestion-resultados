import { AfterViewInit, ChangeDetectorRef, Component, OnDestroy, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { BaseChartDirective } from 'ng2-charts';
import { NgChartsModule } from 'ng2-charts';
import type { ChartConfiguration, ChartData, ChartOptions } from 'chart.js';

import { DashboardService } from './dashboard.service';
import { DashboardResumen } from './dashboard.models';

type Preset = '7d' | '30d' | '90d' | '365d';

/** Dashboard con métricas de la plataforma. Acceso restringido a rol ADMIN. */
@Component({
  selector: 'app-dashboard',
  imports: [NgChartsModule, DatePipe, DecimalPipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly srv = inject(DashboardService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly cargando = signal(false);
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

  onDesde(event: Event): void {
    const v = (event.target as HTMLInputElement).value;
    if (v) {
      this.desde.set(v);
      this.preset.set('30d');
    }
  }

  onHasta(event: Event): void {
    const v = (event.target as HTMLInputElement).value;
    if (v) {
      this.hasta.set(v);
      this.preset.set('30d');
    }
  }

  readonly rangoTexto = computed(() => {
    const d = this.data();
    if (!d) {
      return '';
    }
    return `${d.desde} → ${d.hasta}`;
  });

  /**
   * Opciones globales para los graficos. Animacion de 300ms (vs 1000ms por defecto)
   * para que el repintado al cambiar de rango se sienta inmediato.
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
      },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { color: '#6c757d', maxRotation: 0, autoSkipPadding: 16 },
      },
      y: {
        beginAtZero: true,
        grid: { color: '#eef3f1' },
        ticks: { color: '#6c757d', precision: 0 },
      },
    },
  };

  readonly chartResultados: ChartData<'bar'> = { labels: [], datasets: [] };
  readonly chartUsuarios: ChartData<'line'> = { labels: [], datasets: [] };

  /** Referencias a las directivas para forzar `chart.update('none')` sin animacion. */
  @ViewChild('chartRes') private chartRes?: BaseChartDirective;
  @ViewChild('chartUsu') private chartUsu?: BaseChartDirective;

  ngOnInit(): void {
    this.aplicarPreset('30d', false);
    this.cargar();
  }

  ngAfterViewInit(): void {
    // Nada que hacer aca; las directivas ya estan disponibles via @ViewChild.
  }

  ngOnDestroy(): void {
    // Marcamos seq para descartar respuestas tardias.
    this.peticionSeq++;
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

  aplicar(): void {
    if (!this.desde() || !this.hasta()) {
      this.error.set('Selecciona un rango de fechas valido.');
      return;
    }
    if (this.desde() > this.hasta()) {
      this.error.set('La fecha "desde" no puede ser posterior a "hasta".');
      return;
    }
    this.preset.set('30d');
    this.cargar();
  }

  cargar(): void {
    // Evita requests duplicados si el usuario clickea varios presets rapido.
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
          // Si una peticion posterior se inicio mientras esperabamos esta,
          // descartamos la respuesta vieja para no pintar datos incorrectos.
          if (seq !== this.peticionSeq) {
            return;
          }
          this.data.set(d);
          this.aplicarDatosGraficos(d);
          this.cargando.set(false);
          this.cdr.markForCheck();
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
   * Reemplaza los datos del chart. Cuando los @ViewChild ya estan disponibles
   * (ngAfterViewInit), forzamos `chart.update('none')` para saltarnos la
   * animacion y repintar al instante. Antes del primer ngAfterViewInit el
   * ng2-charts detectara el cambio por si solo.
   */
  private aplicarDatosGraficos(d: DashboardResumen): void {
    const labels = d.serieResultadosPorDia.map((p) => p.fecha);

    this.chartResultados.labels = labels;
    this.chartResultados.datasets = [
      {
        data: d.serieResultadosPorDia.map((p) => p.total),
        label: 'Resultados',
        backgroundColor: 'rgba(18, 140, 126, 0.65)',
        hoverBackgroundColor: 'rgba(7, 94, 84, 0.95)',
        borderRadius: 6,
        maxBarThickness: 28,
      },
    ];

    this.chartUsuarios.labels = labels;
    this.chartUsuarios.datasets = [
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
    ];

    const c1 = this.chartRes?.chart;
    const c2 = this.chartUsu?.chart;
    if (c1 && typeof c1.update === 'function') {
      c1.update('none');
    }
    if (c2 && typeof c2.update === 'function') {
      c2.update('none');
    }
  }

  private toIso(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }
}

// type export para que TS no marque ChartConfiguration como unused en strict.
export type _ChartConfigurationRef = ChartConfiguration;