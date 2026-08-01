import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, interval } from 'rxjs';

import { AuthService } from '../auth/auth.service';
import { MenuItem } from '../models/sesion.model';
import { UploadComponent, UploadPrefill } from '../../features/resultados/upload.component';
import { PacientePendiente, PendientesService } from '../../features/resultados/pendientes.service';
import { NombreNaturalPipe } from '../pipes/nombre-natural.pipe';
import { formatearNombreNatural } from '../utils/util-nombre';

/** Shell principal: menú lateral izquierdo (con submenús) + topbar según el usuario. */
@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, UploadComponent, NombreNaturalPipe],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.css',
})
export class MainLayoutComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly pendientes = inject(PendientesService);

  readonly nombre = this.auth.nombre;
  readonly rolPrincipal = this.auth.rolPrincipal;
  /** El AuthService guarda el nombre en formato canonico "APELLIDOS, NOMBRES".
   *  Para el saludo usamos la version natural ("NOMBRES APELLIDOS") y tomamos
   *  el primer token, que ahora SI es el nombre de pila. */
  readonly primerNombre = computed(() => {
    const n = formatearNombreNatural(this.nombre()).trim();
    return n.split(/\s+/)[0] || 'Usuario';
  });

  // ===== Campanita de pacientes pendientes (ADMIN / RESULTADOS) =====
  readonly puedeSubir = computed(() => this.auth.tieneAlgunRol(['ADMIN', 'RESULTADOS']));
  readonly listaPendientes = this.pendientes.lista;
  readonly contadorPendientes = this.pendientes.contador;
  readonly campanaAbierta = signal(false);
  /** Precarga del modal de subida (null = cerrado). */
  readonly prefillUpload = signal<UploadPrefill | null>(null);

  readonly colapsado = signal(false);
  readonly menuMovilAbierto = signal(false);
  /** Label del grupo (con submenús) actualmente desplegado. */
  readonly expandido = signal<string | null>(null);
  /** URL activa (para resaltar el grupo padre). */
  readonly url = signal(this.router.url);

  private readonly items: MenuItem[] = [
    { label: 'Dashboard', icon: 'fa-chart-line', route: '/dashboard', roles: ['ADMIN'], disponible: true },
    {
      label: 'Resultados', icon: 'fa-file-medical', route: '/resultados',
      roles: ['ADMIN', 'RESULTADOS'], disponible: true,
      children: [
        { label: 'Consultar', icon: 'fa-magnifying-glass', route: '/resultados/consultar' },
        { label: 'Cargar', icon: 'fa-cloud-arrow-up', route: '/resultados/cargar' },
        { label: 'Pendientes', icon: 'fa-clock', route: '/resultados/pendientes' },
        { label: 'Logs de carga', icon: 'fa-list-check', route: '/resultados/logs' },
      ],
    },
    { label: 'Administración', icon: 'fa-users-gear', route: '/admin', roles: ['ADMIN'], disponible: true },
    { label: 'Gestión de usuarios', icon: 'fa-id-badge', route: '/gestion-usuarios', roles: ['ADMIN', 'RESULTADOS', 'ADMISION'], disponible: true },
    { label: 'Mis resultados', icon: 'fa-notes-medical', route: '/paciente', roles: ['ADMIN', 'PACIENTE'], disponible: true }
    //{ label: 'Call Center', icon: 'fa-headset', route: '/callcenter', roles: ['ADMIN', 'CALLCENTER'], disponible: false },
  ];

  readonly menu = computed(() => this.items.filter((m) => this.auth.tieneAlgunRol(m.roles)));

  readonly iniciales = computed(() => {
    const n = formatearNombreNatural(this.nombre()).trim();
    if (!n) {
      return 'U';
    }
    const partes = n.split(/\s+/);
    return (partes[0][0] + (partes[1]?.[0] ?? '')).toUpperCase();
  });

  constructor() {
    this.sincronizarRuta(this.router.url);
    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe((e) => this.sincronizarRuta(e.urlAfterRedirects));

    // Campanita: carga inicial + refresco periódico para agentes de resultados.
    if (this.auth.tieneAlgunRol(['ADMIN', 'RESULTADOS'])) {
      this.pendientes.cargar();
      interval(60000).pipe(takeUntilDestroyed()).subscribe(() => this.pendientes.cargar());
    }
  }

  alternarCampana(): void {
    const abrir = !this.campanaAbierta();
    if (abrir) {
      this.pendientes.cargar();
    }
    this.campanaAbierta.set(abrir);
  }

  cerrarCampana(): void {
    this.campanaAbierta.set(false);
  }

  /** Abre el formulario de carga precargado con los datos del paciente pendiente. */
  abrirCargaPendiente(p: PacientePendiente): void {
    this.campanaAbierta.set(false);
    this.menuMovilAbierto.set(false);
    this.prefillUpload.set({ dni: p.dni, nombre: p.nombre, telefono: p.telefono });
  }

  cerrarCargaModal(): void {
    this.prefillUpload.set(null);
  }

  onUploadEnviado(): void {
    this.cerrarCargaModal();
    this.pendientes.cargar();
  }

  /**
   * Despliega el grupo cuya ruta esté activa y cierra los demás. Al navegar a un
   * módulo fuera de un submenú, el acordeón se pliega automáticamente.
   */
  private sincronizarRuta(u: string): void {
    this.url.set(u);
    const grupo = this.items.find((i) => i.children?.length && u.startsWith(i.route));
    this.expandido.set(grupo ? grupo.label : null);
  }

  /** Si está colapsado navega al módulo; si no, despliega/pliega el submenú. */
  toggleGrupo(item: MenuItem): void {
    if (this.colapsado()) {
      this.router.navigate([item.route]);
      this.cerrarMenuMovil();
      return;
    }
    this.expandido.update((v) => (v === item.label ? null : item.label));
  }

  alternarColapso(): void {
    this.colapsado.update((v) => !v);
  }

  alternarMenuMovil(): void {
    this.menuMovilAbierto.update((v) => !v);
  }

  cerrarMenuMovil(): void {
    this.menuMovilAbierto.set(false);
  }

  cerrarSesion(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
