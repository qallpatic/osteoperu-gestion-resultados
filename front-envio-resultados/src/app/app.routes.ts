import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./pages/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'resultados',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/upload/upload.component').then((m) => m.UploadComponent),
  },
  { path: '**', redirectTo: 'login' },
];
