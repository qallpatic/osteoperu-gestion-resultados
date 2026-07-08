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
  batchId: number | null;
  phone: string;
  dni: string;
  folderName: string;
  totalFiles: number;
  storedFiles: string[];
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
  dni: string;
  nombrePaciente: string | null;
  telefono: string;
  estadoSms: string;
  contadorVistas: number;
  fechaCreacion: string;
  fechaPrimeraVista: string | null;
}

export interface Archivo {
  id: number;
  nombreOriginal: string;
  tipoMedio: string;
  tamanoBytes: number;
  fechaCreacion: string;
}

export interface ResultadoDetalle {
  id: number;
  dni: string;
  nombrePaciente: string | null;
  mensaje: string | null;
  telefono: string;
  carpeta: string;
  tokenPublico: string;
  viewerUrl: string;
  estadoSms: string;
  contadorVistas: number;
  fechaPrimeraVista: string | null;
  fechaUltimaVista: string | null;
  fechaCreacion: string;
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
