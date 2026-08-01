/**
 * Helpers para formatear bytes/MB en valores legibles para humanos.
 * Mantener sincronizado con `resultado_archivo.peso_mb` (MB, BigDecimal
 * precision 12 scale 4) y con la realidad del bucket.
 */

export interface PesoFormateado {
  /** Valor principal (1.4) ya redondeado a 1 decimal. */
  valor: number;
  /** Unidad legible: B / KB / MB / GB / TB. */
  unidad: 'B' | 'KB' | 'MB' | 'GB' | 'TB';
  /** Cadena final lista para mostrar: "1.4 GB". */
  texto: string;
}

/**
 * Convierte MB (tal como lo devuelve el backend) a la unidad mas legible.
 * Devuelve un objeto para que el template pueda decidir que mostrar.
 */
export function formatearPeso(mb: number | null | undefined): PesoFormateado {
  if (mb == null || Number.isNaN(mb) || mb < 0) {
    return { valor: 0, unidad: 'MB', texto: '0 MB' };
  }
  const bytes = mb * 1024 * 1024;
  if (bytes < 1024) {
    return { valor: Math.round(bytes), unidad: 'B', texto: `${Math.round(bytes)} B` };
  }
  const kb = bytes / 1024;
  if (kb < 1024) {
    return { valor: round1(kb), unidad: 'KB', texto: `${round1(kb)} KB` };
  }
  if (mb < 1024) {
    return { valor: round1(mb), unidad: 'MB', texto: `${round1(mb)} MB` };
  }
  const gb = mb / 1024;
  if (gb < 1024) {
    return { valor: round1(gb), unidad: 'GB', texto: `${round1(gb)} GB` };
  }
  const tb = gb / 1024;
  return { valor: round1(tb), unidad: 'TB', texto: `${round1(tb)} TB` };
}

function round1(n: number): number {
  return Math.round(n * 10) / 10;
}