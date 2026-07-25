import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

/**
 * Dialogo de confirmacion estilizado (icono + titulo + mensaje + 2 botones).
 * Soporta un input de texto opcional (placeholder + valorInicial + emit en aceptar).
 *
 * Uso:
 *   <app-confirm-dialog
 *     [titulo]="..." [mensaje]="..." [detalle]="..."
 *     [inputLabel]="..." [inputPlaceholder]="..." [inputValorInicial]="..."
 *     [inputObligatorio]="true"
 *     [textoAceptar]="..." (confirmar)="onConfirm($event)" (cancelar)="..." />
 *
 * El output (confirmar) emite un string opcional con el valor del input
 * (string vacio si no se lleno, undefined si no hay input).
 */
@Component({
  selector: 'app-confirm-dialog',
  imports: [FormsModule],
  template: `
    <div class="cd-backdrop" (click)="cancelar.emit()"></div>
    <div class="cd-card" role="alertdialog" aria-modal="true">
      <div class="cd-icon" [class.cd-icon--warn]="variante === 'warn'" [class.cd-icon--danger]="variante === 'danger'">
        <i class="fas" [class.fa-triangle-exclamation]="variante !== 'danger'" [class.fa-circle-exclamation]="variante === 'danger'"></i>
      </div>
      <h3 class="cd-title">{{ titulo }}</h3>
      @if (mensaje) {
        <p class="cd-msg">{{ mensaje }}</p>
      }
      @if (detalle) {
        <div class="cd-detail">{{ detalle }}</div>
      }
      @if (inputLabel) {
        <div class="cd-field">
          <label class="cd-field__label">
            {{ inputLabel }}@if (inputObligatorio) { <span class="cd-req">*</span> }
          </label>
          <input #campoInput
                 class="cd-field__input"
                 [class.is-invalid]="mostrarErrorInput()"
                 type="text"
                 [(ngModel)]="inputValor"
                 [placeholder]="inputPlaceholder"
                 [maxlength]="inputMaxlength"
                 (input)="onInputChange()"
                 (keyup.enter)="emitirConfirmar()" />
          @if (mostrarErrorInput()) {
            <small class="cd-field__error">Este campo es obligatorio.</small>
          }
        </div>
      }
      <div class="cd-actions">
        <button type="button" class="cd-btn cd-btn--ghost" (click)="cancelar.emit()">
          <i class="fas fa-xmark"></i> {{ textoCancelar }}
        </button>
        <button type="button" class="cd-btn cd-btn--primary"
                [disabled]="inputObligatorio && inputValor.trim().length === 0"
                (click)="emitirConfirmar()">
          <i class="fas fa-check"></i> {{ textoAceptar }}
        </button>
      </div>
    </div>
  `,
  styleUrl: './confirm-dialog.component.css',
})
export class ConfirmDialogComponent {
  @Input() titulo = '';
  @Input() mensaje = '';
  @Input() detalle = '';
  @Input() textoCancelar = 'Cancelar';
  @Input() textoAceptar = 'Confirmar';
  @Input() variante: 'warn' | 'danger' = 'warn';

  // Input opcional (ej: motivo de desactivacion)
  @Input() inputLabel = '';
  @Input() inputPlaceholder = '';
  @Input() inputValorInicial = '';
  @Input() inputMaxlength = 500;
  /** Si true, el input es obligatorio: deshabilita Aceptar hasta tener valor y muestra error inline. */
  @Input() inputObligatorio = false;
  inputValor = '';
  /** true si el operador intento confirmar con el input vacio (para mostrar el error). */
  readonly inputTocado = signal(false);

  ngOnInit(): void {
    this.inputValor = this.inputValorInicial;
    // Si el input es obligatorio y arranca vacio, mostrar el error desde el inicio
    // (asi el operador ve de entrada que tiene que llenarlo, sin esperar al click en Aceptar).
    if (this.inputObligatorio && this.inputValor.trim().length === 0) {
      this.inputTocado.set(true);
    }
  }

  @Output() confirmar = new EventEmitter<string | undefined>();
  @Output() cancelar = new EventEmitter<void>();

  mostrarErrorInput(): boolean {
    return this.inputObligatorio && this.inputTocado() && this.inputValor.trim().length === 0;
  }

  onInputChange(): void {
    // Reaparece el error apenas el operador borra todo, para mantener el feedback consistente.
    if (this.inputObligatorio && this.inputValor.trim().length === 0) {
      this.inputTocado.set(true);
    } else {
      this.inputTocado.set(false);
    }
  }

  emitirConfirmar(): void {
    if (this.inputLabel && this.inputObligatorio && this.inputValor.trim().length === 0) {
      this.inputTocado.set(true);
      return;
    }
    this.confirmar.emit(this.inputLabel ? this.inputValor.trim() : undefined);
  }
}