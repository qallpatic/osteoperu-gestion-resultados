import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../../../core/auth/auth.service';

/**
 * Vista publica para que un paciente solicite su clave por SMS. Por seguridad
 * la respuesta es siempre generica y aparece el telefono de soporte (cuando
 * llega del backend) para que el usuario sepa a quien llamar si el SMS no llega.
 */
@Component({
  selector: 'app-recuperar-password',
  imports: [FormsModule, RouterLink],
  templateUrl: './recuperar-password.component.html',
  styleUrl: './recuperar-password.component.css',
})
export class RecuperarPasswordComponent {
  private readonly auth = inject(AuthService);

  telefono = '';
  cargando = signal(false);
  /** Resultado devuelto por el backend (mensaje generico + soporte). */
  resultado = signal<{ mensajeUsuario: string; soporteTelefono: string; retryAfterSeconds: number | null } | null>(null);
  /** Errores de cliente (formato invalido, http). */
  error = signal('');

  telefonoValido(): boolean {
    return /^9\d{8}$/.test(this.telefono.trim());
  }

  enviar(): void {
    if (this.cargando()) {
      return;
    }
    this.error.set('');
    this.resultado.set(null);

    const tel = this.telefono.trim();
    if (!this.telefonoValido()) {
      this.error.set('Ingresa un telefono valido de 9 digitos (Peru).');
      return;
    }

    this.cargando.set(true);
    this.auth.recuperarPassword(tel).subscribe({
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

  /** Convierte retryAfterSeconds a minutos redondeados hacia arriba. */
  minutosRestantes(): number {
    const s = this.resultado()?.retryAfterSeconds ?? 0;
    return Math.max(1, Math.ceil(s / 60));
  }

  /** Vuelve al formulario limpio (para intentar de nuevo). */
  intentarDeNuevo(): void {
    this.resultado.set(null);
    this.error.set('');
  }

  private mensajeError(err: HttpErrorResponse): string {
    if (err.status === 0) {
      return 'No se pudo conectar con el servidor. Verifica tu conexion.';
    }
    if (err.status === 400) {
      return 'Telefono invalido. Verifica el formato.';
    }
    return err.error?.message ?? 'Ocurrio un error. Intenta nuevamente.';
  }
}
