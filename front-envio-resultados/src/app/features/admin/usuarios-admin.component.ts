import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { UsuariosAdminService } from './usuarios-admin.service';
import { ReniecService } from '../../core/services/reniec.service';
import { RolResponse, UsuarioResponse } from './admin.models';

type Modal = 'crear' | 'editar' | 'roles' | 'password' | 'eliminar' | null;

@Component({
  selector: 'app-admin-usuarios',
  imports: [FormsModule],
  templateUrl: './usuarios-admin.component.html',
  styleUrl: './usuarios-admin.component.css',
})
export class UsuariosAdminComponent implements OnInit {
  private readonly srv = inject(UsuariosAdminService);
  private readonly reniec = inject(ReniecService);

  // Listado
  readonly usuarios = signal<UsuarioResponse[]>([]);
  readonly cargando = signal(false);
  readonly pagina = signal(0);
  readonly totalPaginas = signal(0);
  readonly totalElementos = signal(0);
  readonly tamano = 10;

  buscar = '';
  filtroRol = '';
  filtroEstado = ''; // '', 'true', 'false'

  /** Orden: null = default backend (id asc), 'asc', 'desc'. */
  readonly ordenarPor = signal<string | null>(null);
  readonly direccion = signal<'asc' | 'desc' | null>(null);

  /** Cicla null -> asc -> desc -> null para una columna. */
  ordenar(campo: string): void {
    if (this.ordenarPor() !== campo) {
      this.ordenarPor.set(campo);
      this.direccion.set('asc');
    } else if (this.direccion() === 'asc') {
      this.direccion.set('desc');
    } else {
      this.ordenarPor.set(null);
      this.direccion.set(null);
    }
    this.pagina.set(0);
    this.cargar();
  }

  readonly roles = signal<RolResponse[]>([]);
  readonly notice = signal<{ type: 'ok' | 'error'; text: string } | null>(null);

  // Modal
  readonly modal = signal<Modal>(null);
  readonly seleccionado = signal<UsuarioResponse | null>(null);
  readonly guardando = signal(false);
  readonly modalError = signal('');
  /** Spinner mientras se autocompleta el nombre desde RENIEC por DNI. */
  readonly consultandoReniec = signal(false);

  form = { usuario: '', nombre: '', correo: '', dni: '', password: '' };
  readonly rolesSel = signal<number[]>([]);
  passwordNueva = '';
  private ultimoDniReniec = '';

  readonly rangoDesde = computed(() =>
    this.totalElementos() === 0 ? 0 : this.pagina() * this.tamano + 1,
  );
  readonly rangoHasta = computed(() =>
    Math.min((this.pagina() + 1) * this.tamano, this.totalElementos()),
  );

  ngOnInit(): void {
    this.srv.roles().subscribe({ next: (r) => this.roles.set(r) });
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.srv
      .listar({
        pagina: this.pagina(),
        tamano: this.tamano,
        buscar: this.buscar.trim() || undefined,
        rol: this.filtroRol || undefined,
        estado: this.filtroEstado === '' ? undefined : this.filtroEstado === 'true',
        ordenarPor: this.ordenarPor() ?? undefined,
        direccion: this.direccion() ?? undefined,
      })
      .subscribe({
        next: (p) => {
          this.usuarios.set(p.contenido);
          this.totalPaginas.set(p.totalPaginas);
          this.totalElementos.set(p.totalElementos);
          this.cargando.set(false);
        },
        error: (e: HttpErrorResponse) => {
          this.cargando.set(false);
          this.notice.set({ type: 'error', text: this.msg(e) });
        },
      });
  }

  aplicarFiltros(): void {
    this.pagina.set(0);
    this.cargar();
  }

  irPagina(delta: number): void {
    const nueva = this.pagina() + delta;
    if (nueva < 0 || nueva >= this.totalPaginas()) {
      return;
    }
    this.pagina.set(nueva);
    this.cargar();
  }

  // ===== Roles =====
  private idsDeRoles(nombres: string[]): number[] {
    return this.roles()
      .filter((r) => nombres.includes(r.nombre))
      .map((r) => r.id);
  }

  toggleRol(id: number): void {
    this.rolesSel.update((ids) => (ids.includes(id) ? ids.filter((x) => x !== id) : [...ids, id]));
  }

  rolMarcado(id: number): boolean {
    return this.rolesSel().includes(id);
  }

  // ===== Abrir modales =====
  abrirCrear(): void {
    this.form = { usuario: '', nombre: '', correo: '', dni: '', password: '' };
    this.rolesSel.set([]);
    this.modalError.set('');
    this.ultimoDniReniec = '';
    this.modal.set('crear');
  }

  abrirEditar(u: UsuarioResponse): void {
    this.seleccionado.set(u);
    this.form = {
      usuario: u.usuario,
      nombre: u.nombre ?? '',
      correo: u.correo ?? '',
      dni: u.dni ?? '',
      password: '',
    };
    this.modalError.set('');
    this.ultimoDniReniec = '';
    this.modal.set('editar');
  }

