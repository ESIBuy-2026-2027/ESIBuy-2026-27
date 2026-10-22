import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'info';

export interface ToastMessage {
  text: string;
  type: ToastType;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private timeoutId: ReturnType<typeof setTimeout> | null = null;
  readonly toast = signal<ToastMessage | null>(null);

  show(text: string, type: ToastType = 'success', durationMs = 3500): void {
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
    }
    this.toast.set({ text, type });
    this.timeoutId = setTimeout(() => {
      this.toast.set(null);
      this.timeoutId = null;
    }, durationMs);
  }

  clear(): void {
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
      this.timeoutId = null;
    }
    this.toast.set(null);
  }
}

