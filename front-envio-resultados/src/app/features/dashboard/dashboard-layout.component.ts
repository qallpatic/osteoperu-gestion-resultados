import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Shell de la seccion Dashboard. No renderiza UI propia — el menu lateral
 * (main-layout) ya muestra los sub-items. Solo provee el outlet donde se
 * cargan las rutas hijas (`/dashboard/resumen`, `/dashboard/resultados`, etc.).
 */
@Component({
  selector: 'app-dashboard-layout',
  imports: [RouterOutlet],
  template: '<router-outlet />',
})
export class DashboardLayoutComponent {}