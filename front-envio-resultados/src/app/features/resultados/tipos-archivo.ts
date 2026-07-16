/** Whitelist de tipos MIME aceptados por la aplicacion (PDF, imagen o video). */
const ACCEPTED_MIME_PREFIXES: readonly string[] = ['application/pdf', 'image/', 'video/'];

/** Extension fallback cuando el navegador no detecta MIME (e.g. archivos sin extension conocida). */
const ACCEPTED_EXTENSIONS = /\.(pdf|png|jpe?g|gif|webp|bmp|svg|mp4|mov|avi|webm|mkv|m4v|ogv)$/i;

/** Tamanio maximo por archivo (debe coincidir con el backend). */
export const MAX_FILE_BYTES = 50 * 1024 * 1024;

/**
 * Devuelve true si el archivo es PDF, imagen o video. Confiamos primero en el
 * MIME (mas confiable) y caemos a la extension como fallback cuando el
 * navegador reporta application/octet-stream.
 */
export function esTipoSoportado(file: File): boolean {
  const mime = (file.type || '').toLowerCase();
  if (mime) {
    if (ACCEPTED_MIME_PREFIXES.some((p) => mime.startsWith(p))) {
      return true;
    }
    // MIME reconocido pero no soportado (ej. application/zip,
    // application/vnd.ms-excel, application/vnd.openxmlformats-...)
    if (mime !== 'application/octet-stream') {
      return false;
    }
  }
  return ACCEPTED_EXTENSIONS.test(file.name);
}

/** Namespace para usar como `TiposArchivoSoportados.esSoportado(file)`. */
export const TiposArchivoSoportados = {
  esSoportado: esTipoSoportado,
  MAX_FILE_BYTES,
} as const;