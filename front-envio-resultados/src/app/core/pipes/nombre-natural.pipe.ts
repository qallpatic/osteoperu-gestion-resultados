import { Pipe, PipeTransform } from '@angular/core';

import { formatearNombreNatural } from '../utils/util-nombre';

/**
 * Pipe para mostrar nombres en formato natural ("NOMBRES APELLIDOS") en vez
 * del formato canonico del backend ("APELLIDOS, NOMBRES").
 *
 * Uso en templates: {{ paciente.nombre | nombreNatural }}
 */
@Pipe({ name: 'nombreNatural', standalone: true, pure: true })
export class NombreNaturalPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    return formatearNombreNatural(value);
  }
}
