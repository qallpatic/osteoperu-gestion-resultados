import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

import { AdmisionService, CredencialResponse, PacienteAdmision } from './admision.service';
import { AuthService } from '../../core/auth/auth.service';
import { ReniecService } from '../../core/services/reniec.service';
import { TelefonoPipe } from '../../core/pipes/telefono.pipe';
import { NombreNaturalPipe } from '../../core/pipes/nombre-natural.pipe';
import { sanitizarDocumento } from '../../core/utils/util-documento';
import { formatearTelefono } from '../../core/utils/util-telefono';
import { aFormatoCanonico } from '../../core/utils/util-nombre';

@Component({
  selector: 'app-admision',
  imports: [FormsModule, DatePipe, TelefonoPipe, NombreNaturalPipe],
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
   * Genera o anula el acceso de los pacientes. Habilitado para ADMIN, ADMISION y RESULTADOS.
   * El resto de los roles ven la lista pero no pueden ejecutar acciones de escritura.
   */
  readonly puedeEditarCuenta = computed(() =>
    this.auth.tieneAlgunRol(['ADMIN', 'ADMISION', 'RESULTADOS']),
  );

  readonly pacientes = signal<PacienteAdmision[]>([]);
  readonly cargando = signal(false);
  readonly pagina = signal(0);
  readonly totalPaginas = signal(0);
  readonly totalElementos = signal(0);
  readonly tamano = 10;

  fdni = '';
  readonly notice = signal<{ type: 'ok' | 'error'; text: string } | null>(null);

  /** Orden: null = default backend (fechaCreacion desc). */
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

  // Modal generar
  readonly genOpen = signal(false);
  readonly generando = signal(false);
  genDni = '';
  genNombre = '';
  genTelefono = '';
  /** Switch para decidir si se envia el SMS de la credencial (solo si hay telefono). */
  enviarSms = false;
  readonly regenerando = signal(false);
  /** true = alta de un DNI nuevo (DNI editable). */
  readonly nuevoUsuario = signal(false);
  /** Marca el campo "Nombre completo" como tocado al perder foco o al intentar enviar. */
  readonly genNombreTocado = signal(false);

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
    this.srv.listarPacientes({
      dni: this.fdni.trim() || undefined,
      pagina: this.pagina(),
      tamano: this.tamano,
      ordenarPor: this.ordenarPor() ?? undefined,
      direccion: this.direccion() ?? undefined,
    }).subscribe({
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
    this.genTelefono = p.telefono ?? '';
    this.enviarSms = !!p.telefono;
    this.regenerando.set(p.estadoCuenta === 'ANULADO');
    this.nuevoUsuario.set(false);
    this.genNombreTocado.set(false);
    this.credencial.set(null);
    this.genOpen.set(true);
  }

  /** Alta de un paciente/DNI nuevo (no requiere que exista en la lista). */
  abrirGenerarNuevo(): void {
    this.genDni = '';
    this.genNombre = '';
    this.genTelefono = '';
    this.enviarSms = false;
    this.regenerando.set(false);
    this.nuevoUsuario.set(true);
    this.genNombreTocado.set(false);
    this.credencial.set(null);
    this.genOpen.set(true);
  }

  cerrarGenerar(): void {
    this.genOpen.set(false);
  }

  dniGenValido(): boolean {
    return /^[A-Z0-9]{8,12}$/.test(this.genDni.trim());
  }

  nombreGenValido(): boolean {
    return this.genNombre.trim().length > 0;
  }

  /** Muestra el error del nombre solo después de que el operador interactuó o intentó enviar. */
  mostrarErrorNombre(): boolean {
    return this.genNombreTocado() && !this.nombreGenValido();
  }

  onGenNombreBlur(): void {
    this.genNombreTocado.set(true);
  }

  /**
   * Vista formateada del telefono para mostrar en el modal (input + label del
   * checkbox). Internamente `genTelefono` se conserva solo con digitos para que
   * el backend lo reciba limpio; el formateo es puramente cosmético.
   */
  get telefonoFmt(): string {
    return formatearTelefono(this.genTelefono);
  }
  set telefonoFmt(v: string) {
    this.genTelefono = (v ?? '').replace(/\D/g, '').slice(0, 20);
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

  /** Handler de input para el DNI del paciente generado en admisión: max 12 dígitos. */
  onGenDniInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.genDni = sanitizarDocumento(input.value, 12);
    input.value = this.genDni;
  }

  /** Handler de input para el filtro de DNI en admisión: max 12 dígitos. */
  onFiltroDniInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.fdni = sanitizarDocumento(input.value, 12);
    input.value = this.fdni;
  }

  generar(): void {
    if (this.generando()) {
      return;
    }
    if (!this.dniGenValido()) {
      this.genNombreTocado.set(true);
      this.notice.set({ type: 'error', text: 'Ingresa un documento válido de 8 a 12 dígitos.' });
      return;
    }
    if (!this.nombreGenValido()) {
      this.genNombreTocado.set(true);
      this.notice.set({ type: 'error', text: 'El nombre completo es obligatorio.' });
      return;
    }
    this.generando.set(true);
    this.srv
      .generarUsuario({
        dni: this.genDni,
        nombre: aFormatoCanonico(this.genNombre),
        telefono: this.genTelefono.trim() || undefined,
        enviarSms: this.enviarSms,
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
    const fechaVigencia = new Date();
    fechaVigencia.setDate(fechaVigencia.getDate() + 180);
    const dd = String(fechaVigencia.getDate()).padStart(2, '0');
    const mm = String(fechaVigencia.getMonth() + 1).padStart(2, '0');
    const yyyy = fechaVigencia.getFullYear();
    const fechaVigenciaFmt = `${dd}/${mm}/${yyyy}`;
    const fechaVigenciaEsc = fechaVigenciaFmt.replace(/\//g, '&#x2F;');

    const html = this.construirHtmlTicket(c, fechaVigenciaEsc);
    this.imprimirEnIframe(html);
  }

  private construirHtmlTicket(c: CredencialResponse, fechaVigenciaEsc: string): string {
    return `<!DOCTYPE html>
<html><head><title>Credencial ${c.usuario}</title>
<style id="ticket-style">
  @page { size: 80mm 100mm; margin: 0; }
  html, body { width: 80mm; margin: 0; padding: 0; }
  body {
    font-family: Arial, Helvetica, sans-serif;
    color: #1f2937;
    padding: 2mm 4mm 3mm 4mm;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
  }
  .header {
    text-align: center;
    flex: 0 0 auto;
    font-size: 11px;
    font-weight: 800;
    letter-spacing: 0.4px;
    color: #043d36;
    padding-bottom: 2mm;
    margin-bottom: 3mm;
    border-bottom: 1px dashed #043d36;
  }
  .datos {
    flex: 0 0 auto;
    display: grid;
    grid-template-columns: 16mm 1fr;
    column-gap: 2mm;
    row-gap: 1mm;
    font-size: 10px;
    line-height: 1.25;
  }
  .datos .k { color: #6b7280; text-transform: uppercase; font-size: 8px; }
  .datos .v {
    color: #1f2937;
    font-weight: 700;
    min-width: 0;
    word-break: break-all;
    overflow-wrap: anywhere;
  }
  .datos .v.cred-big { font-size: 13px; letter-spacing: 0.5px; }
  .qr-wrap {
    flex: 0 0 auto;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-top: 6mm;
  }
  .qr-wrap img { width: 28mm; height: 28mm; display: block; }
  .footer {
    flex: 0 0 auto;
    text-align: center;
    font-size: 10.5px;
    color: #1f2937;
    font-weight: 700;
    text-transform: uppercase;
    margin-top: 4mm;
    padding-top: 2mm;
    border-top: 1px dashed #043d36;
  }
</style></head><body>
  <div class="header">SERVICIOS MEDICOS ASOCIADOS OSTEOPERU SAC</div>
  <div class="datos">
    <div class="k">WEB</div><div class="v">${c.portalUrl}</div>
    <div class="k">Nombre</div><div class="v">${c.nombre ?? '-'}</div>
    <div class="k">Usuario</div><div class="v cred-big">${c.usuario}</div>
    <div class="k">Clave</div><div class="v cred-big">${c.clave}</div>
  </div>
  <div class="qr-wrap"><img src="assets/qr-osteoperu.png" alt="QR" /></div>
  <div class="footer">
    Sus resultados estar&aacute;n disponibles hasta el <b> ${fechaVigenciaEsc}</b>.
    Aseg&uacute;rese de descargarlos dentro de este plazo.
  </div>
</body></html>`;
  }

  private imprimirEnIframe(html: string): void {
    const iframe = document.createElement('iframe');
    iframe.setAttribute('aria-hidden', 'true');
    iframe.setAttribute('title', 'ticket-impresion');
    iframe.style.position = 'fixed';
    iframe.style.right = '0';
    iframe.style.bottom = '0';
    iframe.style.width = '0';
    iframe.style.height = '0';
    iframe.style.border = '0';

    const cleanup = () => {
      if (iframe.parentNode) {
        iframe.parentNode.removeChild(iframe);
      }
    };

    iframe.srcdoc = html;

    iframe.addEventListener('load', () => {
      const doc = iframe.contentDocument;
      if (!doc) {
        cleanup();
        return;
      }
      const hMm = Math.max(70, (doc.body.scrollHeight / (96 / 25.4)) + 20);
      doc.documentElement.style.height = hMm + 'mm';
      doc.body.style.height = hMm + 'mm';
      const styleEl = doc.getElementById('ticket-style') as HTMLStyleElement | null;
      if (styleEl?.textContent) {
        styleEl.textContent = styleEl.textContent.replace(
          /@page\s*\{[^}]*\}/,
          `@page { size: 80mm ${hMm}mm; margin: 0; }`,
        );
      }
      const win = iframe.contentWindow;
      if (!win) {
        cleanup();
        return;
      }
      win.addEventListener('afterprint', cleanup, { once: true });
      setTimeout(() => {
        win.print();
        setTimeout(cleanup, 30000);
      }, 150);
    });
    document.body.appendChild(iframe);
  }

  private msg(e: HttpErrorResponse): string {
    if (e.status === 0) {
      return 'No se pudo conectar con el servidor.';
    }
    return e.error?.message ?? 'Ocurrió un error. Intenta nuevamente.';
  }
}
