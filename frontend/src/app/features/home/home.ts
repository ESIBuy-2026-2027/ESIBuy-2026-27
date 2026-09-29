import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div style="padding: 2rem; text-align: center; font-family: sans-serif;">
      <h1>Bienvenido a ESIBuy</h1>
      <p>Plataforma de Comercio Electrónico</p>
      <div style="margin-top: 2rem; display: flex; gap: 1rem; justify-content: center;">
        <a routerLink="/login" style="padding: 0.5rem 1.5rem; background: #007bff; color: white; border-radius: 4px; text-decoration: none;">Iniciar Sesión</a>
        <a routerLink="/register" style="padding: 0.5rem 1.5rem; background: #28a745; color: white; border-radius: 4px; text-decoration: none;">Registrarse</a>
        <a routerLink="/products/new" style="padding: 0.5rem 1.5rem; background: #6c757d; color: white; border-radius: 4px; text-decoration: none;">Alta de Producto</a>
      </div>
    </div>
  `
})
export class Home {}
