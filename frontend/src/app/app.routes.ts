import { Routes } from '@angular/router';
import { Login } from './features/identity/login/login';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: Login },
];