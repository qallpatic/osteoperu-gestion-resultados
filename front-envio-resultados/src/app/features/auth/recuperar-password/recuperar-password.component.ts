import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../../../core/auth/auth.service';

/**
 * Vista publica para que un paciente solicite su clave por SMS. Pide el
 * documento (DNI o CE) y al recibir la respuesta del back muestra el
 * telefono ofuscado al que supuestamente se envio el SMS (para que el
 * usuario verifique visualmente). Mensaje siempre generico para no
 * filtrar si el documento existe o no.
 */
@Component({
  selector: 'app-recuperar-password',
  imports: [FormsModule, RouterLink],
  templateUrl: './recuperar-password.component.html',
  styleUrl: './recuperar-password.component.css',
})
export class RecuperarPasswordComponent {
  private readonly auth = inject(AuthService);

  documento = '';
  cargando = signal(false);
  /** Resultado devuelto por el backend (mensaje generico + telefono ofuscado). */
  resultado = signal<{ mensajeUsuario: string; soporteTelefono: string; telefonoOfuscado: string; retryAfterSeconds: number | null } | null>(null);
  /** Errores de cliente (formato invalido, http). */
  error = signal('');

  /** DNI (8 digitos) o CE (9-12 digitos). */
  documentoValido(): boolean {
    return /^\d{8,12}$/.test(this.documento.trim());
  }

  enviar(): void {
    if (this.cargando()) {
      return;
    }
    this.error.set('');
    this.resultado.set(null);

    const doc = this.documento.trim();
    if (!this.documentoValido()) {
      this.error.set('Ingresa un documento valido (DNI 8 digitos o CE 9-12).');
      return;
    }

    this.cargando.set(true);
    this.auth.recuperarPassword(doc).subscribe({
      next: (data) => {
        this.cargando.set(false);
        this.resultado.set(data);
      },
      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);
        this.error.set(this.mensajeError(err));
      },
    });
  }

  /** Vuelve al formulario limpio (para intentar con otro documento). */
  intentarDeNuevo(): void {
    this.resultado.set(null);
    this.error.set('');
  }

  /** Convierte retryAfterSeconds a minutos redondeados hacia arriba. */
  minutosRestantes(): number {
    const s = this.resultado()?.retryAfterSeconds ?? 0;
    return Math.max(1, Math.ceil(s / 60));
  }

  private mensajeError(err: HttpErrorResponse): string {
    if (err.status === 0) {
      return 'No se pudo conectar con el servidor. Verifica tu conexion.';
    }
    if (err.status === 400) {
      return 'Documento invalido. Verifica el formato.';
    }
    return err.error?.message ?? 'Ocurrio un error. Intenta nuevamente.';
  }
}
