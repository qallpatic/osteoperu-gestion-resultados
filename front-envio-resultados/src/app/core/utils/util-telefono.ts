export function formatearTelefono(telefono: string | null | undefined): string {
  const valor = telefono?.trim() ?? '';
  if (!valor) {
    return '';
  }

  const digitos = valor.replace(/\D/g, '');
  const numeroLocal = /^519\d{8}$/.test(digitos) ? digitos.slice(2) : digitos;

  if (!/^9\d{8}$/.test(numeroLocal)) {
    return valor;
  }

  return `${numeroLocal.slice(0, 3)} ${numeroLocal.slice(3, 6)} ${numeroLocal.slice(6)}`;
}
