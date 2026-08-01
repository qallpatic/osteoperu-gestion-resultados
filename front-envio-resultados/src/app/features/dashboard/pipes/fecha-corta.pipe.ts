import { Pipe, PipeTransform } from '@angular/core';

const MESES_ES = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun',
                  'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];

/**
 * Formatea una fecha a un formato compacto localizado: "27-Jul".
 * Pensado para etiquetas de eje X en graficos y celdas donde el espacio
 * es escaso y se quiere identificar el dia/mes rapidamente.
 *
 * Acepta tanto string ISO (YYYY-MM-DD) como Date. No usa LOCALE_ID de Angular
 * (los meses son siempre espanol abreviado) para poder instanciarlo fuera del
 * inyector (ej. callbacks de Chart.js).
 */
@Pipe({ name: 'fechaCorta', standalone: true, pure: true })
export class FechaCortaPipe implements PipeTransform {
  transform(value: string | Date | null | undefined): string {
    if (!value) {
      return '';
    }
    const d = typeof value === 'string' ? new Date(`${value}T00:00:00`) : value;
    if (Number.isNaN(d.getTime())) {
      return '';
    }
    const dd = String(d.getDate()).padStart(2, '0');
    return `${dd}-${MESES_ES[d.getMonth()]}`;
  }
}