import { describe, expect, it, vi } from 'vitest';
import { ToastService } from './toast.service';

describe('ToastService', () => {
  it('keeps error notifications until they are explicitly cleared', () => {
    vi.useFakeTimers();
    const service = new ToastService();

    service.show('Error persistente', 'error');
    vi.advanceTimersByTime(10000);

    expect(service.toast()).toEqual({ text: 'Error persistente', type: 'error' });

    service.clear();
    expect(service.toast()).toBeNull();
    vi.useRealTimers();
  });

  it('automatically clears informational notifications', () => {
    vi.useFakeTimers();
    const service = new ToastService();

    service.show('Información', 'info');
    vi.advanceTimersByTime(3500);

    expect(service.toast()).toBeNull();
    vi.useRealTimers();
  });
});
