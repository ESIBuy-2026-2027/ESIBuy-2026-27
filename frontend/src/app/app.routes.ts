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
  { path: 'admin/users/new', component: AdminUserCreate },
  { path: 'admin/usuarios', component: AdminUserList },

  // Redirecciones
  { path: 'admin', redirectTo: 'admin/usuarios', pathMatch: 'full' },
  { path: 'admin/users', redirectTo: 'admin/usuarios', pathMatch: 'full' },
  { path: 'admin.html', redirectTo: 'admin/usuarios', pathMatch: 'full' },

  { path: 'products/new', component: ProductCreate },
  { path: '**', redirectTo: '' }
];