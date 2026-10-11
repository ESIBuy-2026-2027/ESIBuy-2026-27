import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Login } from '../identity/login/login';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, Login],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home {
  private readonly authService = inject(AuthService);

  readonly isAuthenticated = computed(() => {
    if (typeof this.authService?.currentUser === 'function') {
      return this.authService.currentUser() !== null;
    }
    return false;
  });
}
