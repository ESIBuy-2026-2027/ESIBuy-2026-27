import { Routes } from '@angular/router';
import { Home } from './features/home/home';
import { Login } from './features/identity/login/login';
import { Register } from './features/identity/register/register';
import { AdminUserCreate } from './features/admin/admin-user-create/admin-user-create';
import { ProductCreate } from './features/products/product-create/product-create';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'admin/users/new', component: AdminUserCreate },
  { path: 'products/new', component: ProductCreate },
  { path: '**', redirectTo: '' }
];