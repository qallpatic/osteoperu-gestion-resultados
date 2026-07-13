import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { ResultadosService } from './resultados.service';

interface PickedFile {
  file: File;
  name: string;
  size: number;
  kind: 'pdf' | 'image' | 'video';
}

/** Datos con los que se puede precargar el formulario (desde la campanita). */
export interface UploadPrefill {
  dni?: string;
  nombre?: string | null;
  telefono?: string | null;
}

interface Confirmacion {
  nombreReniec: string | null;
  nombrePdf: string | null;
}

/** Límite por archivo y tipos aceptados (deben coincidir con el backend). */
const MAX_MB = 50;
const MAX_BYTES = MAX_MB * 1024 * 1024;
const ACCEPTED = ['application/pdf', 'image/', 'video/'];

@Component({
  selector: 'app-upload',
  imports: [FormsModule],
  templateUrl: './upload.component.html',
  styleUrl: './upload.component.css',
})
export class UploadComponent implements OnInit {
  private readonly resultados = inject(ResultadosService);

  /** Precarga (dni/nombre/telefono) cuando se abre desde una notificación. */
  @Input() prefill: UploadPrefill | null = null;
  /** Se emite cuando la carga fue exitosa (para cerrar el modal y refrescar). */
  @Output() enviado = new EventEmitter<void>();

  readonly maxMb = MAX_MB;

  phone = '';
  dni = '';
  patientName = '';
  mensaje = '';
  files = signal<PickedFile[]>([]);
  dragging = signal(false);
  sending = signal(false);
  notice = signal<{ type: 'ok' | 'error'; text: string } | null>(null);

  /** Autocompletado de nombre por RENIEC. */
  buscandoNombre = signal(false);
  private ultimoDniConsultado = '';

  /** Datos del diálogo de confirmación cuando el nombre del PDF no coincide con RENIEC. */
  pendiente = signal<Confirmacion | null>(null);

  ngOnInit(): void {
    if (this.prefill) {
      this.dni = this.prefill.dni ?? '';
      this.patientName = this.prefill.nombre ?? '';
      this.phone = this.normalizarTelefono(this.prefill.telefono);
      this.ultimoDniConsultado = this.dni; // evita re-consultar RENIEC de inmediato
    }
  }

  /** Deja el teléfono en 9 dígitos (quita el prefijo 51 si viene). */
  private normalizarTelefono(t?: string | null): string {
    if (!t) {
      return '';
    }
    const d = t.replace(/\D/g, '');
    if (d.length === 11 && d.startsWith('51')) {
      return d.slice(2);
    }
    return d.length <= 9 ? d : d.slice(-9);
  }

  phoneValid(): boolean {
    // Telefono opcional: vacio es valido, cuando hay valor exige 9 digitos.
    return this.phone === '' || /^\d{9}$/.test(this.phone);
  }

  dniValid(): boolean {
    return /^\d{8}$/.test(this.dni);
  }

  /** Al completar el DNI, trae el nombre desde RENIEC y autocompleta el campo. */
  autocompletarNombre(): void {
    if (!this.dniValid() || this.dni === this.ultimoDniConsultado) {
      return;
    }
    this.ultimoDniConsultado = this.dni;
    this.buscandoNombre.set(true);
    this.resultados.consultarReniec(this.dni).subscribe({
      next: (r) => {
        this.buscandoNombre.set(false);
        if (r.nombre) {
          this.patientName = r.nombre;
        }
      },
      error: () => this.buscandoNombre.set(false),
    });
  }

  canSend(): boolean {
    return this.phoneValid() && this.dniValid() && this.files().length > 0;
  }

  onBrowse(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.addFiles(input.files);
    input.value = '';
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    this.addFiles(event.dataTransfer?.files ?? null);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(true);
  }

  private addFiles(list: FileList | null): void {
    if (!list) {
      return;
    }
    const accepted: PickedFile[] = [];
    const rejected: string[] = [];

    for (const file of Array.from(list)) {
      if (!ACCEPTED.some((t) => file.type.startsWith(t))) {
        rejected.push(`${file.name}: formato no permitido`);
      } else if (file.size > MAX_BYTES) {
        rejected.push(`${file.name}: supera ${MAX_MB} MB`);
      } else {
        accepted.push({
          file,
          name: file.name,
          size: file.size,
          kind: file.type.startsWith('image/')
            ? 'image'
            : file.type.startsWith('video/')
              ? 'video'
              : 'pdf',
        });
      }
    }

    if (accepted.length) {
      this.files.update((f) => [...f, ...accepted]);
    }
    this.notice.set(rejected.length ? { type: 'error', text: rejected.join(' · ') } : null);
  }

  remove(index: number): void {
    this.files.update((f) => f.filter((_, i) => i !== index));
  }

  formatSize(bytes: number): string {
    return bytes < 1024 * 1024
      ? `${(bytes / 1024).toFixed(0)} KB`
      : `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  send(confirmar = false): void {
    if (!this.canSend() || this.sending()) {
      return;
    }
    this.sending.set(true);
    this.notice.set(null);

    this.resultados
      .upload({
        phone: this.phone,
        dni: this.dni,
        patientName: this.patientName.trim(),
        mensaje: this.mensaje.trim(),
        files: this.files().map((f) => f.file),
        confirmarNombre: confirmar,
      })
      .subscribe({
        next: (res) => {
          this.sending.set(false);
          if (res.requiereConfirmacion) {
            // El nombre del PDF no coincide con RENIEC: se pide confirmación.
            this.pendiente.set({ nombreReniec: res.nombreReniec, nombrePdf: res.nombrePdf });
            return;
          }
          this.pendiente.set(null);
          this.notice.set({
            type: 'ok',
            text: `Resultados enviados al paciente. Se generó el link y se encoló el SMS (${res.smsStatus}).`,
          });
          this.reset();
          this.enviado.emit();
        },
        error: (err: HttpErrorResponse) => {
          this.sending.set(false);
          this.notice.set({ type: 'error', text: this.errorMessage(err) });
        },
      });
  }

  confirmarCarga(): void {
    this.pendiente.set(null);
    this.send(true);
  }

  cancelarConfirmacion(): void {
    this.pendiente.set(null);
    this.notice.set({ type: 'error', text: 'Carga cancelada. Verifica el DNI y el PDF del paciente.' });
  }

  private reset(): void {
    this.phone = '';
    this.dni = '';
    this.patientName = '';
    this.mensaje = '';
    this.files.set([]);
  }

  private errorMessage(err: HttpErrorResponse): string {
    if (err.status === 0) {
      return 'No se pudo conectar con el servidor. Verifica tu conexión.';
    }
    if (err.status === 401 || err.status === 403) {
      return 'No tienes permisos para cargar resultados.';
    }
    return err.error?.message ?? 'Ocurrió un error al enviar los resultados.';
  }
}
