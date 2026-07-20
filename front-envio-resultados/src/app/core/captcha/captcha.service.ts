import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { ConfigCaptcha } from './captcha.types';

interface RespuestaApi<T> {
  data: T;
}

const URL_CONFIG = `${environment.apiBaseUrl}/api/auth/captcha-config`;
const URL_SCRIPT = 'https://www.google.com/recaptcha/api.js';

/**
 * Servicio singleton para reCAPTCHA v3.
 * <ul>
 *   <li>{@link config} consulta una unica vez la config publica del backend
 *       (single-flight) y devuelve si esta habilitado + la site key.</li>
 *   <li>{@link obtenerToken} inyecta el script de Google on-demand (la
 *       primera vez) y devuelve un token fresco para la accion dada.</li>
 * </ul>
 * Si el CAPTCHA esta deshabilitado, {@link obtenerToken} devuelve '' y el
 * backend lo acepta (compat con entornos donde todavia no se envia token).
 */
@Injectable({ providedIn: 'root' })
export class CaptchaService {
  private readonly http = inject(HttpClient);

  private configPromise?: Promise<ConfigCaptcha>;
  private readonly scriptPromisePorSiteKey = new Map<string, Promise<void>>();

  async config(): Promise<ConfigCaptcha> {
    if (!this.configPromise) {
      this.configPromise = firstValueFrom(
        this.http
          .get<RespuestaApi<ConfigCaptcha>>(URL_CONFIG)
          .pipe(map((r) => r.data)),
      );
    }
    return this.configPromise;
  }

  async obtenerToken(accion: string): Promise<string> {
    const cfg = await this.config();
    if (!cfg.habilitado) {
      return '';
    }
    if (!cfg.siteKey) {
      throw new Error('reCAPTCHA habilitado sin siteKey en el backend.');
    }
    await this.cargarScript(cfg.siteKey);

    const grecaptcha = window.grecaptcha;
    if (!grecaptcha) {
      throw new Error('No se cargo el script de reCAPTCHA.');
    }

    return new Promise<string>((resolve, reject) => {
      grecaptcha.ready(() => {
        grecaptcha
          .execute(cfg.siteKey as string, { action: accion })
          .then(resolve)
          .catch(reject);
      });
    });
  }

  private cargarScript(siteKey: string): Promise<void> {
    const existente = this.scriptPromisePorSiteKey.get(siteKey);
    if (existente) {
      return existente;
    }

    const promesa = new Promise<void>((resolve, reject) => {
      if (window.grecaptcha) {
        window.grecaptcha.ready(() => resolve());
        return;
      }

      const script = document.createElement('script');
      script.src = `${URL_SCRIPT}?render=${encodeURIComponent(siteKey)}`;
      script.async = true;
      script.defer = true;
      script.dataset['recaptcha'] = siteKey;
      script.onload = () => {
        if (window.grecaptcha) {
          window.grecaptcha.ready(() => resolve());
        } else {
          reject(new Error('Script reCAPTCHA cargo pero grecaptcha no disponible.'));
        }
      };
      script.onerror = () => reject(new Error('Falló la carga del script de reCAPTCHA.'));
      document.head.appendChild(script);
    });

    this.scriptPromisePorSiteKey.set(siteKey, promesa);
    return promesa;
  }
}
