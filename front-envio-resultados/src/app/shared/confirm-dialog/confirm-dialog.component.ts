import { Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * Dialogo de confirmacion estilizado (icono + titulo + mensaje + 2 botones).
 * Patron: el padre controla la visibilidad con un signal; emite (confirmar) o (cancelar).
 *
 * Uso:
 *   @if (confirmacion(); as c) {
 *     <app-confirm-dialog
 *       [icono]="c.icono" [titulo]="c.titulo" [mensaje]="c.mensaje"
 *       [textoCancelar]="c.textoCancelar" [textoAceptar]="c.textoAceptar"
 *       [variante]="c.variante"
 *       (confirmar)="..." (cancelar)="..." />
 *   }
 */
@Component({
  selector: 'app-confirm-dialog',
  imports: [],
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
      <div class="cd-actions">
        <button type="button" class="cd-btn cd-btn--ghost" (click)="cancelar.emit()">
          <i class="fas fa-xmark"></i> {{ textoCancelar }}
        </button>
        <button type="button" class="cd-btn cd-btn--primary" (click)="confirmar.emit()">
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

  @Output() confirmar = new EventEmitter<void>();
  @Output() cancelar = new EventEmitter<void>();
}