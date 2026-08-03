import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { DecimalPipe } from '@angular/common';
import { Subject, debounceTime, interval } from 'rxjs';

import { DashboardService } from '../dashboard.service';
import { DashboardResumen } from '../dashboard.models';
import { SeccionGenericaComponent, PresetSeccion } from './seccion-generica.component';
import { formatearPeso } from '../util-format';

/**
 * Seccion "Almacenamiento usado" del Dashboard.
 *
 * Implementacion actual: placeholder "Proximamente". El endpoint detallado
 * (`/api/dashboard/almacenamiento/detalle`) requiere queries nuevas (top 10
 * archivos pesados + distribucion por tipo de contenido + acumulado temporal)
 * que se entregaran en un PR posterior.
 *
 * Mientras tanto muestra los KPIs ya disponibles (peso total + cantidad
 * de archivos) para que la pestana no quede completamente vacia.
 */
@Component({
  selector: 'app-dashboard-almacenamiento',
  imports: [SeccionGenericaComponent, DecimalPipe],
  templateUrl: './dashboard-almacenamiento.component.html',
  styleUrl: './dashboard-almacenamiento.component.css',
})
export class DashboardAlmacenamientoComponent implements OnInit {
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

  readonly pesoFormateado = computed(() => formatearPeso(this.data()?.pesoTotalMb ?? 0));
  readonly rangoTexto = computed(() => {
    const d = this.data();
    return d ? `${d.desde} -> ${d.hasta}` : '';
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
      .resumen({ desde: this.desde() || undefined, hasta: this.hasta() || undefined })
      .subscribe({
        next: (d) => {
          if (seq !== this.peticionSeq) {
            return;
          }
          this.data.set(d);
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

  private toIso(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }
}