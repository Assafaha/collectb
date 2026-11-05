import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { adminGuard } from './core/admin.guard';
import { usersGuard } from './core/users.guard';
import { rolePermissionsGuard, rolesGuard } from './core/roles.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'my-brands' },
  {
    path: 'callback',
    loadComponent: () => import('./features/callback/callback').then((m) => m.Callback),
  },
  {
    path: 'my-brands',
    canActivate: [authGuard],
    loadComponent: () => import('./features/my-brands/my-brands').then((m) => m.MyBrands),
  },
  {
    path: 'users',
    canActivate: [authGuard, usersGuard],
    loadComponent: () => import('./features/users/users').then((m) => m.Users),
  },
  {
    path: 'roles',
    canActivate: [authGuard, rolesGuard],
    loadComponent: () => import('./features/roles/roles').then((m) => m.Roles),
  },
  {
    path: 'role-permissions',
    canActivate: [authGuard, rolePermissionsGuard],
    loadComponent: () =>
      import('./features/role-permissions/role-permissions').then((m) => m.RolePermissions),
  },
  {
    path: 'permissions',
    canActivate: [authGuard, rolePermissionsGuard],
    loadComponent: () => import('./features/permissions/permissions').then((m) => m.Permissions),
  },
  {
    path: 'brands/:brandId',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/brand-detail/brand-detail').then((m) => m.BrandDetail),
  },
  {
    path: 'brands/:brandId/catalog',
    canActivate: [authGuard],
    loadComponent: () => import('./features/catalog/catalog').then((m) => m.Catalog),
  },
  {
    path: 'admin/brands',
    canActivate: [authGuard, adminGuard],
    loadComponent: () =>
      import('./features/admin/admin-brands/admin-brands').then((m) => m.AdminBrands),
  },
  {
    path: 'admin/brands/:brandId',
    canActivate: [authGuard, adminGuard],
    loadComponent: () =>
      import('./features/admin/admin-brand-detail/admin-brand-detail').then((m) => m.AdminBrandDetail),
  },
];
