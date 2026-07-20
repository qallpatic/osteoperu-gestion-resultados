import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

import { ResultadosService } from './resultados.service';
import { Archivo, ResultadoDetalle, ResultadoResumen } from './resultados.models';
import { sanitizarDocumento } from '../../core/utils/util-documento';
import { TiposArchivoSoportados, esTipoSoportado } from './tipos-archivo';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
import { NombreNaturalPipe } from '../../core/pipes/nombre-natural.pipe';

interface ArchivoEditable extends Archivo {
  marcadoActivo: boolean;
}

interface ConfirmacionPendiente {
  titulo: string;
  mensaje: string;
  detalle?: string;
  textoAceptar: string;
  textoCancelar: string;
  variante: 'warn' | 'danger';
  /** Si true, el dialog muestra un input de texto y emite su valor en (confirmar). */
  conMotivo?: boolean;
  motivoPlaceholder?: string;
  motivoInicial?: string;
  /** Recibe el motivo tipeado (o undefined si no hay input). */
  onAceptar: (motivo?: string) => void;
}

@Component({
  selector: 'app-resultados-consultar',
  imports: [FormsModule, DatePipe, ConfirmDialogComponent, NombreNaturalPipe],
  templateUrl: './consultar-resultados.component.html',
  styleUrl: './consultar-resultados.component.css',
})
export class ConsultarResultadosComponent implements OnInit {
  private readonly srv = inject(ResultadosService);

  readonly resultados = signal<ResultadoResumen[]>([]);
  readonly cargando = signal(false);
  readonly pagina = signal(0);
  readonly totalPaginas = signal(0);
  readonly totalElementos = signal(0);
  readonly tamano = 10;

  fdni = '';
  fnombre = '';
  fdesde = '';
  fhasta = '';

  /** Orden: null = default backend (fechaHoraCarga desc). */
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

