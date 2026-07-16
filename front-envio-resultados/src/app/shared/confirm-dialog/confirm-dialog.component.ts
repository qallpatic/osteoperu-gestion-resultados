import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

/**
 * Dialogo de confirmacion estilizado (icono + titulo + mensaje + 2 botones).
 * Soporta un input de texto opcional (placeholder + valorInicial + emit en aceptar).
 *
 * Uso:
 *   <app-confirm-dialog
 *     [titulo]="..." [mensaje]="..." [detalle]="..."
 *     [inputLabel]="..." [inputPlaceholder]="..." [inputValorInicial]="..."
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
          <label class="cd-field__label">{{ inputLabel }}</label>
          <input #campoInput
                 class="cd-field__input"
                 type="text"
                 [(ngModel)]="inputValor"
                 [placeholder]="inputPlaceholder"
                 [maxlength]="inputMaxlength"
                 (keyup.enter)="emitirConfirmar()" />
        </div>
      }
      <div class="cd-actions">
        <button type="button" class="cd-btn cd-btn--ghost" (click)="cancelar.emit()">
          <i class="fas fa-xmark"></i> {{ textoCancelar }}
        </button>
        <button type="button" class="cd-btn cd-btn--primary" (click)="emitirConfirmar()">
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
  inputValor = '';

  ngOnInit(): void {
    this.inputValor = this.inputValorInicial;
  }

  @Output() confirmar = new EventEmitter<string | undefined>();
  @Output() cancelar = new EventEmitter<void>();

  emitirConfirmar(): void {
    this.confirmar.emit(this.inputLabel ? this.inputValor.trim() : undefined);
  }
}