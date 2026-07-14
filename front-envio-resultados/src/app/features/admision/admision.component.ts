import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

import { AdmisionService, CredencialResponse, PacienteAdmision } from './admision.service';
import { AuthService } from '../../core/auth/auth.service';
import { ReniecService } from '../../core/services/reniec.service';

@Component({
  selector: 'app-admision',
  imports: [FormsModule, DatePipe],
  templateUrl: './admision.component.html',
  styleUrl: './admision.component.css',
})
export class AdmisionComponent implements OnInit {
  private readonly srv = inject(AdmisionService);
  private readonly auth = inject(AuthService);
  private readonly reniec = inject(ReniecService);

  /** Spinner mientras se autocompleta el nombre desde RENIEC por DNI (flujo "Nuevo usuario"). */
  readonly consultandoReniec = signal(false);
  /** DNI ya consultado en este modal: evita repetir el lookup para el mismo valor. */
  private ultimoDniReniec = '';

  /**
   * Solo ADMIN/ADMISION pueden generar o anular cuentas del paciente.
   * RESULTADOS ve la lista pero no puede ejecutar acciones de escritura.
   */
  readonly puedeEditarCuenta = computed(() => this.auth.tieneAlgunRol(['ADMIN', 'ADMISION']));

  readonly pacientes = signal<PacienteAdmision[]>([]);
  readonly cargando = signal(false);
  readonly pagina = signal(0);
  readonly totalPaginas = signal(0);
  readonly totalElementos = signal(0);
  readonly tamano = 10;

  fdni = '';
  readonly notice = signal<{ type: 'ok' | 'error'; text: string } | null>(null);

  // Modal generar
  readonly genOpen = signal(false);
  readonly generando = signal(false);
  genDni = '';
  genNombre = '';
  genTelefono = '';
  readonly regenerando = signal(false);
  /** true = alta de un DNI nuevo (DNI editable). */
  readonly nuevoUsuario = signal(false);

  // Credencial resultante
  readonly credencial = signal<CredencialResponse | null>(null);

  // Confirmar anulación
  readonly anularDni = signal<string | null>(null);
  readonly anulando = signal(false);

