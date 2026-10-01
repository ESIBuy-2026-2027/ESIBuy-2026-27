import { Routes } from '@angular/router';
import { Home } from './features/home/home';
import { Login } from './features/identity/login/login';
import { Register } from './features/identity/register/register';
import { AdminUserCreate } from './features/admin/admin-user-create/admin-user-create';
import { ProductCreate } from './features/products/product-create/product-create';
import { AdminUserList } from './features/admin/admin-user-list/admin-user-list';
import { AdminUserDetail } from './features/admin/admin-user-detail/admin-user-detail';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'admin/users/new', component: AdminUserCreate },
  { path: 'admin/usuarios', component: AdminUserList },
  { path: 'admin/usuarios/:id/editar', component: AdminUserDetail },
  { path: 'admin', redirectTo: 'admin/usuarios', pathMatch: 'full' },

  // Redirecciones compatibles
  { path: 'admin/users', redirectTo: 'admin/usuarios', pathMatch: 'full' },
  { path: 'admin/altas', redirectTo: 'admin/users/new', pathMatch: 'full' },
  { path: 'admin.html', redirectTo: 'admin/usuarios', pathMatch: 'full' },

  { path: 'products/new', component: ProductCreate },
  { path: '**', redirectTo: '' }
];
