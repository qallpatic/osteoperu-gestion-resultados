import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ResultadosService } from '../../services/resultados.service';

interface PickedFile {
  file: File;
  name: string;
  size: number;
  kind: 'pdf' | 'image' | 'video';
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
export class UploadComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  private resultados = inject(ResultadosService);

  readonly maxMb = MAX_MB;

  phone = '';
  dni = '';
  patientName = '';
  files = signal<PickedFile[]>([]);
  dragging = signal(false);
  sending = signal(false);
  notice = signal<{ type: 'ok' | 'error'; text: string } | null>(null);

  // Metodos (no computed): dependen de phone/dni enlazados con ngModel, que son
  // propiedades normales y no signals. Se evaluan en cada deteccion de cambios.
  phoneValid(): boolean {
    return /^\d{9}$/.test(this.phone);
  }

  dniValid(): boolean {
    return /^\d{8}$/.test(this.dni);
  }

  canSend(): boolean {
    return this.phoneValid() && this.dniValid() && this.files().length > 0;
  }

  /* ===== Manejo de archivos ===== */
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
    if (!list) return;
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

    if (accepted.length) this.files.update((f) => [...f, ...accepted]);
    this.notice.set(
      rejected.length
        ? { type: 'error', text: rejected.join(' · ') }
        : null
    );
  }

  remove(index: number): void {
    this.files.update((f) => f.filter((_, i) => i !== index));
  }

  formatSize(bytes: number): string {
    return bytes < 1024 * 1024
      ? `${(bytes / 1024).toFixed(0)} KB`
      : `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  /* ===== Envío ===== */
  send(): void {
    if (!this.canSend() || this.sending()) return;
    this.sending.set(true);
    this.notice.set(null);

    this.resultados
      .upload({
        phone: this.phone,
        dni: this.dni,
        patientName: this.patientName.trim(),
        files: this.files().map((f) => f.file),
      })
      .subscribe({
        next: (res) => {
          this.sending.set(false);
          this.notice.set({
            type: 'ok',
            text: `Resultados enviados al paciente. Se generó el link y se encoló el SMS (${res.smsStatus}).`,
          });
          this.phone = '';
          this.dni = '';
          this.patientName = '';
          this.files.set([]);
        },
        error: (err) => {
          this.sending.set(false);
          this.notice.set({
            type: 'error',
            text: this.errorMessage(err),
          });
        },
      });
  }

  /** Traduce el error HTTP a un mensaje entendible para el operador del panel. */
  private errorMessage(err: unknown): string {
    const e = err as { status?: number; error?: { message?: string } };
    if (e.status === 0) {
      return 'No se pudo conectar con el servidor. Verifica tu conexión.';
    }
    if (e.status === 401) {
      return 'Credenciales del servicio inválidas.';
    }
    return e.error?.message ?? 'Ocurrió un error al enviar los resultados.';
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
