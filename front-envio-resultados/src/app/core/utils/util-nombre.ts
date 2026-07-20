/**
 * Helpers para formatear nombres de pacientes.
 *
 * <p>El backend almacena el nombre en formato canonico RENIEC
 * {@code "APELLIDO_P APELLIDO_M, NOMBRES"} (separado por coma). En pantalla
 * queremos mostrarlo al reves, como estilo "natural" en espanol:
 * {@code "NOMBRES APELLIDO_P APELLIDO_M"}.
 *
 * <p>Estas funciones son la frontera entre el formato interno (BD, parser,
 * SMS) y el formato visible para el usuario. NO cambian el dato que viaja al
 * backend: en el {@code AdmisionComponent} el operador puede tipear en el
 * formato nuevo y se reconvierte a canonico antes de enviar.
 */
export function formatearNombreNatural(nombreCompleto: string | null | undefined): string {
  if (!nombreCompleto) {
    return '';
  }
  const coma = nombreCompleto.indexOf(',');
  if (coma < 0) {
    return nombreCompleto;
  }
  const apellidos = nombreCompleto.substring(0, coma).trim();
  const nombres = nombreCompleto.substring(coma + 1).trim();
  if (!apellidos) {
    return nombres;
  }
  if (!nombres) {
    return apellidos;
  }
  return `${nombres} ${apellidos}`;
}

/**
 * Inverso de {@link #formatearNombreNatural}: toma un nombre en formato
 * "NOMBRES APELLIDO_P APELLIDO_M" (lo que tipea el operador) y lo devuelve
 * en formato canonico "APELLIDO_P APELLIDO_M, NOMBRES" para enviar al
 * backend. Si no se detecta el patron "natural", devuelve el string sin
 * cambios para que el {@code NombreParser} del backend haga su mejor
 * esfuerzo.
 */
export function aFormatoCanonico(nombreCompleto: string | null | undefined): string {
  if (!nombreCompleto) {
    return '';
  }
  const normalizado = nombreCompleto.trim().replace(/\s+/g, ' ');
  if (!normalizado) {
    return '';
  }
  // Si ya tiene coma, asumimos formato canonico y lo devolvemos tal cual.
  if (normalizado.includes(',')) {
    return normalizado;
  }
  const tokens = normalizado.split(' ');
  // 1 token: solo apellido.
  if (tokens.length === 1) {
    return normalizado;
  }
  // 2 tokens: apellido(s) + nombres. Asumimos 1 apellido + 1 nombre.
  // 3 tokens: apellido(s) + nombres. Asumimos 2 apellidos + 1 nombre.
  // 4+ tokens: asumimos 2 apellidos + 2 nombres.
  let cantApellidos: number;
  if (tokens.length === 2) {
    cantApellidos = 1;
  } else if (tokens.length === 3) {
    cantApellidos = 2;
  } else {
    cantApellidos = 2;
  }
  const apellidos = tokens.slice(0, cantApellidos).join(' ');
  const nombres = tokens.slice(cantApellidos).join(' ');
  return `${apellidos}, ${nombres}`;
}