  readonly rangoDesde = computed(() =>
    this.totalElementos() === 0 ? 0 : this.pagina() * this.tamano + 1,
  );
  readonly rangoHasta = computed(() =>
    Math.min((this.pagina() + 1) * this.tamano, this.totalElementos()),
  );

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.srv.listarPacientes({ dni: this.fdni.trim() || undefined, pagina: this.pagina(), tamano: this.tamano }).subscribe({
      next: (p) => {
        this.pacientes.set(p.contenido);
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

  aplicar(): void {
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

  abrirGenerar(p: PacienteAdmision): void {
    this.genDni = p.dni;
    this.genNombre = p.nombre ?? '';
    this.genTelefono = '';
    this.regenerando.set(p.estadoCuenta === 'ANULADO');
    this.nuevoUsuario.set(false);
    this.credencial.set(null);
    this.genOpen.set(true);
  }

  /** Alta de un paciente/DNI nuevo (no requiere que exista en la lista). */
  abrirGenerarNuevo(): void {
    this.genDni = '';
    this.genNombre = '';
    this.genTelefono = '';
    this.regenerando.set(false);
    this.nuevoUsuario.set(true);
    this.credencial.set(null);
    this.genOpen.set(true);
  }

  cerrarGenerar(): void {
    this.genOpen.set(false);
  }

  dniGenValido(): boolean {
    return /^\d{8,12}$/.test(this.genDni.trim());
  }

  /**
   * Autocompleta el campo "Nombre completo" al completar el DNI en el modal
   * "Nuevo usuario". Solo consulta RENIEC cuando el DNI tiene exactamente 8
   * digitos (CE >8 no existe en RENIEC y se ahorra la llamada al proveedor).
   * Si el operador ya escribio un nombre, no lo pisa.
   */
  autocompletarNombrePorDni(): void {
    const dni = (this.genDni ?? '').trim();
    if (!/^\d{8}$/.test(dni) || dni === this.ultimoDniReniec) {
      return;
    }
    this.ultimoDniReniec = dni;
    this.consultandoReniec.set(true);
    this.reniec.consultarNombre(dni).subscribe({
      next: (nombre) => {
        this.consultandoReniec.set(false);
        if (nombre && !this.genNombre.trim()) {
          this.genNombre = nombre;
        }
      },
      error: () => this.consultandoReniec.set(false),
    });
  }

  generar(): void {
    if (this.generando()) {
      return;
    }
    if (!this.dniGenValido()) {
      this.notice.set({ type: 'error', text: 'Ingresa un documento válido de 8 a 12 dígitos.' });
      return;
    }
    this.generando.set(true);
    this.srv
      .generarUsuario({
        dni: this.genDni,
        nombre: this.genNombre.trim() || undefined,
        telefono: this.genTelefono.trim() || undefined,
      })
      .subscribe({
        next: (cred) => {
          this.generando.set(false);
          this.genOpen.set(false);
          this.credencial.set(cred);
          this.notice.set({ type: 'ok', text: 'Credencial generada. Entrégala al paciente.' });
          this.cargar();
        },
        error: (e: HttpErrorResponse) => {
          this.generando.set(false);
          this.notice.set({ type: 'error', text: this.msg(e) });
        },
      });
  }

  cerrarCredencial(): void {
    this.credencial.set(null);
  }

  confirmarAnular(p: PacienteAdmision): void {
    this.anularDni.set(p.dni);
  }

  anular(): void {
    const dni = this.anularDni();
    if (!dni) {
      return;
    }
    this.anulando.set(true);
    this.srv.anularUsuario(dni).subscribe({
      next: () => {
        this.anulando.set(false);
        this.anularDni.set(null);
        this.notice.set({ type: 'ok', text: `Acceso del DNI ${dni} anulado.` });
        this.cargar();
      },
      error: (e: HttpErrorResponse) => {
        this.anulando.set(false);
        this.anularDni.set(null);
        this.notice.set({ type: 'error', text: this.msg(e) });
      },
    });
  }

  imprimir(c: CredencialResponse): void {
    const w = window.open('', '_blank', 'width=320,height=700');
    if (!w) {
      return;
    }
    w.document.write(`
      <html><head><title>Credencial ${c.usuario}</title>
      <style>
        @page { size: 80mm 175mm; margin: 0; }
        html, body { width: 80mm; height: 175mm; margin: 0; padding: 0; }
        body {
          font-family: Arial, Helvetica, sans-serif;
          color: #1f2937;
          padding: 3mm;
          box-sizing: border-box;
          display: flex;
          flex-direction: column;
        }
        .header {
          text-align: center;
          flex: 0 0 auto;
          font-size: 16px;
          font-weight: 800;
          letter-spacing: 2px;
          color: #043d36;
          padding-bottom: 2mm;
          margin-bottom: 2mm;
          border-bottom: 1px dashed #043d36;
        }
        .datos { text-align: left; flex: 0 0 auto; }
        .web { font-size: 10px; color: #075e54; word-break: break-all; margin-bottom: 2mm; }
        .row { text-align: left; margin: 0.5mm 0; font-size: 11px; line-height: 1.25; }
        .k { color: #6b7280; font-size: 8px; text-transform: uppercase; letter-spacing: 0.3px; }
        .clave { font-size: 14px; font-weight: 700; letter-spacing: 1.5px; color: #043d36; }
        .qr-wrap {
          flex: 0 0 auto;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-top: 2mm;
        }
        .qr-wrap img { width: 28mm; height: 28mm; display: block; }
      </style></head><body>
        <div class="header">OSTEOPERU</div>
        <div class="datos">
          <div class="web"><b>WEB:</b> ${c.portalUrl}</div>
          <div class="row"><span class="k">Nombre</span><br>${c.nombre ?? '-'}</div>
          <div class="row"><span class="k">Usuario</span><br>${c.usuario}</div>
          <div class="row"><span class="k">Clave</span><br><span class="clave">${c.clave}</span></div>
        </div>
        <div class="qr-wrap"><img src="${c.qrUrl}" alt="QR" /></div>
      </body></html>`);
    w.document.close();
    w.focus();
    setTimeout(() => w.print(), 400);
  }

  private msg(e: HttpErrorResponse): string {
    if (e.status === 0) {
      return 'No se pudo conectar con el servidor.';
    }
    return e.error?.message ?? 'Ocurrió un error. Intenta nuevamente.';
  }
}
