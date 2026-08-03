import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

export type PresetSeccion = '7d' | '30d' | '90d' | '365d';

/**
 * Shell visual compartido por todas las secciones del Dashboard.
 * Renderiza el header (titulo + subtitulo + rango), la barra de filtros
 * (presets + inputs desde/hasta + boton Aplicar) y dos slots con
 * `select="[graficos]"` y `select="[leyendas]"` para que cada seccion
 * aporte su contenido especifico.
 *
 * Es un componente presentacional: NO hace fetch. La seccion padre es la
 * responsable de cargar los datos y pasarlos a este shell como inputs.
 */
@Component({
  selector: 'app-seccion-generica',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [],
  template: `
    <section class="page">
      <header class="page__head">
        <div>
          <h1>{{ titulo }}</h1>
          @if (subtitulo) {
            <p>{{ subtitulo }}</p>
          }
        </div>
        <div class="page__head-actions">
          <ng-content select="[acciones]" />
          <span class="page__rango">
            <i class="fas fa-calendar-range"></i>
            {{ rangoTexto || '—' }}
          </span>
        </div>
      </header>

      @if (error) {
        <div class="alert alert--err">
          <i class="fas fa-circle-exclamation"></i>
          <span>{{ error }}</span>
        </div>
      }

      <div class="filtros">
        <div class="presets">
          <button type="button" class="preset"
                  [class.preset--on]="preset === '7d'"
                  (click)="presetChange.emit('7d')" [disabled]="cargando">
            7 dias
          </button>
          <button type="button" class="preset"
                  [class.preset--on]="preset === '30d'"
                  (click)="presetChange.emit('30d')" [disabled]="cargando">
            30 dias
          </button>
          <button type="button" class="preset"
                  [class.preset--on]="preset === '90d'"
                  (click)="presetChange.emit('90d')" [disabled]="cargando">
            90 dias
          </button>
          <button type="button" class="preset"
                  [class.preset--on]="preset === '365d'"
                  (click)="presetChange.emit('365d')" [disabled]="cargando">
            1 año
          </button>
        </div>
        <div class="rango">
          <label class="rf">
            <span>Desde</span>
            <input type="date" [value]="desde"
                   (change)="desdeChange.emit(($any($event.target)).value)"
                   [max]="hasta" [disabled]="cargando" />
          </label>
          <label class="rf">
            <span>Hasta</span>
            <input type="date" [value]="hasta"
                   (change)="hastaChange.emit(($any($event.target)).value)"
                   [min]="desde" [max]="today" [disabled]="cargando" />
          </label>
          <button type="button" class="btn btn--primary" (click)="aplicar.emit()" [disabled]="cargando">
            @if (cargando) {
              <i class="fas fa-spinner fa-spin"></i> Cargando...
            } @else {
              <i class="fas fa-magnifying-glass-chart"></i> Aplicar
            }
          </button>
        </div>
      </div>

      <div class="charts charts--seccion">
        <ng-content select="[graficos]" />
      </div>

      <div class="leyendas">
        <ng-content select="[leyendas]" />
      </div>
    </section>
  `,
  styles: `
    :host { display: block; }
    .page { max-width: var(--content-max); margin: 0 auto; padding: 36px 32px 60px; }

    .page__head {
      display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; flex-wrap: wrap;
      margin-bottom: 22px;
    }
    .page__head h1 { color: var(--brand-900); font-size: 1.55rem; font-weight: 700; }
    .page__head p { color: var(--muted); margin-top: 4px; font-size: 0.92rem; }
    .page__head-actions {
      display: inline-flex; align-items: center; gap: 12px; flex-wrap: wrap;
    }
    .page__rango {
      display: inline-flex; align-items: center; gap: 8px;
      background: #fff; border: 1px solid var(--border); border-radius: 999px;
      padding: 8px 14px; color: var(--muted); font-size: 0.85rem;
    }

    .alert {
      display: flex; align-items: center; gap: 10px;
      padding: 12px 15px; border-radius: 10px; font-size: 0.9rem; margin-bottom: 18px;
    }
    .alert--err { background: var(--danger-bg); color: var(--danger); border-left: 4px solid var(--danger); }

    .filtros {
      display: flex; flex-wrap: wrap; gap: 18px; align-items: flex-end;
      justify-content: space-between; margin-bottom: 22px;
    }
    .presets { display: flex; gap: 8px; flex-wrap: wrap; }
    .preset {
      background: #fff; border: 1.5px solid var(--border); border-radius: 999px;
      padding: 8px 16px; font-family: inherit; font-size: 0.85rem; color: var(--muted);
      cursor: pointer; transition: all 0.15s;
    }
    .preset:hover:not(:disabled) { border-color: var(--brand-300); color: var(--brand-700); }
    .preset--on {
      background: linear-gradient(135deg, var(--brand-700), var(--brand-500));
      border-color: transparent; color: #fff;
    }
    .preset:disabled { opacity: 0.55; cursor: not-allowed; }

    .rango { display: flex; gap: 10px; align-items: flex-end; flex-wrap: wrap; }
    .rf { display: flex; flex-direction: column; gap: 5px; }
    .rf span { font-size: 0.78rem; color: var(--brand-700); font-weight: 500; }
    .rf input {
      padding: 10px 12px; border: 1.5px solid var(--border); border-radius: 10px;
      font-family: inherit; font-size: 0.9rem; background: #fff;
    }
    .rf input:focus { outline: none; border-color: var(--brand-700); box-shadow: 0 0 0 3px rgba(7, 94, 84, 0.12); }
    .rf input:disabled { background: var(--brand-050); color: var(--muted-2); }

    .btn {
      display: inline-flex; align-items: center; gap: 8px;
      padding: 10px 16px; border-radius: 10px; border: none;
      font-weight: 600; font-size: 0.9rem; cursor: pointer;
      font-family: inherit; transition: transform 0.15s, box-shadow 0.15s, background 0.15s, opacity 0.15s;
    }
    .btn:disabled { opacity: 0.55; cursor: not-allowed; }
    .btn--primary { background: linear-gradient(135deg, var(--brand-700), var(--brand-500)); color: #fff; box-shadow: 0 5px 16px rgba(7, 94, 84, 0.3); }
    .btn--primary:hover:not(:disabled) { transform: translateY(-1px); }
    .btn--ghost { background: #fff; color: var(--brand-700); border: 1.5px solid var(--border); box-shadow: none; }
    .btn--ghost:hover:not(:disabled) { transform: translateY(-1px); border-color: var(--brand-300); }

    .charts--seccion {
      display: grid; gap: 18px;
      grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
      margin-bottom: 22px;
    }
    .leyendas { display: grid; gap: 18px; }
  `,
})
export class SeccionGenericaComponent {
  @Input({ required: true }) titulo!: string;
  @Input() subtitulo = '';
  @Input() rangoTexto = '';
  @Input() cargando = false;
  @Input() error: string | null = null;
  @Input() desde = '';
  @Input() hasta = '';
  @Input() today = '';
  @Input() preset: PresetSeccion = '30d';

  @Output() readonly presetChange = new EventEmitter<PresetSeccion>();
  @Output() readonly aplicar = new EventEmitter<void>();
  @Output() readonly desdeChange = new EventEmitter<string>();
  @Output() readonly hastaChange = new EventEmitter<string>();
}