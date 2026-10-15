import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Category, ProductsService } from '../../products/products.service';

type CategoryDialogMode = 'create' | 'edit' | 'delete' | null;

@Component({
  selector: 'app-admin-category-management',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './admin-category-management.html',
  styleUrl: './admin-category-management.css',
})
export class AdminCategoryManagement {
  private readonly productsService = inject(ProductsService);

  readonly categories = signal<Category[]>([]);
  readonly loading = signal(true);
  readonly loadError = signal(false);
  readonly search = signal('');
  readonly dialogMode = signal<CategoryDialogMode>(null);
  readonly draftName = signal('');
  readonly selectedCategory = signal<Category | null>(null);
  readonly feedback = signal('');
  readonly filteredCategories = computed(() => {
    const query = this.search().trim().toLocaleLowerCase();
    return this.categories()
      .filter((category) => category.name.toLocaleLowerCase().includes(query))
      .sort((first, second) => first.name.localeCompare(second.name, 'es'));
  });

  constructor() {
    this.loadCategories();
  }

  openCreateDialog(): void {
    this.selectedCategory.set(null);
    this.draftName.set('');
    this.dialogMode.set('create');
  }

  openEditDialog(category: Category): void {
    this.selectedCategory.set(category);
    this.draftName.set(category.name);
    this.dialogMode.set('edit');
  }

  openDeleteDialog(category: Category): void {
    this.selectedCategory.set(category);
    this.dialogMode.set('delete');
  }

  closeDialog(): void {
    this.dialogMode.set(null);
  }

  saveCategory(): void {
    const name = this.draftName().trim();
    if (!name || this.hasDuplicateName(name)) {
      return;
    }

    const mode = this.dialogMode();
    if (mode === 'create') {
      const category = { id: `prototype-${Date.now()}`, name };
      this.categories.update((categories) => [...categories, category]);
      this.feedback.set(`Se añadió «${name}» a la maqueta.`);
    } else if (mode === 'edit') {
      const selected = this.selectedCategory();
      if (!selected) {
        return;
      }
      this.categories.update((categories) =>
        categories.map((category) => category.id === selected.id ? { ...category, name } : category),
      );
      this.feedback.set(`Se actualizó «${name}» en la maqueta.`);
    } else {
      return;
    }

    this.closeDialog();
  }

  deleteCategory(): void {
    const selected = this.selectedCategory();
    if (!selected) {
      return;
    }

    this.categories.update((categories) => categories.filter((category) => category.id !== selected.id));
    this.feedback.set(`Se eliminó «${selected.name}» de la maqueta.`);
    this.closeDialog();
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

  private loadCategories(): void {
    this.productsService.getCategories().subscribe({
      next: (categories) => {
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.loadError.set(true);
      },
    });
  }
}
