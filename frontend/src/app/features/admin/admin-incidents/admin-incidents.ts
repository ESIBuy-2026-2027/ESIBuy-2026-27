import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AppSidebarComponent } from '../../../shared/app-sidebar/app-sidebar';

export type IncidentStatus = 'PENDIENTE' | 'RESUELTA';

export interface Incident {
  id: string;
  subject: string;
  category: string;
  reference?: string | null;
  description: string;
  status: IncidentStatus;
  createdBy: string;
  createdAt: string;
  resolvedAt?: string | null;
}

export const MOCK_INCIDENTS_PREVIEW: Incident[] = [
  {
    id: 'INC-2026-001',
    subject: 'Error de sincronización de inventario en categoría Electrónica',
    category: 'PRODUCTO',
    reference: 'PROD-772',
    description: 'El stock disponible no coincide con las unidades confirmadas tras la última compra.',
    status: 'PENDIENTE',
    createdBy: 'admin@esibuy.com',
    createdAt: '2026-11-05T10:15:00Z',
  },
  {
    id: 'INC-2026-002',
    subject: 'Comprobante bancario ilegible en pedido #849',
    category: 'PAGO',
    reference: 'PED-849',
    description: 'El archivo adjunto no permite verificar la transferencia de forma automática.',
    status: 'RESUELTA',
    createdBy: 'admin@esibuy.com',
    createdAt: '2026-11-04T16:30:00Z',
    resolvedAt: '2026-11-04T17:45:00Z',
  },
  {
    id: 'INC-2026-003',
    subject: 'Fallo intermitente en notificación por correo OTP',
    category: 'SISTEMA',
    reference: null,
    description: 'Retraso de más de 3 minutos en la entrega de códigos 2FA.',
    status: 'RESUELTA',
    createdBy: 'admin@esibuy.com',
    createdAt: '2026-11-03T09:00:00Z',
    resolvedAt: '2026-11-03T11:20:00Z',
  },
];

@Component({
  selector: 'app-admin-incidents',
  standalone: true,
  imports: [AppSidebarComponent, CommonModule, FormsModule, DatePipe, RouterLink],
  templateUrl: './admin-incidents.html',
  styleUrl: './admin-incidents.css',
})
export class AdminIncidents {
  readonly sidebarCollapsed = signal(false);
  readonly isUnderDevelopment = signal(true);

  // Form signals (preview)
  readonly subject = signal('');
  readonly category = signal('');
  readonly reference = signal('');
  readonly description = signal('');
  readonly submitting = signal(false);

  // Notifications
  readonly mensajeExito = signal('');
  readonly mensajeError = signal('');

  // Incidents preview data (en memoria, sin llamadas backend)
  readonly incidents = signal<Incident[]>(MOCK_INCIDENTS_PREVIEW);
  readonly loading = signal(false);
  readonly loadError = signal(false);
  readonly resolvingId = signal<string | null>(null);

  // Contadores computados
  readonly totalCount = computed(() => this.incidents().length);
  readonly pendingCount = computed(() => this.incidents().filter((i) => i.status === 'PENDIENTE').length);
  readonly resolvedCount = computed(() => this.incidents().filter((i) => i.status === 'RESUELTA').length);

  toggleSidebar(): void {
    this.sidebarCollapsed.update((collapsed) => !collapsed);
  }
}
