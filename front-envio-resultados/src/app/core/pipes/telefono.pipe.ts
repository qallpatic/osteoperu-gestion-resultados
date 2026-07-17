import { Pipe, PipeTransform } from '@angular/core';

import { formatearTelefono } from '../utils/util-telefono';

@Pipe({
  name: 'telefono',
  standalone: true,
})
export class TelefonoPipe implements PipeTransform {
  transform(telefono: string | null | undefined): string {
    return formatearTelefono(telefono);
  }
}
