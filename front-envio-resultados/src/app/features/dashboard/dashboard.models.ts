/** Modelos del módulo Dashboard (consumido solo por rol ADMIN). */

export interface PuntoSerieDiaria {
  /** Fecha local (YYYY-MM-DD) ya formateada por el backend. */
  fecha: string;
  total: number;
}

export interface PicoRegistros {
  fecha: string;
  total: number;
}

export interface DashboardResumen {
  totalUsuarios: number;
  totalResultados: number;
  totalArchivos: number;
  /** Peso total acumulado en MB. Considera TODOS los archivos (incluso soft-deleted). */
  pesoTotalMb: number;
  totalVistas: number;
  picoUsuarios: PicoRegistros | null;
  desde: string;
  hasta: string;
  serieResultadosPorDia: PuntoSerieDiaria[];
  serieUsuariosPorDia: PuntoSerieDiaria[];
}

export interface DashboardParams {
  /** YYYY-MM-DD. Si es null, el backend usa default (hoy - 29 dias). */
  desde?: string;
  /** YYYY-MM-DD. Si es null, el backend usa default (hoy). */
  hasta?: string;
}

// ===== Pestana Almacenamiento =====

export interface DistribucionTipoContenido {
  tipoContenido: string;
  cantidad: number;
  pesoMb: number;
}

export interface ArchivoPesado {
  idGenerado: string;
  nombreOriginal: string;
  tipoContenido: string;
  pesoMb: number;
}

export interface PuntoAcumuladoMb {
  fecha: string;
  mbAcumulado: number;
}

export interface AlmacenamientoDetalle {
  desde: string;
  hasta: string;
  distribucionPorTipo: DistribucionTipoContenido[];
  archivosMasPesados: ArchivoPesado[];
  acumuladoMb: PuntoAcumuladoMb[];
}

// ===== Pestana Vistas =====

export interface ResultadoMasVisto {
  idGenerado: string;
  documentoIdentidad: string;
  nombrePaciente: string;
  contadorVistas: number;
  fechaHoraCarga: string;
}

export interface BucketVistas {
  etiqueta: string;
  cantidad: number;
}

export interface PuntoVistaDiaria {
  fecha: string;
  total: number;
}

export interface VistasDetalle {
  desde: string;
  hasta: string;
  resultadosMasVistos: ResultadoMasVisto[];
  distribucionBuckets: BucketVistas[];
  curvaTemporal: PuntoVistaDiaria[];
}
