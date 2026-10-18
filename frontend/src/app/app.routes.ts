import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { Home } from './features/home/home';
import { Register } from './features/identity/register/register';
import { AdminUserCreate } from './features/admin/admin-user-create/admin-user-create';
import { ProductCreate } from './features/products/product-create/product-create';
import { AdminUserList } from './features/admin/admin-user-list/admin-user-list';
import { AdminUserDetail } from './features/admin/admin-user-detail/admin-user-detail';
import { SellerCatalog } from './features/products/seller-catalog/seller-catalog';
import { ClientDashboard } from './features/client/client-dashboard/client-dashboard';
import { ClientProfile } from './features/client/client-profile/client-profile';
import { SellerProfile } from './features/products/seller-profile/seller-profile';
import { SellerOrders } from './features/products/seller-orders/seller-orders';
import { AdminProfile } from './features/admin/admin-profile/admin-profile';
import { ClientCart } from './features/client/cart/cart';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'register', component: Register },

  { path: 'admin/users/new', component: AdminUserCreate, canActivate: [authGuard, roleGuard('ADMIN')] },
  { path: 'admin/altas', redirectTo: 'admin/users/new', pathMatch: 'full' },
  { path: 'admin/usuarios', component: AdminUserList, canActivate: [authGuard, roleGuard('ADMIN')] },
  { path: 'admin/usuarios/:id/editar', component: AdminUserDetail, canActivate: [authGuard, roleGuard('ADMIN')] },

  { path: 'admin', redirectTo: 'admin/usuarios', pathMatch: 'full' },
  { path: 'admin/users', redirectTo: 'admin/usuarios', pathMatch: 'full' },
  { path: 'admin.html', redirectTo: 'admin/usuarios', pathMatch: 'full' },
  { path: 'admin/profile', component: AdminProfile, canActivate: [authGuard, roleGuard('ADMIN')] },

  { path: 'seller/catalog', component: SellerCatalog, canActivate: [authGuard, roleGuard('SELLER')] },
  { path: 'seller/orders', component: SellerOrders, canActivate: [authGuard, roleGuard('SELLER')] },
  { path: 'products/new', component: ProductCreate, canActivate: [authGuard, roleGuard('SELLER')] },
  { path: 'products/:id/edit', component: ProductCreate, canActivate: [authGuard, roleGuard('SELLER')] },
  { path: 'seller/profile', component: SellerProfile, canActivate: [authGuard, roleGuard('SELLER')] },

  { path: 'client/dashboard', component: ClientDashboard, canActivate: [authGuard, roleGuard('CLIENT')] },
  { path: 'client/cart', component: ClientCart, canActivate: [authGuard, roleGuard('CLIENT')] },
  { path: 'client/profile', component: ClientProfile, canActivate: [authGuard, roleGuard('CLIENT')] },

  { path: '**', redirectTo: '' }
];