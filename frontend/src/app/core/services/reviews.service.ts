import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface CreateReviewRequest {
  orderId: string;
  productId: string;
  rating: number;
  comment: string | null;
}

export interface ProductReview {
  rating: number;
  comment: string | null;
  createdAt: string;
}

export interface ProductReviewSummary {
  averageRating: number | null;
  reviewCount: number;
  reviews: ProductReview[];
}

export interface ReviewedPurchase {
  orderId: string;
  productId: string;
}

@Injectable({ providedIn: 'root' })
export class ReviewsService {
  private readonly http = inject(HttpClient);

  createReview(request: CreateReviewRequest): Observable<string> {
    return this.http.post('/api/reviews', request, { responseType: 'text' });
  }

  getProductReviews(productId: string): Observable<ProductReviewSummary> {
    return this.http.get<ProductReviewSummary>(`/api/reviews/product/${productId}`);
  }

  getMyReviewedPurchases(): Observable<ReviewedPurchase[]> {
    return this.http.get<ReviewedPurchase[]>('/api/reviews/mine');
  }
}
