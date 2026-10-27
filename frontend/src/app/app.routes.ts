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
import { SellerCatalog } from './features/products/seller-catalog/seller-catalog';
import { ClientCatalog } from './features/client/client-catalog/client-catalog';
import { ClientWishlist } from './features/client/client-wishlist/client-wishlist';
import { ClientOrders } from './features/client/client-orders/client-orders';
import { ClientOrderDetail } from './features/client/client-order-detail/client-order-detail';
import { ClientProfile } from './features/client/client-profile/client-profile';
import { SellerProfile } from './features/products/seller-profile/seller-profile';
import { SellerOrders } from './features/products/seller-orders/seller-orders';
import { AdminProfile } from './features/admin/admin-profile/admin-profile';
import { AdminCategoryList } from './features/admin/admin-category-list/admin-category-list';
import { AdminProductList } from './features/admin/admin-product-list/admin-product-list';
import { ClientCart } from './features/client/cart/cart';
import { PasswordRecovery } from './features/identity/password-recovery/password-recovery';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'login', component: Login },
  { path: 'recuperar-password', component: PasswordRecovery },
  { path: 'restablecer-password', component: PasswordRecovery },
  { path: 'register', component: Register },

  { path: 'admin/users/new', component: AdminUserCreate, canActivate: [authGuard, roleGuard('ADMIN')] },
  { path: 'admin/altas', redirectTo: 'admin/users/new', pathMatch: 'full' },
  { path: 'admin/usuarios', component: AdminUserList, canActivate: [authGuard, roleGuard('ADMIN')] },
  { path: 'admin/usuarios/:id/editar', component: AdminUserDetail, canActivate: [authGuard, roleGuard('ADMIN')] },
  { path: 'admin/categorias', component: AdminCategoryList, canActivate: [authGuard, roleGuard('ADMIN')] },
  { path: 'admin/productos', component: AdminProductList, canActivate: [authGuard, roleGuard('ADMIN')] },
  { path: 'admin/products', redirectTo: 'admin/productos', pathMatch: 'full' },

  { path: 'admin', redirectTo: 'admin/usuarios', pathMatch: 'full' },
  { path: 'admin/users', redirectTo: 'admin/usuarios', pathMatch: 'full' },
  { path: 'admin.html', redirectTo: 'admin/usuarios', pathMatch: 'full' },
  { path: 'admin/profile', component: AdminProfile, canActivate: [authGuard, roleGuard('ADMIN')] },

  { path: 'seller/catalog', component: SellerCatalog, canActivate: [authGuard, roleGuard('SELLER')] },
  { path: 'seller/orders', component: SellerOrders, canActivate: [authGuard, roleGuard('SELLER')] },
  { path: 'products/new', component: ProductCreate, canActivate: [authGuard, roleGuard('SELLER')] },
  { path: 'products/:id', component: ProductDetail },
  { path: 'products/:id/edit', component: ProductCreate, canActivate: [authGuard, roleGuard('SELLER')] },
  { path: 'seller/profile', component: SellerProfile, canActivate: [authGuard, roleGuard('SELLER')] },

  { path: 'client/catalog', component: ClientCatalog, canActivate: [authGuard, roleGuard('CLIENT')] },
  { path: 'client/dashboard', redirectTo: 'client/catalog', pathMatch: 'full' },
  { path: 'client/wishlist', component: ClientWishlist, canActivate: [authGuard, roleGuard('CLIENT')] },
  { path: 'client/orders', component: ClientOrders, canActivate: [authGuard, roleGuard('CLIENT')] },
  { path: 'client/orders/:pedidoId', component: ClientOrderDetail, canActivate: [authGuard, roleGuard('CLIENT')] },
  { path: 'client/compras', redirectTo: 'client/orders', pathMatch: 'full' },
  { path: 'client/cart', component: ClientCart, canActivate: [authGuard, roleGuard('CLIENT')] },
  { path: 'client/profile', component: ClientProfile, canActivate: [authGuard, roleGuard('CLIENT')] },

  { path: '**', redirectTo: '' }
];
