/**
 * Sanitiza un valor de documento (DNI/CE): deja únicamente letras y números,
 * fuerza mayúsculas y opcionalmente limita la longitud del resultado.
 */
export function sanitizarDocumento(valor: string, maxLen = 12): string {
  if (!valor) {
    return '';
  }
  return valor.replace(/[^A-Za-z0-9]/g, '').toUpperCase().slice(0, maxLen);
}
