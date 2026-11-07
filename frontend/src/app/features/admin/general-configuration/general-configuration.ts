import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import {
  GeneralConfiguration,
  GeneralConfigurationService,
} from '../../../core/services/general-configuration.service';
import { AppSidebarComponent } from '../../../shared/app-sidebar/app-sidebar';

const DEFAULT_CONFIGURATION: GeneralConfiguration = {
  ivaPorcentaje: 21,
  gastosEnvio: 4.99,
  envioGratuitoDesde: 50,
  registroHabilitado: true,
};

@Component({
  selector: 'app-general-configuration',
  standalone: true,
  imports: [FormsModule, AppSidebarComponent],
  templateUrl: './general-configuration.html',
  styleUrl: './general-configuration.css',
})
export class GeneralConfigurationPage implements OnInit {
  private readonly configurationService = inject(GeneralConfigurationService);

  readonly sidebarCollapsed = signal(false);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  form: GeneralConfiguration = { ...DEFAULT_CONFIGURATION };

  ngOnInit(): void {
    this.loadConfiguration();
  }

  toggleSidebar(): void {
    this.sidebarCollapsed.update((collapsed) => !collapsed);
  }

  loadConfiguration(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.configurationService.get().subscribe({
      next: (configuration) => {
        this.form = { ...configuration };
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.errorMessage.set(this.getErrorMessage(error));
      },
    });
  }

  save(form: NgForm): void {
    form.control.markAllAsTouched();
    this.errorMessage.set(null);
    this.successMessage.set(null);

    if (form.invalid || this.saving() || !this.isConfigurationValid()) {
      if (!this.isConfigurationValid()) {
        this.errorMessage.set('Revisa los valores: deben ser números válidos y no negativos.');
      }
      return;
    }

    this.saving.set(true);
    this.configurationService.update({ ...this.form }).subscribe({
      next: (configuration) => {
        this.form = { ...configuration };
        this.successMessage.set('La configuración se ha actualizado correctamente.');
        this.saving.set(false);
      },
      error: (error: unknown) => {
        this.errorMessage.set(this.getErrorMessage(error));
        this.saving.set(false);
      },
    });
  }

  private isConfigurationValid(): boolean {
    const values = [
      this.form.ivaPorcentaje,
      this.form.gastosEnvio,
      this.form.envioGratuitoDesde,
    ];

    return values.every((value) => Number.isFinite(value) && value >= 0)
      && this.form.ivaPorcentaje <= 100;
  }

  private getErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const message = this.getServerMessage(error.error);
      if (message) {
        return message;
      }

      if (error.status === 401) {
        return 'Inicia sesión con una cuenta de administrador para acceder a la configuración.';
      }
      if (error.status === 403) {
        return 'No tienes permisos para gestionar la configuración general.';
      }
      if (error.status === 0) {
        return 'No se pudo conectar con el servidor. Comprueba tu conexión e inténtalo de nuevo.';
      }
    }

    return 'No se pudo guardar o cargar la configuración. Inténtalo de nuevo.';
  }

  private getServerMessage(body: unknown): string | null {
    if (typeof body !== 'object' || body === null) {
      return null;
    }

    if ('message' in body && typeof body.message === 'string') {
      return body.message;
    }

    if ('errors' in body && typeof body.errors === 'object' && body.errors !== null) {
      const messages = Object.values(body.errors)
        .filter((value): value is string => typeof value === 'string');
      return messages.length > 0 ? messages.join(' ') : null;
    }

    return null;
  }
}
