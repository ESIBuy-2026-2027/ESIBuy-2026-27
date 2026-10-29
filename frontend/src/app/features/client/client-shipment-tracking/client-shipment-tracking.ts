import { Component, OnInit, inject, input, signal } from '@angular/core';
import { CustomerOrderStatus, PedidoService, SeguimientoEnvio } from '../../../core/services/pedido.service';

interface TrackingStep {
  label: string;
  statuses: CustomerOrderStatus[];
}

@Component({
  selector: 'app-client-shipment-tracking',
  standalone: true,
  templateUrl: './client-shipment-tracking.html',
  styleUrl: './client-shipment-tracking.css',
})
export class ClientShipmentTracking implements OnInit {
  private readonly pedidoService = inject(PedidoService);
  readonly pedidoId = input.required<string>();
  readonly currentStatus = input.required<CustomerOrderStatus>();
  readonly tracking = signal<SeguimientoEnvio | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly steps: TrackingStep[] = [
    { label: 'Pedido recibido', statuses: ['PAGADO', 'PENDIENTE'] },
    { label: 'Preparando', statuses: ['PREPARANDO'] },
    { label: 'Enviado', statuses: ['ENVIADO'] },
    { label: 'Entregado', statuses: ['ENTREGADO'] },
  ];
  private readonly dateFormatter = new Intl.DateTimeFormat('es-ES', {
    dateStyle: 'medium',
  });
  private readonly statusLabels: Record<CustomerOrderStatus, string> = {
    PAGADO: 'Pagado',
    PENDIENTE: 'Pendiente',
    PREPARANDO: 'Preparando',
    ENVIADO: 'Enviado',
    ENTREGADO: 'Entregado',
    CANCELADO: 'Cancelado',
  };
  ngOnInit(): void {
    this.pedidoService.obtenerHistorialEstadosEnvio(this.pedidoId()).subscribe({
      next: (tracking) => {
        this.tracking.set(tracking);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el seguimiento del envío.');
        this.loading.set(false);
      },
    });
  }

  statusLabel(status: CustomerOrderStatus): string {
    return this.statusLabels[status];
  }

  isStepComplete(index: number): boolean {
    return index < this.currentStepIndex();
  }

  isCurrentStep(index: number): boolean {
    return index === this.currentStepIndex();
  }

  progressWidth(): string {
    const progress = (this.currentStepIndex() / (this.steps.length - 1)) * 75;
    return `${progress}%`;
  }

  stepDate(step: TrackingStep): string | null {
    const event = this.tracking()?.historialEstados
      .filter((entry) => step.statuses.includes(entry.estado))
      .at(-1);
    return event?.fecha ?? null;
  }

  formatStepDate(date: string): string {
    return new Intl.DateTimeFormat('es-ES', { day: 'numeric', month: 'short' }).format(new Date(date));
  }

  formatEstimatedDate(date: string): string {
    return this.dateFormatter.format(new Date(`${date}T00:00:00`));
  }

  private currentStepIndex(): number {
    if (this.currentStatus() === 'CANCELADO') {
      return 0;
    }
    return Math.max(
      this.steps.findIndex((step) => step.statuses.includes(this.currentStatus())),
      0,
    );
  }
}
