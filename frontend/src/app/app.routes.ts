import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { Home } from './features/home/home';
import { Login } from './features/identity/login/login';
import { Register } from './features/identity/register/register';
import { AdminUserCreate } from './features/admin/admin-user-create/admin-user-create';
import { ProductCreate } from './features/products/product-create/product-create';
import { ProductDetail } from './features/products/product-detail/product-detail';
import { AdminUserList } from './features/admin/admin-user-list/admin-user-list';
import { AdminUserDetail } from './features/admin/admin-user-detail/admin-user-detail';
import { AdminCategoryManagement } from './features/admin/admin-category-management/admin-category-management';
import { SellerCatalog } from './features/products/seller-catalog/seller-catalog';
import { ClientCatalog } from './features/client/client-catalog/client-catalog';
import { ClientDashboard } from './features/client/client-dashboard/client-dashboard';
import { ClientWishlist } from './features/client/client-wishlist/client-wishlist';
import { ClientProfile } from './features/client/client-profile/client-profile';
import { SellerProfile } from './features/products/seller-profile/seller-profile';
import { AdminProfile } from './features/admin/admin-profile/admin-profile';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'products/:id', component: ProductDetail },

  { path: 'admin/users/new', component: AdminUserCreate, canActivate: [authGuard, roleGuard('ADMIN')] },
  { path: 'admin/altas', redirectTo: 'admin/users/new', pathMatch: 'full' },
  { path: 'admin/usuarios', component: AdminUserList, canActivate: [authGuard, roleGuard('ADMIN')] },
  { path: 'admin/usuarios/:id/editar', component: AdminUserDetail, canActivate: [authGuard, roleGuard('ADMIN')] },
  { path: 'admin/categorias', component: AdminCategoryManagement, canActivate: [authGuard, roleGuard('ADMIN')] },

  { path: 'admin', redirectTo: 'admin/usuarios', pathMatch: 'full' },
  { path: 'admin/users', redirectTo: 'admin/usuarios', pathMatch: 'full' },
  { path: 'admin.html', redirectTo: 'admin/usuarios', pathMatch: 'full' },
  { path: 'admin/profile', component: AdminProfile, canActivate: [authGuard, roleGuard('ADMIN')] },

  { path: 'seller/catalog', component: SellerCatalog, canActivate: [authGuard, roleGuard('SELLER')] },
  { path: 'products/new', component: ProductCreate, canActivate: [authGuard, roleGuard('SELLER')] },
  { path: 'seller/profile', component: SellerProfile, canActivate: [authGuard, roleGuard('SELLER')] },

  { path: 'client/catalog', component: ClientCatalog, canActivate: [authGuard, roleGuard('CLIENT')] },
  { path: 'client/dashboard', component: ClientDashboard, canActivate: [authGuard, roleGuard('CLIENT')] },
  { path: 'client/wishlist', component: ClientWishlist, canActivate: [authGuard, roleGuard('CLIENT')] },
  { path: 'client/profile', component: ClientProfile, canActivate: [authGuard, roleGuard('CLIENT')] },

  { path: '**', redirectTo: '' }
];