import { Component, Input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

export type SidebarVariant = 'admin' | 'seller' | 'seller-compact' | 'client';

@Component({
  selector: 'app-shared-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './app-sidebar.html',
  styleUrl: './app-sidebar.css',
})
export class AppSidebarComponent {
  @Input({ required: true }) variant: SidebarVariant = 'seller';
  @Input() cartCount = 0;
}
