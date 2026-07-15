/** Modelos del módulo de resultados. */

export interface PaginaResponse<T> {
  contenido: T[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
  ultima: boolean;
}

export interface UploadResultadoResponse {
  cargaId: number | null;
  idGenerado: string | null;
  phone: string;
  dni: string;
  folderKey: string;
  totalFiles: number;
  storedFileIds: string[];
  shortLink: string;
  viewerUrl: string;
  smsStatus: string;
  requiereConfirmacion: boolean;
  nombreReniec: string | null;
  nombrePdf: string | null;
}

export interface UploadRequest {
  phone: string;
  dni: string;
  patientName: string;
  mensaje: string;
  files: File[];
  confirmarNombre: boolean;
}

export interface ResultadoResumen {
  id: number;
  idGenerado: string;
  dni: string;
  nombrePaciente: string | null;
  telefono: string | null;
  estadoSms: string;
  contadorVistas: number;
  cantidadArchivos: number;
  pesoTotalMb: number;
  estado: number;
  fechaHoraCarga: string;
  fechaPrimeraVista: string | null;
}

export interface Archivo {
  id: number;
  idGenerado: string;
  idArchivo: string;
  nombreOriginal: string;
  tipoContenido: string;
  tipoMedio: string;
  extension: string | null;
  pesoMb: number;
  fechaCreacion: string;
}

export interface ResultadoDetalle {
  id: number;
  idGenerado: string;
  dni: string;
  nombrePaciente: string | null;
  mensaje: string | null;
  telefono: string | null;
  tokenPublico: string;
  viewerUrl: string;
  estadoSms: string;
  contadorVistas: number;
  estado: number;
  fechaEliminacion: string | null;
  usuarioEliminacion: string | null;
  motivoEliminacion: string | null;
  cantidadArchivos: number;
  pesoTotalMb: number;
  fechaHoraCarga: string;
  fechaPrimeraVista: string | null;
  fechaUltimaVista: string | null;
  archivos: Archivo[];
}

export interface LogCarga {
  id: number;
  idResultado: number | null;
  dni: string | null;
  usuarioCarga: string | null;
  estado: string;
  mensaje: string | null;
  nombreReniec: string | null;
  nombrePdf: string | null;
  coincideNombre: boolean | null;
  fechaCreacion: string;
}

export interface ActualizarResultadoRequest {
  telefono?: string;
  nombrePaciente?: string;
  reenviarSms: boolean;
}
