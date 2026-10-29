import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, beforeEach, it, expect, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { PedidoService } from '../../../core/services/pedido.service';
import { ClientShipmentTracking } from './client-shipment-tracking';

describe('ClientShipmentTracking', () => {
  let fixture: ComponentFixture<ClientShipmentTracking>;
  let obtenerHistorialEstadosEnvio: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    obtenerHistorialEstadosEnvio = vi.fn().mockReturnValue(of({
      historialEstados: [
        { estado: 'PREPARANDO', fecha: '2026-10-27T09:00:00' },
        { estado: 'ENVIADO', fecha: '2026-10-28T12:00:00' },
        { estado: 'ENTREGADO', fecha: '2026-10-30T09:00:00' },
      ],
      estimatedDeliveryDate: '2026-10-30',
    }));

    await TestBed.configureTestingModule({
      imports: [ClientShipmentTracking],
      providers: [{
        provide: PedidoService,
        useValue: { obtenerHistorialEstadosEnvio },
      }],
    }).compileComponents();

    fixture = TestBed.createComponent(ClientShipmentTracking);
    fixture.componentRef.setInput('pedidoId', 'pedido-123');
    fixture.componentRef.setInput('currentStatus', 'ENTREGADO');
  });

  it('should display the recorded shipment states and available delivery date', async () => {
    fixture.detectChanges();
    await fixture.whenStable();

    const element: HTMLElement = fixture.nativeElement;
    expect(obtenerHistorialEstadosEnvio).toHaveBeenCalledWith('pedido-123');
    expect(element.textContent).toContain('Preparando');
    expect(element.textContent).toContain('Enviado');
    expect(element.textContent).toContain('Entregado');
    expect(element.textContent).toContain('Entrega estimada');
    expect(element.textContent).toContain('30 oct 2026');
    expect(element.querySelectorAll('.progress-step')).toHaveLength(4);
    expect(element.querySelector('.progress-step[aria-current="step"]')?.textContent).toContain('Entregado');
    expect(element.querySelector('.progress-steps')?.getAttribute('style')).toContain('--progress-width: 75%');
    expect(element.querySelector('.estimated-date')?.textContent).toContain('3 días hábiles desde el envío');
  });

  it('should show the current order status when no history is available', async () => {
    obtenerHistorialEstadosEnvio.mockReturnValue(of({
      historialEstados: [],
      estimatedDeliveryDate: null,
    }));
    fixture.componentRef.setInput('currentStatus', 'PREPARANDO');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain('Estado actual: Preparando.');
    expect(fixture.nativeElement.textContent).toContain('Tiempo estimado');
    expect(fixture.nativeElement.textContent).toContain('3 días hábiles desde el envío');
    expect(fixture.nativeElement.querySelector('.progress-step[aria-current="step"]')?.textContent)
      .toContain('Preparando');
    expect(fixture.nativeElement.querySelector('.progress-steps')?.getAttribute('style'))
      .toContain('--progress-width: 25%');
  });

  it('should show an explicit error when the tracking request fails', async () => {
    obtenerHistorialEstadosEnvio.mockReturnValue(throwError(() => new Error('network error')));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain('No se pudo cargar el seguimiento del envío.');
  });
});
