import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { ResultadosService } from './resultados.service';
import { sanitizarDocumento } from '../../core/utils/util-documento';

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
  /** Marca si el usuario interactuo con el textarea de mensaje (para mostrar el hint solo cuando aplica). */
  mensajeTocado = signal(false);

  /** Autocompletado de nombre por RENIEC. */
  buscandoNombre = signal(false);
  private ultimoDniConsultado = '';
  /** DNI con el que se abrio el formulario (prefill del campanita). Sirve para
   *  no pisar los datos que ya llegaron del prefill mientras el operador no
   *  cambie el DNI. */
  private dniPrefill: string | null = null;

  /** Datos del diálogo de confirmación cuando el nombre del PDF no coincide con RENIEC. */
  pendiente = signal<Confirmacion | null>(null);

  ngOnInit(): void {
    if (this.prefill) {
      this.dni = this.prefill.dni ?? '';
      this.patientName = this.prefill.nombre ?? '';
      this.phone = this.normalizarTelefono(this.prefill.telefono);
      this.ultimoDniConsultado = this.dni; // evita re-consultar RENIEC de inmediato
      this.dniPrefill = this.dni;
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

  /** Da formato visual "### ### ###" a un telefono (9 digitos o menos). */
  formatPhoneDisplay(value: string): string {
    const digits = this.normalizarTelefono(value);
    const a = digits.slice(0, 3);
    const b = digits.slice(3, 6);
    const c = digits.slice(6, 9);
    return [a, b, c].filter(Boolean).join(' ');
  }

  /** Handler de input para el telefono: normaliza y reformatea (cubre paste con +51 o 51). */
  onPhoneInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.phone = this.normalizarTelefono(input.value);
    input.value = this.formatPhoneDisplay(this.phone);
  }

  /** Handler de input para el DNI: permite solo letras y numeros (mayusculas) y limita a 12 caracteres. */
  onDniInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.dni = sanitizarDocumento(input.value, 12);
    input.value = this.dni;
  }

  phoneValid(): boolean {
    return this.phone === '' || /^\d{9}$/.test(this.phone);
  }

  dniValid(): boolean {
    return /^[A-Z0-9]{8,12}$/.test(this.dni);
  }

  /** El mensaje es obligatorio (los adjuntos pueden ir por link externo). */
  mensajeValido(): boolean {
    return this.mensaje.trim().length > 0;
  }

  /** Marca el textarea como tocado al primer input/blur. */
  onMensajeInteraccion(): void {
    this.mensajeTocado.set(true);
  }

  /**
   * Al completar el DNI, intenta autocompletar nombre y telefono con lo que
   * el operador ya guardo en PACIENTE (via Gestion de usuarios / Admisión).
   * Si PACIENTE tiene nombre, ese manda (puede tener sufijos o correcciones
   * que RENIEC no conoce, ej: "_TEST"). Solo si PACIENTE no tiene nombre cae
   * a RENIEC para recuperar el nombre oficial.
   *
   * Reglas:
   *  - Si el DNI tipeado coincide con el del prefill inicial, no consulta:
   *    los datos ya estan en el form.
   *  - Si el DNI ya fue consultado antes en esta sesion, tampoco consulta
   *    (asi respetamos ediciones manuales del operador sobre el resultado).
   *  - Si es un DNI nuevo (distinto del prefill y del ultimo consultado),
   *    consulta y sobrescribe nombre/telefono con lo encontrado.
   */
  autocompletarNombre(): void {
    if (!this.dniValid()) {
      return;
    }
    if (this.dni === this.dniPrefill) {
      this.ultimoDniConsultado = this.dni;
      return;
    }
    if (this.dni === this.ultimoDniConsultado) {
      return;
    }
    this.ultimoDniConsultado = this.dni;
    this.buscandoNombre.set(true);
    this.resultados.buscarPaciente(this.dni).subscribe({
      next: (p) => {
        if (this.dni !== this.ultimoDniConsultado) {
          this.buscandoNombre.set(false);
          return;
        }
        let nombreResuelto: string | null = p.nombre;
        if (!nombreResuelto && this.dni.length === 8) {
          this.resultados.consultarReniec(this.dni).subscribe({
            next: (r) => {
              this.buscandoNombre.set(false);
              if (r.nombre) {
                this.patientName = r.nombre;
              }
              if (p.telefono) {
                this.phone = this.normalizarTelefono(p.telefono);
              }
            },
            error: () => this.buscandoNombre.set(false),
          });
          return;
        }
        this.buscandoNombre.set(false);
        if (nombreResuelto) {
          this.patientName = nombreResuelto;
        }
        if (p.telefono) {
          this.phone = this.normalizarTelefono(p.telefono);
        }
      },
      error: () => this.buscandoNombre.set(false),
    });
  }

  canSend(): boolean {
    // Los archivos son opcionales: el paciente puede recibirlos por link en el mensaje.
    return this.phoneValid() && this.dniValid() && this.mensajeValido();
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
    this.mensajeTocado.set(false);
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
