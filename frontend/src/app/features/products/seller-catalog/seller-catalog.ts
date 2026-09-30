import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-seller-catalog',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './seller-catalog.html',
  styleUrl: './seller-catalog.css',
})
export class SellerCatalog {}
