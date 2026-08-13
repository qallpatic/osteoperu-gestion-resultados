import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../../../core/auth/auth.service';
import { CaptchaService } from '../../../core/captcha/captcha.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly captcha = inject(CaptchaService);

  usuario = '';
  contrasena = '';
  showPassword = signal(false);
  error = signal('');
  /** Mensaje informativo (no de error) que viene de query params al ser redirigido
   *  desde el errorInterceptor cuando expiro la sesion. */
  info = signal('');
  cargando = signal(false);

  /** Config captcha consultada una vez al cargar la pantalla. */
  captchaHabilitado = false;

  async ngOnInit(): Promise<void> {
    // Si el usuario viene redirigido porque expiro la sesion, mostramos
    // un mensaje claro en lugar del toast generico.
    const motivo = this.route.snapshot.queryParamMap.get('motivo');
    if (motivo === 'sesion-expirada') {
      this.info.set('Tu sesion expiro por inactividad. Vuelve a iniciar sesion.');
    }
    try {
      const cfg = await this.captcha.config();
      this.captchaHabilitado = cfg.enabled;
    } catch {
      this.captchaHabilitado = false;
    }
  }

  async submit(): Promise<void> {
    if (this.cargando()) {
      return;
    }
    this.error.set('');
    this.cargando.set(true);

    let token: string | null = null;
    if (this.captchaHabilitado) {
      try {
        token = await this.captcha.obtenerToken('login');
        console.log('Token captcha obtenido', token);
      } catch {
        this.cargando.set(false);
        this.error.set('No se pudo completar la verificacion de seguridad. Recarga la pagina.');
        return;
      }
    }

    this.auth.login(this.usuario.trim(), this.contrasena, token).subscribe({
      next: () => {
        this.cargando.set(false);
        // Limpiamos el query param tras un login exitoso.
        this.router.navigate(['/'], { replaceUrl: true });
        console.log('Login exitoso-1', token);
      },
      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);
        this.error.set(this.mensajeError(err));
      },
    });
  }

  private mensajeError(err: HttpErrorResponse): string {
    if (err.status === 0) {
      return 'No se pudo conectar con el servidor. Verifica tu conexion.';
    }
    if (err.status === 429) {
      return 'Demasiados intentos. Espera unos minutos antes de reintentar.';
    }
    if (err.status === 403) {
      return 'Verificacion de seguridad fallida. Intenta nuevamente.';
    }
    if (err.status === 401 || err.status === 400) {
      return 'Usuario o contrasena incorrectos.';
    }
    if (err.status >= 500) {
      return `Error del servidor (${err.status}). Reintenta en unos segundos.`;
    }
    return err.error?.message ?? 'Ocurrio un error al iniciar sesion.';
  }
}
