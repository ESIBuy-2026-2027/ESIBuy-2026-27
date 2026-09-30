import { Location } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ProductSummary, SellerCatalogService } from './seller-catalog.service';

@Component({
  selector: 'app-seller-catalog',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './seller-catalog.html',
  styleUrl: './seller-catalog.css',
})
export class SellerCatalog implements OnInit {
  private readonly catalogService = inject(SellerCatalogService);
  private readonly router = inject(Router);
  private readonly location = inject(Location);

  summary = signal<ProductSummary | null>(null);
  loading = signal(true);
  loadError = signal(false);
  productCreated = signal(
    this.router.currentNavigation()?.extras.state?.['productCreated'] === true
      || this.hasProductCreatedState(this.location.getState()),
  );

  ngOnInit(): void {
    this.loadSummary();
  }

  loadSummary(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.summary.set(null);

    this.catalogService.getSummary().subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.loadError.set(true);
      },
    });
  }

  private hasProductCreatedState(state: unknown): boolean {
    return typeof state === 'object'
      && state !== null
      && 'productCreated' in state
      && state.productCreated === true;
  }
}
