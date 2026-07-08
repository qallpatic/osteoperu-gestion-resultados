import { Routes } from '@angular/router';

import { authGuard, guestGuard, roleGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./core/layout/main-layout.component').then((m) => m.MainLayoutComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'resultados',
        canActivate: [roleGuard(['ADMIN', 'RESULTADOS'])],
        loadComponent: () =>
          import('./features/resultados/resultados-layout.component').then(
            (m) => m.ResultadosLayoutComponent,
          ),
        children: [
          { path: '', pathMatch: 'full', redirectTo: 'cargar' },
          {
            path: 'cargar',
            loadComponent: () =>
              import('./features/resultados/upload.component').then((m) => m.UploadComponent),
          },
          {
            path: 'pendientes',
            loadComponent: () =>
              import('./features/resultados/pendientes-resultados.component').then(
                (m) => m.PendientesResultadosComponent,
              ),
          },
          {
            path: 'consultar',
            loadComponent: () =>
              import('./features/resultados/consultar-resultados.component').then(
                (m) => m.ConsultarResultadosComponent,
              ),
          },
          {
            path: 'logs',
            loadComponent: () =>
              import('./features/resultados/logs-carga.component').then((m) => m.LogsCargaComponent),
          },
        ],
      },
      {
        path: 'admin',
        canActivate: [roleGuard(['ADMIN'])],
        loadComponent: () =>
          import('./features/admin/usuarios-admin.component').then((m) => m.UsuariosAdminComponent),
      },
      {
        path: 'admision',
        canActivate: [roleGuard(['ADMIN', 'ADMISION'])],
        loadComponent: () =>
          import('./features/admision/admision.component').then((m) => m.AdmisionComponent),
      },
      {
        path: 'paciente',
        canActivate: [roleGuard(['ADMIN', 'PACIENTE'])],
        loadComponent: () =>
          import('./features/paciente/mis-resultados.component').then((m) => m.MisResultadosComponent),
      },
      // Fase 10: admision, callcenter
    ],
  },
  { path: '**', redirectTo: '' },
];
