import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'designs' },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'change-password',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/auth/change-password.component').then((m) => m.ChangePasswordComponent),
  },
  {
    path: 'designs',
    canActivate: [authGuard],
    loadComponent: () => import('./features/gallery/gallery.component').then((m) => m.GalleryComponent),
  },
  {
    path: 'designs/mine',
    canActivate: [authGuard, roleGuard('CREATOR', 'ADMIN', 'TECH_ADMIN')],
    loadComponent: () =>
      import('./features/gallery/my-designs.component').then((m) => m.MyDesignsComponent),
  },
  {
    path: 'designs/new',
    canActivate: [authGuard, roleGuard('CREATOR', 'ADMIN', 'TECH_ADMIN')],
    loadComponent: () =>
      import('./features/design-editor/design-editor.component').then((m) => m.DesignEditorComponent),
  },
  {
    path: 'designs/:id/edit',
    canActivate: [authGuard, roleGuard('CREATOR', 'ADMIN', 'TECH_ADMIN')],
    loadComponent: () =>
      import('./features/design-editor/design-editor.component').then((m) => m.DesignEditorComponent),
  },
  {
    path: 'designs/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/design-detail/design-detail.component').then((m) => m.DesignDetailComponent),
  },
  {
    path: 'admin/review',
    canActivate: [authGuard, roleGuard('ADMIN', 'TECH_ADMIN')],
    loadComponent: () =>
      import('./features/admin/review-queue.component').then((m) => m.ReviewQueueComponent),
  },
  {
    path: 'admin/users',
    canActivate: [authGuard, roleGuard('ADMIN', 'TECH_ADMIN')],
    loadComponent: () =>
      import('./features/admin/user-management.component').then((m) => m.UserManagementComponent),
  },
  { path: '**', redirectTo: 'designs' },
];
