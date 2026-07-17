/**
 * Sanitiza un valor de documento (DNI/CE), dejando únicamente los dígitos.
 * Opcionalmente limita la longitud del resultado.
 */
export function sanitizarDocumento(valor: string, maxLen = 12): string {
  if (!valor) {
    return '';
  }
  return valor.replace(/\D/g, '').slice(0, maxLen);
}
