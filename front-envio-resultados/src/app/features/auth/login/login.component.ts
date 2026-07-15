import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  usuario = '';
  contrasena = '';
  showPassword = signal(false);
  error = signal('');
  cargando = signal(false);

  submit(): void {
    if (this.cargando()) {
      return;
    }
    this.error.set('');
    this.cargando.set(true);

    this.auth.login(this.usuario.trim(), this.contrasena).subscribe({
      next: () => {
        this.cargando.set(false);
        this.router.navigate(['/']);
      },
      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);
        this.error.set(this.mensajeError(err));
      },
    });
  }

  private mensajeError(err: HttpErrorResponse): string {
    if (err.status === 0) {
      return 'No se pudo conectar con el servidor. Verifica tu conexión.';
    }
    if (err.status === 401 || err.status === 400) {
      return 'Usuario o contraseña incorrectos.';
    }
    return err.error?.message ?? 'Ocurrió un error al iniciar sesión.';
  }
}