  abrirRoles(u: UsuarioResponse): void {
    this.seleccionado.set(u);
    this.rolesSel.set(this.idsDeRoles(u.roles));
    this.modalError.set('');
    this.modal.set('roles');
  }

  abrirPassword(u: UsuarioResponse): void {
    this.seleccionado.set(u);
    this.passwordNueva = '';
    this.modalError.set('');
    this.modal.set('password');
  }

  confirmarEliminar(u: UsuarioResponse): void {
    this.seleccionado.set(u);
    this.modalError.set('');
    this.modal.set('eliminar');
  }

  cerrar(): void {
    this.modal.set(null);
    this.seleccionado.set(null);
    this.guardando.set(false);
    this.modalError.set('');
  }

  // ===== Acciones =====
  guardarCrear(): void {
    if (!this.form.usuario.trim() || this.form.password.length < 6 || this.rolesSel().length === 0) {
      this.modalError.set('Usuario, contraseña (mín. 6) y al menos un rol son obligatorios.');
      return;
    }
    this.guardando.set(true);
    this.srv
      .crear({
        usuario: this.form.usuario.trim(),
        nombre: this.form.nombre || undefined,
        correo: this.form.correo || undefined,
        dni: this.form.dni || undefined,
        password: this.form.password,
        rolesIds: this.rolesSel(),
      })
      .subscribe({ next: () => this.exito('Usuario creado correctamente.'), error: (e) => this.errorModal(e) });
  }

  guardarEditar(): void {
    const u = this.seleccionado();
    if (!u) {
      return;
    }
    this.guardando.set(true);
    this.srv
      .actualizar(u.id, {
        nombre: this.form.nombre || undefined,
        correo: this.form.correo || undefined,
        dni: this.form.dni || undefined,
      })
      .subscribe({ next: () => this.exito('Usuario actualizado.'), error: (e) => this.errorModal(e) });
  }

  guardarRoles(): void {
    const u = this.seleccionado();
    if (!u) {
      return;
    }
    if (this.rolesSel().length === 0) {
      this.modalError.set('Debe asignar al menos un rol.');
      return;
    }
    this.guardando.set(true);
    this.srv
      .asignarRoles(u.id, this.rolesSel())
      .subscribe({ next: () => this.exito('Roles actualizados.'), error: (e) => this.errorModal(e) });
  }

  guardarPassword(): void {
    const u = this.seleccionado();
    if (!u) {
      return;
    }
    if (this.passwordNueva.length < 6) {
      this.modalError.set('La contraseña debe tener al menos 6 caracteres.');
      return;
    }
    this.guardando.set(true);
    this.srv
      .cambiarPassword(u.id, this.passwordNueva)
      .subscribe({ next: () => this.exito('Contraseña actualizada.'), error: (e) => this.errorModal(e) });
  }

  eliminar(): void {
    const u = this.seleccionado();
    if (!u) {
      return;
    }
    this.guardando.set(true);
    this.srv
      .eliminar(u.id)
      .subscribe({ next: () => this.exito('Usuario eliminado.'), error: (e) => this.errorModal(e) });
  }

  cambiarEstado(u: UsuarioResponse): void {
    this.srv.cambiarEstado(u.id, !u.estado).subscribe({
      next: () => {
        this.notice.set({ type: 'ok', text: `Usuario ${u.estado ? 'desactivado' : 'activado'}.` });
        this.cargar();
      },
      error: (e) => this.notice.set({ type: 'error', text: this.msg(e) }),
    });
  }

  /**
   * Autocompleta el campo "Nombre completo" al completar el DNI.
   * El backend ya cachea RENIEC en BD, asi que este lookup es gratuito
   * para DNIs vistos en los ultimos 180 dias.
   */
  autocompletarNombrePorDni(): void {
    const dni = (this.form.dni ?? '').trim();
    if (!/^\d{8}$/.test(dni) || dni === this.ultimoDniReniec) {
      return;
    }
    this.ultimoDniReniec = dni;
    this.consultandoReniec.set(true);
    this.reniec.consultarNombre(dni).subscribe({
      next: (nombre) => {
        this.consultandoReniec.set(false);
        if (nombre && !this.form.nombre) {
          this.form.nombre = nombre;
        }
      },
      error: () => this.consultandoReniec.set(false),
    });
  }

  private exito(texto: string): void {
    this.notice.set({ type: 'ok', text: texto });
    this.cerrar();
    this.cargar();
  }

  private errorModal(e: HttpErrorResponse): void {
    this.guardando.set(false);
    this.modalError.set(this.msg(e));
  }

  private msg(e: HttpErrorResponse): string {
    if (e.status === 0) {
      return 'No se pudo conectar con el servidor.';
    }
    return e.error?.message ?? 'Ocurrió un error. Intenta nuevamente.';
  }
}
