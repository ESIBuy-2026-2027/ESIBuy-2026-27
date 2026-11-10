import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Category } from '../../../core/services/products.service';
import { AdminCategoryService } from '../../../core/services/admin-category.service';
import { AdminCategoryList } from './admin-category-list';

describe('AdminCategoryList', () => {
  let categories: Category[];
  let categoryService: {
    getAll: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    categories = [
      { id: 'c-1', name: 'Electrónica' },
      { id: 'c-2', name: 'Hogar' },
    ];
    categoryService = {
      getAll: vi.fn(() => of(categories)),
      create: vi.fn(),
      update: vi.fn(),
      delete: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [AdminCategoryList],
      providers: [
        provideRouter([]),
        { provide: AdminCategoryService, useValue: categoryService },
      ],
    }).compileComponents();
  });

  it('loads the categories through the admin service', () => {
    const fixture = TestBed.createComponent(AdminCategoryList);
    fixture.detectChanges();

    expect(categoryService.getAll).toHaveBeenCalledOnce();
    expect(fixture.componentInstance.categories()).toEqual(categories);
    expect(fixture.componentInstance.loading()).toBe(false);
  });

  it('filters categories by name without changing the source list', () => {
    const fixture = TestBed.createComponent(AdminCategoryList);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    component.search.set('HOG');

    expect(component.filteredCategories().map((category) => category.name)).toEqual(['Hogar']);
    expect(component.categories()).toHaveLength(2);
  });

  it('creates and updates categories through the service', () => {
    const fixture = TestBed.createComponent(AdminCategoryList);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const created = { id: 'c-3', name: 'Papelería' };
    categoryService.create.mockReturnValue(of(created));
    categoryService.update.mockReturnValue(of({ id: 'c-3', name: 'Oficina' }));

    component.openCreateDialog();
    component.draftName.set(' Papelería ');
    component.saveCategory();
    expect(categoryService.create).toHaveBeenCalledWith('Papelería');
    expect(component.categories()).toContainEqual(created);
    expect(component.mensajeExito()).toContain('creado correctamente');

    component.openEditDialog(created);
    component.draftName.set('Oficina');
    component.saveCategory();
    expect(categoryService.update).toHaveBeenCalledWith('c-3', 'Oficina');
    expect(component.categories().find((category) => category.id === 'c-3')?.name).toBe('Oficina');
  });

  it('prevents local duplicate names and displays server field validation', () => {
    const fixture = TestBed.createComponent(AdminCategoryList);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    component.openCreateDialog();
    component.draftName.set(' hogar ');
    expect(component.hasDuplicateName()).toBe(true);
    component.saveCategory();
    expect(categoryService.create).not.toHaveBeenCalled();

    categoryService.create.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 400,
      error: { errors: { name: 'El nombre debe tener entre 2 y 50 caracteres' } },
    })));
    component.draftName.set('Hogar y jardín');
    component.saveCategory();
    expect(component.serverFieldErrors().name).toBe('El nombre debe tener entre 2 y 50 caracteres');
  });

  it('deletes categories through the service and reports product conflicts', () => {
    const fixture = TestBed.createComponent(AdminCategoryList);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    categoryService.delete.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409,
      error: { message: 'La categoría está en uso por productos y no se puede eliminar' },
    })));

    component.openDeleteDialog(categories[0]);
    component.deleteCategory();

    expect(categoryService.delete).toHaveBeenCalledWith('c-1');
    expect(component.categories()).toHaveLength(2);
    expect(component.mensajeError()).toBe('Está en uso por productos y no se puede eliminar.');
  });

  it('maps authentication errors when loading categories', () => {
    categoryService.getAll.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 401 })));
    const fixture = TestBed.createComponent(AdminCategoryList);
    fixture.detectChanges();

    expect(fixture.componentInstance.loadError()).toBe(true);
    expect(fixture.componentInstance.mensajeError()).toBe('Tu sesión ha caducado. Inicia sesión de nuevo.');
  });
});
