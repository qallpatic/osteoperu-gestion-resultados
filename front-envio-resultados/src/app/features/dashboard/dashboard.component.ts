import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';

interface Acceso {
  label: string;
  icon: string;
  route: string;
  roles: string[];
  descripcion: string;
}

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  template: `
    <section class="dash">
      <header class="dash__head">
        <h1>Hola, {{ nombre() }} 👋</h1>
        <p>Bienvenido al panel de OsteoPerú. Estos son tus accesos según tu perfil.</p>
        <div class="chips">
          @for (rol of roles(); track rol) {
            <span class="chip">{{ rol }}</span>
          }
        </div>
      </header>

      @if (accesos().length) {
        <div class="tiles">
          @for (a of accesos(); track a.route) {
            <a class="tile" [routerLink]="a.route">
              <span class="tile__icon"><i class="fas {{ a.icon }}"></i></span>
              <span class="tile__body">
                <span class="tile__title">{{ a.label }}</span>
                <span class="tile__desc">{{ a.descripcion }}</span>
              </span>
              <i class="fas fa-arrow-right tile__go"></i>
            </a>
          }
        </div>
      } @else {
        <div class="vacio">
          <i class="fas fa-circle-info"></i>
          <p>Aún no tienes módulos habilitados. Contacta al administrador.</p>
        </div>
      }
    </section>
  `,
  styles: `
    :host { display: block; }
    .dash { max-width: var(--content-max); margin: 0 auto; padding: 40px 32px; }
    .dash__head h1 { color: var(--brand-900); font-size: 1.8rem; font-weight: 700; }
    .dash__head p { color: var(--muted); margin-top: 6px; }
    .chips { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 16px; }
    .chip {
      font-size: 0.72rem; font-weight: 700; letter-spacing: 0.5px;
      text-transform: uppercase; color: var(--brand-700);
      background: #e6f2f0; padding: 5px 12px; border-radius: 999px;
    }
    .tiles {
      display: grid; gap: 18px; margin-top: 32px;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    }
    .tile {
      display: flex; align-items: center; gap: 16px;
      background: #fff; border: 1px solid var(--border);
      border-radius: 16px; padding: 20px; text-decoration: none;
      color: inherit; box-shadow: 0 4px 16px rgba(7, 94, 84, 0.06);
      transition: transform 0.18s, box-shadow 0.18s, border-color 0.18s;
    }
    .tile:hover { transform: translateY(-3px); box-shadow: 0 10px 26px rgba(7, 94, 84, 0.16); border-color: var(--brand-300); }
    .tile__icon {
      flex: none; width: 52px; height: 52px; display: grid; place-items: center;
      border-radius: 14px; font-size: 1.4rem; color: #fff;
      background: linear-gradient(135deg, var(--brand-700), var(--brand-500));
    }
    .tile__body { display: flex; flex-direction: column; flex: 1; }
    .tile__title { font-weight: 600; color: var(--brand-900); }
    .tile__desc { font-size: 0.85rem; color: var(--muted); margin-top: 2px; }
    .tile__go { color: var(--brand-500); }
    .vacio {
      margin-top: 40px; text-align: center; color: var(--muted);
      display: flex; flex-direction: column; align-items: center; gap: 10px;
    }
    .vacio i { font-size: 2rem; color: var(--brand-300); }
  `,
})
export class DashboardComponent {
  private readonly auth = inject(AuthService);

  readonly nombre = this.auth.nombre;
  readonly roles = this.auth.roles;

  private readonly modulos: Acceso[] = [
    {
      label: 'Administración',
      icon: 'fa-users-gear',
      route: '/admin',
      roles: ['ADMIN'],
      descripcion: 'Gestionar usuarios, roles y accesos',
    },
    {
      label: 'Resultados',
      icon: 'fa-file-medical',
      route: '/resultados',
      roles: ['ADMIN', 'RESULTADOS'],
      descripcion: 'Cargar y enviar resultados de pacientes',
    },
    {
      label: 'Admisión',
      icon: 'fa-id-badge',
      route: '/admision',
      roles: ['ADMIN', 'ADMISION'],
      descripcion: 'Generar y anular accesos de pacientes',
    },
    {
      label: 'Mis resultados',
      icon: 'fa-notes-medical',
      route: '/paciente',
      roles: ['ADMIN', 'PACIENTE'],
      descripcion: 'Consultar y descargar tus estudios',
    },
  ];

  readonly accesos = computed(() => this.modulos.filter((m) => this.auth.tieneAlgunRol(m.roles)));
}
