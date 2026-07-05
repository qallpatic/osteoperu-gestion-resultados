import { Injectable, signal } from '@angular/core';

const STORAGE_KEY = 'osteo_resultados_session';

const VALID_USER = 'osteoperu_resultados';
const VALID_PASS = 'Osteo#2026';

const SESSION_TTL_MS = 8 * 60 * 60 * 1000;

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly loggedIn = signal<boolean>(this.readSession());

  login(user: string, pass: string): boolean {
    const ok = user.trim() === VALID_USER && pass === VALID_PASS;
    if (ok) {
      const expiresAt = Date.now() + SESSION_TTL_MS;
      localStorage.setItem(STORAGE_KEY, String(expiresAt));
      this.loggedIn.set(true);
    }
    return ok;
  }

  logout(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.loggedIn.set(false);
  }

  private readSession(): boolean {
    const raw = localStorage.getItem(STORAGE_KEY);
    const expiresAt = raw ? Number(raw) : 0;
    if (expiresAt && Date.now() < expiresAt) {
      return true;
    }
    localStorage.removeItem(STORAGE_KEY);
    return false;
  }
}
