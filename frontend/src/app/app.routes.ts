import { Routes } from '@angular/router';
import { Home } from './features/home/home';
import { Login } from './features/identity/login/login';
import { Register } from './features/identity/register/register';
import { AdminUserCreate } from './features/admin/admin-user-create/admin-user-create';
import { ProductCreate } from './features/products/product-create/product-create';
import { AdminUserList } from './features/admin/admin-user-list/admin-user-list';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'admin', component: AdminUserList },
  { path: 'admin/altas', component: AdminUserCreate },

  // Redirecciones compatibles
  { path: 'admin/usuarios', redirectTo: 'admin', pathMatch: 'full' },
  { path: 'admin/users', redirectTo: 'admin', pathMatch: 'full' },
  { path: 'admin/users/new', redirectTo: 'admin/altas', pathMatch: 'full' },
  { path: 'admin.html', redirectTo: 'admin', pathMatch: 'full' },

  { path: 'products/new', component: ProductCreate },
  { path: '**', redirectTo: '' }
];
