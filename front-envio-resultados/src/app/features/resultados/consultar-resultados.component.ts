import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

import { ResultadosService } from './resultados.service';
import { Archivo, ResultadoDetalle, ResultadoResumen } from './resultados.models';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';

interface ArchivoEditable extends Archivo {
  /** Estado local pendiente de guardar (true = sigue activo). */
  marcadoActivo: boolean;
}

interface ConfirmacionPendiente {
  titulo: string;
  mensaje: string;
  detalle?: string;
  textoAceptar: string;
  textoCancelar: string;
  variante: 'warn' | 'danger';
  onAceptar: () => void;
}

@Component({
  selector: 'app-resultados-consultar',
  imports: [FormsModule, DatePipe, ConfirmDialogComponent],
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

  readonly notice = signal<{ type: 'ok' | 'error'; text: string } | null>(null);

  // Detalle / edicion de telefono y nombre
  readonly detalle = signal<ResultadoDetalle | null>(null);
  readonly cargandoDetalle = signal(false);
  readonly guardando = signal(false);
  editTelefono = '';
  editNombre = '';
  reenviar = false;

  // Edicion de archivos (modal separado)
  readonly editArchivosAbierto = signal(false);
  readonly editArchivosIdResultado = signal<number | null>(null);
  readonly editArchivosArchivos = signal<ArchivoEditable[]>([]);
  readonly editArchivosNuevos = signal<File[]>([]);
  /** Override individual del motivo por archivo (id -> motivo). Opcional. */
  readonly motivosPorArchivo = signal<Record<number, string>>({});
  editArchivosMotivo = '';
  editArchivosReenviar = false;
  readonly guardandoEdicion = signal(false);
  readonly toastExito = signal<string | null>(null);
  private toastTimer: ReturnType<typeof setTimeout> | null = null;
  /** Ultimo motivo persistido (sobrevive entre guardados para no reescribirlo). */
  private ultimoMotivoUsado = '';

  // Confirmacion estilizada (reemplaza window.confirm)
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
        // Si el operador ya uso un motivo en esta sesion, lo mantenemos.
        // Si no, lo inicializamos con el ultimo motivo persistido en la cabecera
        // (para ver el contexto historico de por que se desactivo algo).
        const motivoExistente = d.archivos.find((a) => a.motivoEliminacion)?.motivoEliminacion;
        this.editArchivosMotivo = this.ultimoMotivoUsado || motivoExistente || '';
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
  }

  toggleArchivoActivo(a: ArchivoEditable): void {
    if (!a.marcadoActivo) {
      // Reactivar no requiere confirmacion.
      this.editArchivosArchivos.set(
        this.editArchivosArchivos().map((x) =>
          x.id === a.id ? { ...x, marcadoActivo: true } : x,
        ),
      );
      // Limpiar override individual: vuelve a heredar el global.
      const copia = { ...this.motivosPorArchivo() };
      delete copia[a.id];
      this.motivosPorArchivo.set(copia);
      return;
    }
    const restantesSiDesactivo = this.contarActivosSiDesactivo(a.id);
    if (restantesSiDesactivo <= 0) {
      this.notice.set({
        type: 'error',
        text: 'La cabecera debe quedar con al menos un archivo activo.',
      });
      return;
    }
    this.confirmacion.set({
      titulo: 'Desactivar archivo',
      mensaje: 'El paciente dejara de verlo en su visor.',
      detalle: a.nombreOriginal,
      textoAceptar: 'Si, desactivar',
      textoCancelar: 'Cancelar',
      variante: 'warn',
      onAceptar: () => this.aplicarToggle(a, false),
    });
  }

  private aplicarToggle(a: ArchivoEditable, nuevoEstado: boolean): void {
    this.editArchivosArchivos.set(
      this.editArchivosArchivos().map((x) =>
        x.id === a.id ? { ...x, marcadoActivo: nuevoEstado } : x,
      ),
    );
    // Al desactivar, pre-llenar el motivo individual con el global
    // para que el operador pueda ajustarlo si lo necesita.
    if (!nuevoEstado && !this.motivosPorArchivo()[a.id]) {
      this.motivosPorArchivo.set({
        ...this.motivosPorArchivo(),
        [a.id]: this.editArchivosMotivo || '',
      });
    }
  }

  /** Actualiza el motivo individual de un archivo marcado para desactivar. */
  actualizarMotivoIndividual(idArchivo: number, motivo: string): void {
    this.motivosPorArchivo.set({ ...this.motivosPorArchivo(), [idArchivo]: motivo });
  }

  /** True si el operador personalizo el motivo individual (override del global). */
  motivoIndividualPersonalizado(idArchivo: number): boolean {
    const m = this.motivosPorArchivo()[idArchivo];
    return m != null && m !== this.editArchivosMotivo;
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
      if (file.size > 50 * 1024 * 1024) {
        rechazados.push(`${file.name}: supera 50 MB`);
      } else {
        aceptados.push(file);
      }
    }
    if (aceptados.length) {
      this.editArchivosNuevos.set([...this.editArchivosNuevos(), ...aceptados]);
    }
    if (rechazados.length) {
      this.notice.set({ type: 'error', text: rechazados.join(' · ') });
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

    // Solo enviar los overrides individuales para los IDs efectivamente a desactivar.
    // Si el motivo individual coincide con el global, NO se envia (es redundante).
    const motivosPorArchivo: Record<number, string> = {};
    for (const id of idsDesactivar) {
      const m = this.motivosPorArchivo()[id];
      if (m != null && m.trim() !== '' && m.trim() !== this.editArchivosMotivo.trim()) {
        motivosPorArchivo[id] = m.trim();
      }
    }

    const ejecutarGuardado = () => {
      this.guardandoEdicion.set(true);
      this.toastExito.set(null);
      this.srv
        .actualizarArchivos(idResultado, {
          idsDesactivar,
          idsReactivar,
          archivosNuevos: nuevos,
          motivo: this.editArchivosMotivo,
          motivosPorArchivo: Object.keys(motivosPorArchivo).length ? motivosPorArchivo : undefined,
          reenviarSms: this.editArchivosReenviar,
        })
        .subscribe({
          next: (res) => {
            this.guardandoEdicion.set(false);
            // Refrescar el signal del modal con la respuesta del backend
            // (incluye los archivos nuevos con sus IDs y el motivo persistido).
            // NO limpiamos motivo: queda visible/editables para una segunda pasada.
            // NO limpiamos los switches: vuelven a su estado real persistido.
            this.editArchivosArchivos.set(
              res.archivos.map((a) => ({ ...a, marcadoActivo: a.activo })),
            );
            this.editArchivosNuevos.set([]);
            this.motivosPorArchivo.set({});
            this.editArchivosMotivo = res.archivos.find((a) => a.motivoEliminacion)
              ? (this.editArchivosMotivo || this.ultimoMotivoUsado)
              : this.editArchivosMotivo;
            this.ultimoMotivoUsado = res.archivos.find((a) => a.motivoEliminacion)?.motivoEliminacion
              ?? this.ultimoMotivoUsado;
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
    };

    if (idsDesactivar.length) {
      this.confirmacion.set({
        titulo: `Desactivar ${idsDesactivar.length} archivo(s)`,
        mensaje: 'El paciente dejara de verlos en su visor. El motivo quedara registrado en la bitacora.',
        textoAceptar: 'Si, desactivar',
        textoCancelar: 'Cancelar',
        variante: 'warn',
        onAceptar: () => ejecutarGuardado(),
      });
    } else {
      ejecutarGuardado();
    }
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

  ejecutarConfirmacion(): void {
    const c = this.confirmacion();
    if (!c) {
      return;
    }
    this.confirmacion.set(null);
    c.onAceptar();
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