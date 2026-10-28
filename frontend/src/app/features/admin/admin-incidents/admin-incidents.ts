import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AdminIncidentService, Incident } from '../../../core/services/admin-incident.service';

@Component({
  selector: 'app-admin-incidents',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RouterLinkActive],
  templateUrl: './admin-incidents.html',
  styleUrl: './admin-incidents.css',
})
export class AdminIncidents {
  readonly sidebarCollapsed = signal(false);
  private readonly incidentService = inject(AdminIncidentService);

  // Form signals
  readonly subject = signal('');
  readonly category = signal('');
  readonly reference = signal('');
  readonly description = signal('');
  readonly submitting = signal(false);

  // Notifications
  readonly mensajeExito = signal('');
  readonly mensajeError = signal('');

  // Incidents data
  readonly incidents = signal<Incident[]>([]);
  readonly loading = signal(false);
  readonly loadError = signal(false);

  // Real-time calculated counters
  readonly totalCount = computed(() => this.incidents().length);
  readonly pendingCount = computed(() => this.incidents().filter((i) => i.status === 'PENDIENTE').length);
  readonly resolvedCount = computed(() => this.incidents().filter((i) => i.status === 'RESUELTA').length);

  toggleSidebar(): void {
    this.sidebarCollapsed.update((collapsed) => !collapsed);
  }

  onSubmit(): void {
    const subj = this.subject().trim();
    const cat = this.category().trim();
    const desc = this.description().trim();
    const ref = this.reference().trim();

    if (!subj) {
      this.mensajeError.set('Por favor, indica el asunto de la incidencia.');
      return;
    }
    if (!cat) {
      this.mensajeError.set('Por favor, selecciona una categoría para clasificar la incidencia.');
      return;
    }
    if (!desc) {
      this.mensajeError.set('Por favor, describe con detalle la incidencia.');
      return;
    }

    this.submitting.set(true);
    this.mensajeError.set('');
    this.mensajeExito.set('');

    this.incidentService
      .create({
        subject: subj,
        category: cat,
        reference: ref ? ref : undefined,
        description: desc,
      })
      .subscribe({
        next: (created) => {
          this.submitting.set(false);
          this.mensajeExito.set('Incidencia registrada correctamente con estado PENDIENTE.');
          this.subject.set('');
          this.category.set('');
          this.reference.set('');
          this.description.set('');
          this.incidents.update((list) => [created, ...list]);
        },
        error: (err: unknown) => {
          this.submitting.set(false);
          const msg =
            err && typeof err === 'object' && 'error' in err && (err as { error?: { message?: string } }).error?.message
              ? (err as { error: { message: string } }).error.message
              : 'Error al registrar la incidencia. Compruebe los datos e inténtelo de nuevo.';
          this.mensajeError.set(msg);
        },
      });
  }
}

