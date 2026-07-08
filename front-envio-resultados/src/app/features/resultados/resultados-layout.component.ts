import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/** Contenedor de la sección Resultados. La navegación (Cargar/Consultar/Logs)
 *  vive en el menú lateral como submenús. */
@Component({
  selector: 'app-resultados-layout',
  imports: [RouterOutlet],
  template: '<router-outlet />',
})
export class ResultadosLayoutComponent {}
