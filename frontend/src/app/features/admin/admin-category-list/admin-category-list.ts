import { httpErrorMessage } from '../../../core/utils/http-error-message';
import { AppSidebarComponent } from '../../../shared/app-sidebar/app-sidebar';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminCategoryService } from './admin-category.service';
import { Category } from '../../products/products.service';

type CategoryDialogMode = 'create' | 'edit' | 'delete' | null;

@Component({
  selector: 'app-admin-category-list',
  standalone: true,
  imports: [FormsModule, AppSidebarComponent],
  templateUrl: './admin-category-list.html',
  styleUrl: './admin-category-list.css',
})
export class AdminCategoryList {
  private readonly categoryService = inject(AdminCategoryService);
  readonly categories = signal<Category[]>([]);
  readonly loading = signal(true);
  readonly loadError = signal(false);
  readonly search = signal('');
  readonly dialogMode = signal<CategoryDialogMode>(null);
  readonly draftName = signal('');
  readonly selectedCategory = signal<Category | null>(null);
  readonly mensajeExito = signal('');
  readonly mensajeError = signal('');
  readonly serverFieldErrors = signal<Partial<Record<'name', string>>>({});
  readonly saving = signal(false);
  readonly deleting = signal(false);
  readonly filteredCategories = computed(() => {
    const query = this.search().trim().toLocaleLowerCase();
    return this.categories()
      .filter((category) => category.name.toLocaleLowerCase().includes(query))
      .sort((first, second) => first.name.localeCompare(second.name, 'es'));
  });

  constructor() {
    this.loadCategories();
  }

  loadCategories(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.mensajeError.set('');
    this.categoryService.getAll().subscribe({
      next: (categories) => {
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.loadError.set(true);
        this.mensajeError.set(this.errorMessage(error, 'load'));
      },
    });
  }

  openCreateDialog(): void {
    this.selectedCategory.set(null);
    this.draftName.set('');
    this.clearMessages();
    this.dialogMode.set('create');
  }

  openEditDialog(category: Category): void {
    this.selectedCategory.set(category);
    this.draftName.set(category.name);
    this.clearMessages();
    this.dialogMode.set('edit');
  }

  openDeleteDialog(category: Category): void {
    this.selectedCategory.set(category);
    this.clearMessages();
    this.dialogMode.set('delete');
  }

  closeDialog(): void {
    this.dialogMode.set(null);
    this.serverFieldErrors.set({});
  }

  saveCategory(): void {
    const name = this.draftName().trim();
    if (!this.isValidName(name) || this.hasDuplicateName(name) || this.saving()) {
      return;
    }

    const mode = this.dialogMode();
    const selected = this.selectedCategory();
    if (mode !== 'create' && mode !== 'edit') {
      return;
    }

    this.saving.set(true);
    const onSave = (category: Category): void => {
      this.categories.update((categories) => mode === 'create'
        ? [...categories, category]
        : categories.map((current) => current.id === category.id ? category : current));
      this.mensajeExito.set(mode === 'create'
        ? `La categoría «${category.name}» se ha creado correctamente.`
        : `La categoría «${category.name}» se ha actualizado correctamente.`);
      this.saving.set(false);
      this.closeDialog();
    };
    const onError = (error: unknown): void => {
      this.saving.set(false);
      this.handleSaveError(error);
    };

    if (mode === 'create') {
      this.categoryService.create(name).subscribe({ next: onSave, error: onError });
    } else if (selected) {
      this.categoryService.update(selected.id, name).subscribe({ next: onSave, error: onError });
    } else {
      this.saving.set(false);
    }
  }

  deleteCategory(): void {
    const selected = this.selectedCategory();
    if (!selected || this.deleting()) {
      return;
    }

    this.deleting.set(true);
    this.categoryService.delete(selected.id).subscribe({
      next: () => {
        this.categories.update((categories) => categories.filter((category) => category.id !== selected.id));
        this.mensajeExito.set(`La categoría «${selected.name}» se ha eliminado correctamente.`);
        this.deleting.set(false);
        this.closeDialog();
      },
      error: (error: unknown) => {
        this.deleting.set(false);
        this.mensajeError.set(this.errorMessage(error, 'delete'));
        if (error instanceof HttpErrorResponse && error.status === 404) {
          this.categories.update((categories) => categories.filter((category) => category.id !== selected.id));
          this.closeDialog();
        }
      },
    });
  }

  hasDuplicateName(name = this.draftName()): boolean {
    const normalizedName = name.trim().toLocaleLowerCase();
    if (!normalizedName) {
      return false;
    }
    const selectedId = this.selectedCategory()?.id;
    return this.categories().some((category) =>
      category.id !== selectedId && category.name.trim().toLocaleLowerCase() === normalizedName);
  }

  isValidName(name = this.draftName()): boolean {
    const trimmedName = name.trim();
    return trimmedName.length >= 2 && trimmedName.length <= 50;
  }

  clearFieldError(): void {
    this.serverFieldErrors.set({});
    this.mensajeError.set('');
  }

  private handleSaveError(error: unknown): void {
    if (error instanceof HttpErrorResponse && error.status === 400) {
      const errors = this.apiErrors(error);
      this.serverFieldErrors.set(typeof errors['name'] === 'string' ? { name: errors['name'] } : {});
      this.mensajeError.set('Revisa los datos de la categoría e inténtalo de nuevo.');
      return;
    }

    this.mensajeError.set(this.errorMessage(error, 'save'));
    if (error instanceof HttpErrorResponse && error.status === 404) {
      const selectedId = this.selectedCategory()?.id;
      if (selectedId) {
        this.categories.update((categories) => categories.filter((category) => category.id !== selectedId));
      }
      this.closeDialog();
    }
  }

  private errorMessage(error: unknown, operation: 'load' | 'save' | 'delete'): string {
    if (!(error instanceof HttpErrorResponse)) {
      return 'No se pudo conectar con el servidor. Inténtalo de nuevo.';
    }
    const sharedMessage = httpErrorMessage(error);
    if (sharedMessage) return sharedMessage;
    if (error.status === 403) {
      return 'No tienes permisos para gestionar las categorías.';
    }
    if (error.status === 404) {
      return 'La categoría ya no existe. Actualiza la lista e inténtalo de nuevo.';
    }
    if (error.status === 409) {
      const message = this.apiMessage(error).toLocaleLowerCase();
      return message.includes('uso') || message.includes('producto')
        ? 'Está en uso por productos y no se puede eliminar.'
        : 'Ya existe una categoría con ese nombre.';
    }
    if (error.status === 400 && operation === 'load') {
      return 'No se pudo cargar la lista de categorías.';
    }
    return operation === 'load'
      ? 'No se pudieron cargar las categorías. Inténtalo de nuevo.'
      : 'No se pudo guardar la categoría. Inténtalo de nuevo.';
  }

  private apiErrors(error: HttpErrorResponse): Record<string, unknown> {
    const body: unknown = error.error;
    if (!this.isRecord(body)) {
      return {};
    }
    const errors = body['errors'];
    return this.isRecord(errors) ? errors : {};
  }

  private apiMessage(error: HttpErrorResponse): string {
    const body: unknown = error.error;
    if (!this.isRecord(body)) {
      return '';
    }
    const message = body['message'];
    return typeof message === 'string' ? message : '';
  }

  private isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
  }

  private clearMessages(): void {
    this.mensajeExito.set('');
    this.mensajeError.set('');
    this.serverFieldErrors.set({});
  }

}