  /** Handler de input para el filtro de DNI: max 12 dígitos. */
  onDniInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.fdni = sanitizarDocumento(input.value, 12);
    input.value = this.fdni;
  }

  readonly notice = signal<{ type: 'ok' | 'error'; text: string } | null>(null);

  readonly detalle = signal<ResultadoDetalle | null>(null);
  readonly cargandoDetalle = signal(false);
  readonly guardando = signal(false);
  editTelefono = '';
  editNombre = '';
  reenviar = false;

  // Edicion de archivos
  readonly editArchivosAbierto = signal(false);
  readonly editArchivosIdResultado = signal<number | null>(null);
  readonly editArchivosArchivos = signal<ArchivoEditable[]>([]);
  readonly editArchivosNuevos = signal<File[]>([]);
  readonly motivosPorArchivo = signal<Record<number, string>>({});
  editArchivosReenviar = false;
  readonly guardandoEdicion = signal(false);
  readonly toastExito = signal<string | null>(null);
  /** Errores que deben verse aunque el modal este abierto (no quedan tapados por el backdrop). */
  readonly errorModalEdicion = signal<string | null>(null);
  private toastTimer: ReturnType<typeof setTimeout> | null = null;
  private errorModalTimer: ReturnType<typeof setTimeout> | null = null;
  /** Ultimo motivo persistido: pre-rellena el siguiente dialog para evitar re-tipear. */
  private ultimoMotivoUsado = '';

  readonly confirmacion = signal<ConfirmacionPendiente | null>(null);

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
    this.srv
      .listar({
        dni: this.fdni.trim() || undefined,
        nombre: this.fnombre.trim() || undefined,
        desde: this.fdesde || undefined,
        hasta: this.fhasta || undefined,
        pagina: this.pagina(),
        tamano: this.tamano,
        ordenarPor: this.ordenarPor() ?? undefined,
        direccion: this.direccion() ?? undefined,
      })
      .subscribe({
        next: (p) => {
          this.resultados.set(p.contenido);
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

  verDetalle(r: ResultadoResumen): void {
    this.cargandoDetalle.set(true);
    this.detalle.set(null);
    this.srv.detalle(r.id).subscribe({
      next: (d) => {
        this.detalle.set(d);
        this.editTelefono = d.telefono ?? '';
        this.editNombre = d.nombrePaciente ?? '';
        this.reenviar = false;
        this.cargandoDetalle.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.cargandoDetalle.set(false);
        this.notice.set({ type: 'error', text: this.msg(e) });
      },
    });
  }

  cerrarDetalle(): void {
    this.detalle.set(null);
  }

  guardarCambios(): void {
    const d = this.detalle();
    if (!d) {
      return;
    }
    this.guardando.set(true);
    this.srv
      .actualizar(d.id, {
        telefono: this.editTelefono || undefined,
        nombrePaciente: this.editNombre || undefined,
        reenviarSms: this.reenviar,
      })
      .subscribe({
        next: (res) => {
          this.guardando.set(false);
          this.detalle.set(res);
          this.notice.set({
            type: 'ok',
            text: this.reenviar ? 'Resultado actualizado y SMS reenviado.' : 'Resultado actualizado.',
          });
          this.cargar();
        },
        error: (e: HttpErrorResponse) => {
          this.guardando.set(false);
          this.notice.set({ type: 'error', text: this.msg(e) });
        },
      });
  }

  abrirEdicionArchivos(r: ResultadoResumen): void {
    if (r.estado !== 1) {
      return;
    }
    this.editArchivosIdResultado.set(r.id);
    this.srv.detalle(r.id).subscribe({
      next: (d) => {
        this.editArchivosArchivos.set(
          d.archivos.map((a) => ({ ...a, marcadoActivo: a.activo })),
        );
        this.editArchivosNuevos.set([]);
        this.motivosPorArchivo.set({});
        this.editArchivosReenviar = false;
        this.editArchivosAbierto.set(true);
        this.toastExito.set(null);
      },
      error: (e: HttpErrorResponse) =>
        this.notice.set({ type: 'error', text: this.msg(e) }),
    });
  }

  cerrarEdicionArchivos(): void {
    if (this.guardandoEdicion()) {
      return;
    }
    this.editArchivosAbierto.set(false);
    this.editArchivosIdResultado.set(null);
    this.editArchivosArchivos.set([]);
    this.editArchivosNuevos.set([]);
    this.motivosPorArchivo.set({});
    this.toastExito.set(null);
    this.errorModalEdicion.set(null);
    if (this.errorModalTimer) {
      clearTimeout(this.errorModalTimer);
      this.errorModalTimer = null;
    }
  }

  toggleArchivoActivo(event: Event, a: ArchivoEditable): void {
    const checkbox = event.target as HTMLInputElement;
    if (!a.marcadoActivo) {
      // Reactivar no requiere confirmacion.
      this.aplicarToggle(a, true);
      checkbox.checked = true;
      return;
    }
    const restantesSiDesactivo = this.contarActivosSiDesactivo(a.id);
    if (restantesSiDesactivo <= 0) {
      checkbox.checked = true;
      this.notice.set({
        type: 'error',
        text: 'La cabecera debe quedar con al menos un archivo activo.',
      });
      return;
    }
    // Revertir cambio visual hasta confirmar
    checkbox.checked = true;
    this.confirmacion.set({
      titulo: 'Desactivar archivo',
      mensaje: 'El paciente dejara de verlo en su visor.',
      detalle: a.nombreOriginal,
      textoAceptar: 'Si, desactivar',
      textoCancelar: 'Cancelar',
      variante: 'warn',
      conMotivo: true,
      motivoPlaceholder: 'Motivo (opcional · queda en la bitacora)',
      motivoInicial: this.ultimoMotivoUsado,
      onAceptar: (motivo) => {
        this.aplicarToggle(a, false);
        checkbox.checked = false;
        if (motivo) {
          this.motivosPorArchivo.set({ ...this.motivosPorArchivo(), [a.id]: motivo });
          this.ultimoMotivoUsado = motivo;
        }
      },
    });
  }

  private aplicarToggle(a: ArchivoEditable, nuevoEstado: boolean): void {
    this.editArchivosArchivos.set(
      this.editArchivosArchivos().map((x) =>
        x.id === a.id ? { ...x, marcadoActivo: nuevoEstado } : x,
      ),
    );
    if (nuevoEstado && this.motivosPorArchivo()[a.id] != null) {
      const copia = { ...this.motivosPorArchivo() };
      delete copia[a.id];
      this.motivosPorArchivo.set(copia);
    }
  }

  private contarActivosSiDesactivo(idExcluir: number): number {
    return (
      this.editArchivosArchivos().filter((x) => x.id !== idExcluir && x.marcadoActivo).length +
      this.editArchivosNuevos().length
    );
  }

  onArchivosNuevosDrop(event: DragEvent): void {
    event.preventDefault();
    this.agregarArchivosNuevos(event.dataTransfer?.files ?? null);
  }

  onArchivosNuevosBrowse(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.agregarArchivosNuevos(input.files);
    input.value = '';
  }

  private agregarArchivosNuevos(list: FileList | null): void {
    if (!list) {
      return;
    }
    const aceptados: File[] = [];
    const rechazados: string[] = [];
    for (const file of Array.from(list)) {
      if (!TiposArchivoSoportados.esSoportado(file)) {
        rechazados.push(`${file.name}: formato no permitido (solo PDF, imagen o video)`);
      } else if (file.size > 50 * 1024 * 1024) {
        rechazados.push(`${file.name}: supera 50 MB`);
      } else {
        aceptados.push(file);
      }
    }
    if (aceptados.length) {
      this.editArchivosNuevos.set([...this.editArchivosNuevos(), ...aceptados]);
    }
    if (rechazados.length) {
      const texto = rechazados.join(' · ');
      this.notice.set({ type: 'error', text: texto });
      this.errorModalEdicion.set(texto);
      if (this.errorModalTimer) {
        clearTimeout(this.errorModalTimer);
      }
      this.errorModalTimer = setTimeout(() => this.errorModalEdicion.set(null), 6000);
    }
  }

  removerArchivoNuevo(index: number): void {
    this.editArchivosNuevos.set(this.editArchivosNuevos().filter((_, i) => i !== index));
  }

  guardarEdicionArchivos(): void {
    const archivos = this.editArchivosArchivos();
    const nuevos = this.editArchivosNuevos();
    const idsDesactivar = archivos
      .filter((a) => a.activo && !a.marcadoActivo)
      .map((a) => a.id);
    const idsReactivar = archivos
      .filter((a) => !a.activo && a.marcadoActivo)
      .map((a) => a.id);

    if (!idsDesactivar.length && !idsReactivar.length && !nuevos.length) {
      this.notice.set({ type: 'error', text: 'No hay cambios para guardar.' });
      return;
    }

    const idResultado = this.editArchivosIdResultado();
    if (!idResultado) {
      this.notice.set({ type: 'error', text: 'No se puede guardar: no hay cabecera seleccionada.' });
      return;
    }

    // Filtrar motivosPorArchivo: solo los que correspondan a IDs efectivamente a desactivar.
    const motivosPorArchivo: Record<number, string> = {};
    for (const id of idsDesactivar) {
      const m = this.motivosPorArchivo()[id];
      if (m != null && m.trim() !== '') {
        motivosPorArchivo[id] = m.trim();
      }
    }

    this.guardandoEdicion.set(true);
    this.toastExito.set(null);
    this.srv
      .actualizarArchivos(idResultado, {
        idsDesactivar,
        idsReactivar,
        archivosNuevos: nuevos,
        motivosPorArchivo: Object.keys(motivosPorArchivo).length ? motivosPorArchivo : undefined,
        reenviarSms: this.editArchivosReenviar,
      })
      .subscribe({
        next: (res) => {
          this.guardandoEdicion.set(false);
          this.editArchivosArchivos.set(
            res.archivos.map((a) => ({ ...a, marcadoActivo: a.activo })),
          );
          this.editArchivosNuevos.set([]);
          this.motivosPorArchivo.set({});
          // Recordar el ultimo motivo persistido para la proxima sesion
          const ultMotivo = res.archivos.find((a) => a.motivoEliminacion)?.motivoEliminacion;
          if (ultMotivo) {
            this.ultimoMotivoUsado = ultMotivo;
          }
          this.detalle.set(res);
          this.cargar();
          this.mostrarToastExito(
            this.editArchivosReenviar
              ? 'Archivos actualizados y SMS reencolado.'
              : 'Archivos actualizados correctamente.',
          );
        },
        error: (e: HttpErrorResponse) => {
          this.guardandoEdicion.set(false);
          this.notice.set({ type: 'error', text: this.msg(e) });
        },
      });
  }

  private mostrarToastExito(texto: string): void {
    this.toastExito.set(texto);
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
    }
    this.toastTimer = setTimeout(() => this.toastExito.set(null), 5000);
  }

  cerrarToast(): void {
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
      this.toastTimer = null;
    }
    this.toastExito.set(null);
  }

  cerrarErrorModal(): void {
    if (this.errorModalTimer) {
      clearTimeout(this.errorModalTimer);
      this.errorModalTimer = null;
    }
    this.errorModalEdicion.set(null);
  }

  ejecutarConfirmacion(motivo?: string): void {
    const c = this.confirmacion();
    if (!c) {
      return;
    }
    this.confirmacion.set(null);
    c.onAceptar(motivo);
  }

  cancelarConfirmacion(): void {
    this.confirmacion.set(null);
  }

  formatSize(mb: number): string {
    if (mb == null) {
      return '0 B';
    }
    return mb < 1
      ? `${Math.round(mb * 1024)} KB`
      : `${mb.toFixed(1)} MB`;
  }

  private msg(e: HttpErrorResponse): string {
    if (e.status === 0) {
      return 'No se pudo conectar con el servidor.';
    }
    return e.error?.message ?? 'Ocurrió un error. Intenta nuevamente.';
  }
}