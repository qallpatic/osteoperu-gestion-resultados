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

export interface PuntoVistaDiaria {
  fecha: string;
  total: number;
}

/**
 * Celda del heatmap dia-de-la-semana x hora-del-dia.
 * `diaSemana` usa la convencion MySQL DAYOFWEEK default: 1=Dom, 7=Sab.
 * El componente reordena para mostrar L M M J V S D.
 */
export interface PuntoHeatmap {
  diaSemana: number;
  hora: number;
  total: number;
}

export interface VistasDetalle {
  desde: string;
  hasta: string;
  /** Total combinado (aperturas + descargas). Conservado para back-compat. */
  curvaTemporal: PuntoVistaDiaria[];
  /** Solo aperturas del visor (base del stacked bar). */
  curvaTemporalAperturas: PuntoVistaDiaria[];
  /** Solo descargas de archivos (segmento superior del stacked bar). */
  curvaTemporalDescargas: PuntoVistaDiaria[];
  /** Eventos VISTA en el rango. */
  totalAperturas: number;
  /** Eventos DESCARGA en el rango. */
  totalDescargas: number;
  /** descargas / aperturas * 100, redondeado a 1 decimal. 0 si no hay aperturas. */
  ratioDescargaPorApertura: number;
  /** Hora del dia con mas interacciones (0-23). null si no hubo actividad. */
  horaPico: number | null;
  /** Cantidad de interacciones registradas en la hora pico (sumadas todos los dias). */
  totalHoraPico: number | null;
  /** 168 puntos (7x24) densificados con el total de interacciones por celda. */
  heatmap: PuntoHeatmap[];
}
